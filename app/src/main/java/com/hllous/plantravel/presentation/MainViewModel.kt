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
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.DeleteInviteUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.GenerateInviteUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val createGroupUseCase: CreateGroupUseCase,
    private val updateGroupNameUseCase: UpdateGroupNameUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val generateInviteUseCase: GenerateInviteUseCase,
    private val deleteInviteUseCase: DeleteInviteUseCase,
    private val consumeInviteUseCase: ConsumeInviteUseCase,
    private val addExpenseItemUseCase: AddExpenseItemUseCase,
    private val assignItemToMemberUseCase: AssignItemToMemberUseCase,
    private val deleteExpenseItemUseCase: DeleteExpenseItemUseCase,
    private val calculateSettlementUseCase: CalculateSettlementUseCase
) : ViewModel() {

    private val _selectedGroupId = MutableStateFlow<Long?>(null)
    val selectedGroupId: StateFlow<Long?> = _selectedGroupId

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

    val groups: StateFlow<List<TravelGroup>> = repository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<GroupMember>> = selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeMembers(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val invites: StateFlow<List<InviteToken>> = selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeInvites(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expenseItems: StateFlow<List<ExpenseItem>> = selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeExpenseItems(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val assignments: StateFlow<List<ItemAssignment>> = selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeAssignments(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            _regions.value = repository.getRegions()
        }
    }

    fun selectGroup(groupId: Long) {
        _selectedGroupId.value = groupId
        _settlements.value = emptyList()
        _settlementWarnings.value = emptyList()
    }

    fun leaveSelectedGroupForDebug() {
        _selectedGroupId.value = null
        _settlements.value = emptyList()
        _settlementWarnings.value = emptyList()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun createGroup(groupName: String, adminName: String) {
        viewModelScope.launch {
            if (groupName.isBlank() || adminName.isBlank()) {
                _message.value = "Completa nombre del grupo y administrador"
                return@launch
            }
            val groupId = createGroupUseCase(groupName, adminName)
            _message.value = "Grupo creado"
            _selectedGroupId.value = groupId
        }
    }

    fun updateSelectedGroupName(name: String) {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null || name.isBlank()) {
                _message.value = "Selecciona grupo y nombre valido"
                return@launch
            }
            updateGroupNameUseCase(groupId, name)
            _message.value = "Nombre del grupo actualizado"
        }
    }

    fun deleteMember(memberId: Long) {
        viewModelScope.launch {
            deleteMemberUseCase(memberId)
            val groupId = _selectedGroupId.value
            if (groupId != null) {
                recalculateSettlementSilently(groupId)
            }
            _message.value = "Integrante eliminado"
        }
    }

    fun deleteSelectedGroup() {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null) {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            deleteGroupUseCase(groupId)
            _selectedGroupId.value = null
            _settlements.value = emptyList()
            _settlementWarnings.value = emptyList()
            _message.value = "Grupo eliminado"
        }
    }

    fun generateInvite() {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null) {
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

    fun selectRegion(region: String) {
        _selectedRegion.value = region
        viewModelScope.launch {
            _recommendations.value = repository.getRecommendationsByRegion(region)
        }
    }

    fun addExpenseItem(name: String, unitPriceText: String, quantityText: String) {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null) {
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

    fun assignItem(itemId: Long, memberId: Long, quantityText: String) {
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
                    val groupId = _selectedGroupId.value
                    if (groupId != null) recalculateSettlementSilently(groupId)
                }
                is AssignmentOutcome.Rejected -> _message.value = when (outcome.reason) {
                    AssignmentRejectionReason.OVER_ASSIGNED -> "La cantidad asignada supera la cantidad del item"
                    AssignmentRejectionReason.NEGATIVE_QUANTITY -> "Cantidad invalida"
                }
            }
        }
    }

    fun deleteExpenseItem(itemId: Long) {
        viewModelScope.launch {
            deleteExpenseItemUseCase(itemId)
            val groupId = _selectedGroupId.value
            if (groupId != null) {
                recalculateSettlementSilently(groupId)
            }
        }
    }

    fun calculateSettlement() {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null) {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            updateSettlement(calculateSettlementUseCase(groupId))
            _message.value = "Division calculada"
        }
    }

    fun refreshSettlement() {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value ?: return@launch
            updateSettlement(calculateSettlementUseCase(groupId))
        }
    }

    private suspend fun recalculateSettlementSilently(groupId: Long) {
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
