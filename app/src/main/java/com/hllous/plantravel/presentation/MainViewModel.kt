package com.hllous.plantravel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
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
import com.hllous.plantravel.domain.usecase.ConsumeInviteUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.DeleteInviteUseCase
import com.hllous.plantravel.domain.usecase.GenerateInviteUseCase
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val generateInviteUseCase: GenerateInviteUseCase,
    private val deleteInviteUseCase: DeleteInviteUseCase,
    private val consumeInviteUseCase: ConsumeInviteUseCase,
    private val addExpenseItemUseCase: AddExpenseItemUseCase,
    private val assignItemToMemberUseCase: AssignItemToMemberUseCase,
    private val deleteExpenseItemUseCase: DeleteExpenseItemUseCase,
    private val calculateSettlementUseCase: CalculateSettlementUseCase
) : ViewModel() {

    val selectedGroupId: StateFlow<String?> = selectedGroupHolder.selectedGroupId.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _regions = MutableStateFlow<List<String>>(emptyList())
    val regions: StateFlow<List<String>> = _regions

    private val _selectedRegion = MutableStateFlow<String?>(null)
    val selectedRegion: StateFlow<String?> = _selectedRegion

    private val _recommendations = MutableStateFlow<List<DestinationRecommendation>>(emptyList())
    val recommendations: StateFlow<List<DestinationRecommendation>> = _recommendations

    private val _settlements = MutableStateFlow<List<MemberSettlement>>(emptyList())
    val settlements: StateFlow<List<MemberSettlement>> = _settlements

    private val _settlementWarnings = MutableStateFlow<List<SettlementWarning>>(emptyList())
    val settlementWarnings: StateFlow<List<SettlementWarning>> = _settlementWarnings

    // groups and members observed here so BallroomScreen continues to work until #23/24
    val groups: StateFlow<List<TravelGroup>> = repository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<GroupMember>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Placeholder until #22 implements observeInvites in repository
    val invites: StateFlow<List<InviteToken>> = MutableStateFlow(emptyList<InviteToken>()).asStateFlow()

    // Placeholder until #23 implements observeExpenseItems in repository
    val expenseItems: StateFlow<List<ExpenseItem>> = MutableStateFlow(emptyList<ExpenseItem>()).asStateFlow()
    val assignments: StateFlow<List<ItemAssignment>> = MutableStateFlow(emptyList<ItemAssignment>()).asStateFlow()

    init {
        viewModelScope.launch {
            runCatching { _regions.value = repository.getRegions() }
        }
    }

    // forwarding method so existing call sites (BallroomScreen, QrScannerScreen) continue to work
    fun selectGroup(groupId: String) {
        selectedGroupHolder.selectedGroupId.value = groupId
    }

    fun clearMessage() {
        _message.value = null
    }

    fun consumeInvite(code: String) {
        viewModelScope.launch {
            if (code.isBlank()) {
                _message.value = "Codigo de invitacion requerido"
                return@launch
            }
            val result = consumeInviteUseCase(code)
            _message.value = result.fold(
                onSuccess = { "Te uniste al grupo" },
                onFailure = { it.message ?: "No se pudo usar el QR" }
            )
        }
    }

    fun generateInvite() {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            generateInviteUseCase(groupId)
            _message.value = "Invitacion generada"
        }
    }

    fun deleteInvite(code: String) {
        viewModelScope.launch {
            deleteInviteUseCase(code)
            _message.value = "Invitacion eliminada"
        }
    }

    fun selectRegion(region: String) {
        _selectedRegion.value = region
        viewModelScope.launch {
            runCatching { _recommendations.value = repository.getRecommendationsByRegion(region) }
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
            addExpenseItemUseCase(groupId, name, totalCents, quantity)
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
            val outcome = try {
                assignItemToMemberUseCase(itemId, memberId, quantity)
            } catch (e: IllegalStateException) {
                _message.value = "El item ya no existe"
                return@launch
            }
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
            deleteExpenseItemUseCase(itemId)
            val groupId = selectedGroupHolder.selectedGroupId.value
            if (groupId != null) recalculateSettlementSilently(groupId)
        }
    }

    fun calculateSettlement() {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            updateSettlement(calculateSettlementUseCase(groupId))
            _message.value = "Division calculada"
        }
    }

    fun refreshSettlement() {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: return@launch
            updateSettlement(calculateSettlementUseCase(groupId))
        }
    }

    private suspend fun recalculateSettlementSilently(groupId: String) {
        updateSettlement(calculateSettlementUseCase(groupId))
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
