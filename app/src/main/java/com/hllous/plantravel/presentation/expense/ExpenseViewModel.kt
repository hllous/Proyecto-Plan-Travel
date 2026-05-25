package com.hllous.plantravel.presentation.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.AssignmentRejectionReason
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CalculateSettlementUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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
) : ViewModel() {

    val selectedGroupId: StateFlow<String?> = selectedGroupHolder.selectedGroupId.asStateFlow()

    val groups: StateFlow<List<TravelGroup>> = repository.observeGroups()
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
            if (groupId == null) flowOf(emptyList()) else repository.observeMembers(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _expenseRetryTrigger = MutableStateFlow(0)

    val expenseItemsUiState: StateFlow<UiState<List<ExpenseItem>>> = _expenseRetryTrigger
        .flatMapLatest {
            selectedGroupHolder.selectedGroupId.flatMapLatest { groupId ->
                if (groupId == null) flowOf(UiState.Success(emptyList()))
                else repository.observeExpenseItems(groupId)
                    .map<List<ExpenseItem>, UiState<List<ExpenseItem>>> { UiState.Success(it) }
                    .catch { e -> emit(UiState.Error(e.message ?: "Error al cargar gastos")) }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val expenseItems: StateFlow<List<ExpenseItem>> = expenseItemsUiState
        .map { if (it is UiState.Success) it.data else emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<ItemAssignment>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList()) else repository.observeAssignments(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settlements = MutableStateFlow<List<MemberSettlement>>(emptyList())
    val settlements: StateFlow<List<MemberSettlement>> = _settlements

    private val _settlementWarnings = MutableStateFlow<List<SettlementWarning>>(emptyList())
    val settlementWarnings: StateFlow<List<SettlementWarning>> = _settlementWarnings

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun reloadExpenseItems() {
        _expenseRetryTrigger.value++
    }

    fun selectGroup(groupId: String) {
        selectedGroupHolder.selectedGroupId.value = groupId
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refreshSettlement() {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: return@launch
            val result = runCatching { calculateSettlementUseCase(groupId) }
            if (result.isFailure) {
                _message.value = "Error al calcular liquidacion"
                return@launch
            }
            updateSettlement(result.getOrThrow())
        }
    }

    fun addExpenseItem(name: String, unitPriceText: String, quantityText: String) {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            val unitCents = parsePriceToCents(unitPriceText)
            val quantity = quantityText.toIntOrNull() ?: 0
            val totalCents = unitCents * quantity
            if (name.isBlank() || unitCents <= 0 || quantity <= 0) {
                _message.value = "Carga item, precio unitario y cantidad validos"
                return@launch
            }
            val result = runCatching { addExpenseItemUseCase(groupId, name, totalCents, quantity) }
            if (result.isFailure) {
                _message.value = "Error al agregar gasto"
                return@launch
            }
            recalculateSettlementSilently(groupId)
        }
    }

    fun assignItem(itemId: String, memberId: String, quantityText: String) {
        viewModelScope.launch {
            val quantity = quantityText.toIntOrNull() ?: -1
            if (quantity < 0) {
                _message.value = "Cantidad invalida"
                return@launch
            }
            val result = runCatching { assignItemToMemberUseCase(itemId, memberId, quantity) }
            if (result.isFailure) {
                val e = result.exceptionOrNull()
                _message.value = if (e is IllegalStateException) "El item ya no existe" else "Error al asignar"
                return@launch
            }
            val outcome = result.getOrThrow()
            when (outcome) {
                AssignmentOutcome.Accepted -> {
                    val groupId = selectedGroupHolder.selectedGroupId.value
                    if (groupId != null) recalculateSettlementSilently(groupId)
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
            val result = runCatching { deleteExpenseItemUseCase(itemId) }
            if (result.isFailure) {
                _message.value = "Error al eliminar gasto"
                return@launch
            }
            val groupId = selectedGroupHolder.selectedGroupId.value
            if (groupId != null) recalculateSettlementSilently(groupId)
        }
    }

    private suspend fun recalculateSettlementSilently(groupId: String) {
        runCatching { calculateSettlementUseCase(groupId) }.onSuccess { updateSettlement(it) }
    }

    private fun updateSettlement(result: SettlementResult) {
        _settlements.value = result.memberSettlements
        _settlementWarnings.value = result.warnings
    }

    private fun parsePriceToCents(value: String): Long {
        val normalized = value.replace(',', '.').trim()
        val parts = normalized.split('.')
        val integer = parts.getOrNull(0)?.toLongOrNull() ?: return 0
        val decimalPart = parts.getOrNull(1).orEmpty().padEnd(2, '0').take(2)
        val decimals = decimalPart.toLongOrNull() ?: 0
        return (integer * 100) + decimals
    }
}
