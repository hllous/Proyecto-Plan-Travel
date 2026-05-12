package com.hllous.plantravel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AddMemberUseCase
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
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val createGroupUseCase: CreateGroupUseCase,
    private val addMemberUseCase: AddMemberUseCase,
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

    private val _currentMemberId = MutableStateFlow<Long?>(null)
    val currentMemberId: StateFlow<Long?> = _currentMemberId

    val groups: StateFlow<List<TravelGroup>> = repository.observeGroups()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<GroupMember>> = selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) kotlinx.coroutines.flow.flowOf(emptyList())
            else repository.observeMembers(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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
    }

    fun setCurrentMember(memberId: Long) {
        _currentMemberId.value = memberId
    }

    fun leaveSelectedGroupForDebug() {
        _selectedGroupId.value = null
        _currentMemberId.value = null
        _settlements.value = emptyList()
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

    fun addMember(memberName: String) {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value
            if (groupId == null || memberName.isBlank()) {
                _message.value = "Selecciona grupo y nombre valido"
                return@launch
            }
            addMemberUseCase(groupId, memberName)
            _message.value = "Integrante agregado"
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
            _currentMemberId.value = null
            _settlements.value = emptyList()
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

    fun consumeInvite(code: String, memberName: String) {
        viewModelScope.launch {
            if (code.isBlank() || memberName.isBlank()) {
                _message.value = "Codigo y nombre son obligatorios"
                return@launch
            }
            val result = consumeInviteUseCase(code, memberName)
            result.getOrNull()?.let { _currentMemberId.value = it }
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
            assignItemToMemberUseCase(itemId, memberId, quantity)
            val groupId = _selectedGroupId.value
            if (groupId != null) {
                recalculateSettlementSilently(groupId)
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
            _settlements.value = calculateSettlementUseCase(groupId)
            _message.value = "Division calculada"
        }
    }

    fun refreshSettlement() {
        viewModelScope.launch {
            val groupId = _selectedGroupId.value ?: return@launch
            _settlements.value = calculateSettlementUseCase(groupId)
        }
    }

    private suspend fun recalculateSettlementSilently(groupId: Long) {
        _settlements.value = calculateSettlementUseCase(groupId)
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


