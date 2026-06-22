package com.hllous.plantravel.presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.PeerToPerDebt
import com.hllous.plantravel.domain.model.PeerToPerDebtUiModel
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.AssignmentRejectionReason
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CalculateSettlementUseCase
import com.hllous.plantravel.domain.usecase.CreateExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.FinalizeExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.SetExpenseGroupPayerUseCase
import com.hllous.plantravel.domain.usecase.SetExpenseGroupPinnedUseCase
import com.hllous.plantravel.domain.usecase.UpdateExpenseGroupNameUseCase
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ExpenseViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val addExpenseItemUseCase: AddExpenseItemUseCase,
    private val assignItemToMemberUseCase: AssignItemToMemberUseCase,
    private val deleteExpenseItemUseCase: DeleteExpenseItemUseCase,
    private val calculateSettlementUseCase: CalculateSettlementUseCase,
    private val createExpenseGroupUseCase: CreateExpenseGroupUseCase,
    private val updateExpenseGroupNameUseCase: UpdateExpenseGroupNameUseCase,
    private val deleteExpenseGroupUseCase: DeleteExpenseGroupUseCase,
    private val finalizeExpenseGroupUseCase: FinalizeExpenseGroupUseCase,
    private val setExpenseGroupPinnedUseCase: SetExpenseGroupPinnedUseCase,
    private val setExpenseGroupPayerUseCase: SetExpenseGroupPayerUseCase,
) : ViewModel() {

    data class ExpenseDashboardMovement(
        val group: ExpenseGroup,
        val memberNetCents: Long,
    )

    data class ExpenseDashboardState(
        val totalCents: Long = 0,
        val pendingGroupsCount: Int = 0,
        val memberNetCents: Long = 0,
        val pinnedMovements: List<ExpenseDashboardMovement> = emptyList(),
        val recentMovements: List<ExpenseDashboardMovement> = emptyList(),
    )

    private val _dashboardState = MutableStateFlow(ExpenseDashboardState())
    val dashboardState: StateFlow<ExpenseDashboardState> = _dashboardState.asStateFlow()

    val selectedGroupId: StateFlow<String?> = selectedGroupHolder.selectedGroupId.asStateFlow()

    val groups: StateFlow<List<TravelGroup>> = repository.observeGroups()
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            groups.collect { list ->
                if (list.isNotEmpty() && selectedGroupHolder.selectedGroupId.value == null) {
                    selectedGroupHolder.selectedGroupId.value = list.first().id
                }
            }
        }
    }

    val members: StateFlow<List<GroupMember>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // ── Peer-to-peer debts (declared here so init blocks below can reference them) ──

    private val _peerToPerDebts = MutableStateFlow<List<PeerToPerDebt>>(emptyList())
    val peerToPerDebts: StateFlow<List<PeerToPerDebt>> = _peerToPerDebts

    private val _peerToPerDebtsWithLinks = MutableStateFlow<List<PeerToPerDebtUiModel>>(emptyList())
    val peerToPerDebtsWithLinks: StateFlow<List<PeerToPerDebtUiModel>> = _peerToPerDebtsWithLinks

    // ── Expense Groups ────────────────────────────────────────────────────────

    private val _expenseGroupsRetryTrigger = MutableStateFlow(0)

    val expenseGroups: StateFlow<List<ExpenseGroup>> = combine(
        selectedGroupHolder.selectedGroupId,
        _expenseGroupsRetryTrigger,
    ) { groupId, _ -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeExpenseGroups(groupId)
                .retryWhen { _, attempt -> attempt < 5 }
                .catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(expenseGroups, currentMember) { groups, member -> groups to member }
                .collectLatest { (groups, member) ->
                    recalculateDashboard(groups, member)
                }
        }
        // When another user confirms a payment, the broadcast triggers expenseGroups to re-emit.
        // Re-fetch debt links so the confirmation status updates without restart.
        viewModelScope.launch {
            expenseGroups.collect { if (_peerToPerDebts.value.isNotEmpty()) refreshDebtLinks() }
        }
    }

    fun reloadExpenseGroups() { _expenseGroupsRetryTrigger.value++ }

    private val _selectedExpenseGroupId = MutableStateFlow<String?>(null)
    val selectedExpenseGroupId: StateFlow<String?> = _selectedExpenseGroupId.asStateFlow()

    fun selectExpenseGroup(id: String?) {
        _selectedExpenseGroupId.value = id
    }

    fun createExpenseGroup(
        name: String,
        category: String? = null,
        onSuccess: (() -> Unit)? = null,
    ) {
        viewModelScope.launch {
            val travelGroupId = selectedGroupHolder.selectedGroupId.value ?: return@launch
            val result = runCatching { createExpenseGroupUseCase(travelGroupId, name, category) }
            if (result.isFailure) {
                _message.value = "Error al crear gasto"
            } else {
                reloadExpenseGroups()
                onSuccess?.invoke()
            }
        }
    }

    fun deleteExpenseGroup(id: String) {
        viewModelScope.launch {
            val result = runCatching { deleteExpenseGroupUseCase(id) }
            if (result.isFailure) {
                _message.value = "Error al eliminar grupo de gastos"
            } else {
                if (_selectedExpenseGroupId.value == id) {
                    _selectedExpenseGroupId.value = null
                }
                reloadExpenseGroups()
            }
        }
    }

    fun renameExpenseGroup(id: String, name: String) {
        if (name.isBlank()) {
            _message.value = "El nombre no puede estar vacío"
            return
        }
        viewModelScope.launch {
            val result = runCatching { updateExpenseGroupNameUseCase(id, name) }
            if (result.isFailure) {
                _message.value = "Error al renombrar grupo de gastos"
            } else {
                reloadExpenseGroups()
            }
        }
    }

    fun setExpenseGroupPinned(id: String, pinned: Boolean) {
        viewModelScope.launch {
            val result = runCatching { setExpenseGroupPinnedUseCase(id, pinned) }
            if (result.isFailure) {
                _message.value = "Error al fijar grupo de gastos"
            } else {
                reloadExpenseGroups()
            }
        }
    }

    fun divideEqually() {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            val currentItems = expenseItems.value
            val currentMembers = members.value
            if (currentMembers.isEmpty() || currentItems.isEmpty()) return@launch
            val memberCount = currentMembers.size
            currentItems.forEach { item ->
                val share = item.quantity / memberCount
                currentMembers.forEach { member ->
                    runCatching { assignItemToMemberUseCase(item.id, member.id, share) }
                }
            }
            reloadAssignments()
            recalculateSettlementSilently(expenseGroupId)
        }
    }

    fun resetAllAssignments() {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            val currentItems = expenseItems.value
            val currentMembers = members.value
            currentItems.forEach { item ->
                currentMembers.forEach { member ->
                    runCatching { assignItemToMemberUseCase(item.id, member.id, 0) }
                }
            }
            reloadAssignments()
            recalculateSettlementSilently(expenseGroupId)
        }
    }

    fun setPayer(memberId: String?) {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            val result = runCatching { setExpenseGroupPayerUseCase(expenseGroupId, memberId) }
            if (result.isFailure) {
                _message.value = "Error al seleccionar pagador"
            } else {
                reloadExpenseGroups()
            }
        }
    }

    fun finalizeExpenseGroup() {
        viewModelScope.launch {
            if (currentMember.value?.role != MemberRole.ADMIN) return@launch
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            val group = expenseGroups.value.firstOrNull { it.id == expenseGroupId }
            if (group?.paidByMemberId == null) {
                _message.value = "Seleccioná quién pagó antes de finalizar"
                return@launch
            }
            if (hasUnassignedItems()) {
                _message.value = "Todos los items deben estar asignados antes de finalizar"
                return@launch
            }
            val result = runCatching { finalizeExpenseGroupUseCase(expenseGroupId) }
            if (result.isFailure) {
                _message.value = "Error al finalizar grupo de gastos"
            } else {
                reloadExpenseGroups()
            }
        }
    }

    private fun hasUnassignedItems(): Boolean {
        val items = expenseItems.value
        val currentAssignments = assignments.value
        if (items.isEmpty()) return false
        return items.any { item ->
            val assigned = currentAssignments
                .filter { it.itemId == item.id }
                .sumOf { it.quantity.coerceAtLeast(0) }
            assigned < item.quantity
        }
    }

    // ── Expense Items (drill-in) ───────────────────────────────────────────────

    private val _expenseRetryTrigger = MutableStateFlow(0)
    private val _assignmentRetryTrigger = MutableStateFlow(0)

    val expenseItemsUiState: StateFlow<UiState<List<ExpenseItem>>> = _expenseRetryTrigger
        .flatMapLatest {
            _selectedExpenseGroupId.flatMapLatest { expenseGroupId ->
                if (expenseGroupId == null) flowOf(UiState.Success(emptyList()))
                else repository.observeExpenseItems(expenseGroupId)
                    .map<List<ExpenseItem>, UiState<List<ExpenseItem>>> { UiState.Success(it) }
                    .catch { e -> emit(UiState.Error(e.message ?: "Error al cargar gastos")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val expenseItems: StateFlow<List<ExpenseItem>> = expenseItemsUiState
        .map { if (it is UiState.Success) it.data else emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<ItemAssignment>> = _assignmentRetryTrigger
        .flatMapLatest {
            _selectedExpenseGroupId.flatMapLatest { expenseGroupId ->
                if (expenseGroupId == null) flowOf(emptyList())
                else repository.observeAssignments(expenseGroupId).catch { emit(emptyList()) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settlements = MutableStateFlow<List<MemberSettlement>>(emptyList())
    val settlements: StateFlow<List<MemberSettlement>> = _settlements

    private val _settlementWarnings = MutableStateFlow<List<SettlementWarning>>(emptyList())
    val settlementWarnings: StateFlow<List<SettlementWarning>> = _settlementWarnings

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    fun reloadExpenseItems() {
        _expenseRetryTrigger.value++
    }

    fun reloadAssignments() {
        _assignmentRetryTrigger.value++
    }

    fun selectGroup(groupId: String) {
        selectedGroupHolder.selectedGroupId.value = groupId
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refreshSettlement() {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            val result = runCatching { calculateSettlementUseCase(expenseGroupId) }
            if (result.isFailure) {
                _message.value = "Error al calcular liquidacion"
                return@launch
            }
            updateSettlement(result.getOrThrow())
        }
    }

    fun addExpenseItem(name: String, unitPriceText: String, quantityText: String) {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            if (expenseGroups.value.firstOrNull { it.id == expenseGroupId }?.state == ExpenseGroupState.Finalized) return@launch
            val unitCents = parsePriceToCents(unitPriceText)
            val quantity = quantityText.toIntOrNull() ?: 0
            val totalCents = unitCents * quantity
            if (name.isBlank() || unitCents <= 0 || quantity <= 0) {
                _message.value = "Carga item, precio unitario y cantidad validos"
                return@launch
            }
            val result = runCatching { addExpenseItemUseCase(expenseGroupId, name, totalCents, quantity) }
            if (result.isFailure) {
                _message.value = "Error al agregar gasto"
                return@launch
            }
            _message.value = "Item agregado"
            reloadExpenseItems()
            reloadExpenseGroups()
            recalculateSettlementSilently(expenseGroupId)
        }
    }

    fun assignItem(itemId: String, memberId: String, quantityText: String) {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value
            if (expenseGroupId != null && expenseGroups.value.firstOrNull { it.id == expenseGroupId }?.state == ExpenseGroupState.Finalized) return@launch
            val quantity = quantityText.toIntOrNull() ?: -1
            if (quantity < 0) {
                _message.value = "Cantidad invalida"
                return@launch
            }
            val result = runCatching { assignItemToMemberUseCase(itemId, memberId, quantity) }
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                _message.value = when {
                    e is IllegalStateException -> "El item ya no existe"
                    e?.message?.contains("Over-assignment", ignoreCase = true) == true ||
                    e?.message?.contains("check_violation", ignoreCase = true) == true ->
                        "La cantidad asignada supera la cantidad del item"
                    else -> "Error al asignar"
                }
                return@launch
            }
            val outcome = result.getOrThrow()
            when (outcome) {
                AssignmentOutcome.Accepted -> {
                    reloadAssignments()
                    reloadExpenseGroups()
                    val expenseGroupId = _selectedExpenseGroupId.value
                    if (expenseGroupId != null) recalculateSettlementSilently(expenseGroupId)
                }
                is AssignmentOutcome.Rejected -> _message.value = when (outcome.reason) {
                    AssignmentRejectionReason.OVER_ASSIGNED -> "La cantidad asignada supera la cantidad del item"
                    AssignmentRejectionReason.NEGATIVE_QUANTITY -> "Cantidad invalida"
                }
            }
        }
    }

    fun deleteExpenseItem(itemId: String) {
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value
            if (expenseGroupId != null && expenseGroups.value.firstOrNull { it.id == expenseGroupId }?.state == ExpenseGroupState.Finalized) return@launch
            val result = runCatching { deleteExpenseItemUseCase(itemId) }
            if (result.isFailure) {
                _message.value = "Error al eliminar gasto"
                return@launch
            }
            reloadExpenseItems()
            reloadExpenseGroups()
            if (expenseGroupId != null) recalculateSettlementSilently(expenseGroupId)
        }
    }

    private suspend fun recalculateSettlementSilently(expenseGroupId: String) {
        runCatching { calculateSettlementUseCase(expenseGroupId) }.onSuccess { updateSettlement(it) }
    }

    private suspend fun recalculateDashboard(
        groups: List<ExpenseGroup>,
        member: GroupMember?,
    ) {
        if (groups.isEmpty()) {
            _dashboardState.value = ExpenseDashboardState()
            return
        }

        val allMovements = groups.map { group ->
            val memberNetCents = member?.let { currentMember ->
                runCatching {
                    val settlements = calculateSettlementUseCase(group.id).memberSettlements
                    val payerMemberId = group.paidByMemberId
                    when {
                        payerMemberId == null -> 0L
                        currentMember.id == payerMemberId ->
                            -settlements.filter { it.memberId != payerMemberId }.sumOf { it.amountCents }
                        else ->
                            settlements.firstOrNull { it.memberId == currentMember.id }?.amountCents ?: 0L
                    }
                }.getOrDefault(0L)
            } ?: 0L
            ExpenseDashboardMovement(group = group, memberNetCents = memberNetCents)
        }.sortedWith(
            compareByDescending<ExpenseDashboardMovement> { it.group.pinnedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.group.createdAtMillis ?: Long.MIN_VALUE }
        )

        val pinnedMovements = allMovements.filter { it.group.pinnedAtMillis != null }
        val recentMovements = allMovements.filter { it.group.pinnedAtMillis == null }

        _dashboardState.value = ExpenseDashboardState(
            totalCents = groups.sumOf { it.totalPriceCents },
            pendingGroupsCount = groups.count { it.state == ExpenseGroupState.Open },
            memberNetCents = allMovements.sumOf { it.memberNetCents },
            pinnedMovements = pinnedMovements,
            recentMovements = recentMovements,
        )
    }

    private fun updateSettlement(result: SettlementResult) {
        _settlements.value = result.memberSettlements
        _settlementWarnings.value = result.warnings
        val payerMemberId = _selectedExpenseGroupId.value?.let { id ->
            expenseGroups.value.firstOrNull { it.id == id }?.paidByMemberId
        }
        val debts = buildDebtsFromPayerView(result.memberSettlements, payerMemberId)
        _peerToPerDebts.value = debts
        viewModelScope.launch { refreshDebtLinks(debts) }
    }

    private fun buildDebtsFromPayerView(
        settlements: List<MemberSettlement>,
        payerMemberId: String?
    ): List<PeerToPerDebt> {
        val payer = settlements.firstOrNull { it.memberId == payerMemberId } ?: return emptyList()
        return settlements
            .filter { it.memberId != payerMemberId && it.amountCents > 0 }
            .map { debtor ->
                PeerToPerDebt(
                    fromMemberId = debtor.memberId,
                    fromMemberName = debtor.memberName,
                    toMemberId = payer.memberId,
                    toMemberName = payer.memberName,
                    amountCents = debtor.amountCents,
                )
            }
    }

    private suspend fun refreshDebtLinks(debts: List<PeerToPerDebt> = _peerToPerDebts.value) {
        val expenseGroupId = _selectedExpenseGroupId.value ?: return
        val currentMembers = members.value
        val uiModels = debts.map { debt ->
            val creditorUserId = currentMembers.firstOrNull { it.id == debt.toMemberId }?.userId
            val mpAlias = if (creditorUserId != null)
                runCatching { repository.getMpAlias(creditorUserId) }.getOrNull()
            else null
            val deepLink = mpAlias?.takeIf { it.isNotBlank() }?.let { alias ->
                val amountPesos = debt.amountCents / 100
                "mercadopago://send?amount=$amountPesos&alias=$alias"
            }
            val status = runCatching {
                repository.getPaymentStatus(debt.fromMemberId, debt.toMemberId, expenseGroupId)
            }.getOrNull()
            PeerToPerDebtUiModel(
                debt = debt,
                deepLink = deepLink,
                debtorConfirmed = status?.debtorConfirmed ?: false,
                creditorConfirmed = status?.creditorConfirmed ?: false,
            )
        }
        _peerToPerDebtsWithLinks.value = uiModels
    }

    fun markDebtorConfirmed(fromMemberId: String, toMemberId: String) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            _isSubmitting.value = true
            try {
                val result = runCatching { repository.markDebtorConfirmed(fromMemberId, toMemberId, expenseGroupId) }
                if (result.isFailure) {
                    _message.value = "Error al confirmar el pago"
                } else {
                    _message.value = "Pago confirmado"
                    refreshDebtLinks()
                    reloadExpenseGroups()
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun markCreditorConfirmed(fromMemberId: String, toMemberId: String) {
        if (_isSubmitting.value) return
        viewModelScope.launch {
            val expenseGroupId = _selectedExpenseGroupId.value ?: return@launch
            _isSubmitting.value = true
            try {
                val result = runCatching { repository.markCreditorConfirmed(fromMemberId, toMemberId, expenseGroupId) }
                if (result.isFailure) {
                    _message.value = "Error al confirmar el pago"
                } else {
                    _message.value = "Pago recibido confirmado"
                    refreshDebtLinks()
                    reloadExpenseGroups()
                }
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    private fun parsePriceToCents(value: String): Long {
        val normalized = value
            .replace("$", "")
            .replace(" ", "")
            .replace(".", "")
            .replace(',', '.')
            .trim()
        val parts = normalized.split('.')
        val integer = parts.getOrNull(0)?.toLongOrNull() ?: return 0
        val decimalPart = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
        val decimals = decimalPart.toLongOrNull() ?: 0
        return (integer * 100) + decimals
    }
}
