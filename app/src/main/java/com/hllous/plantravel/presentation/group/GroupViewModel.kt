package com.hllous.plantravel.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.LeaveGroupUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import com.hllous.plantravel.presentation.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class GroupViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val sessionProvider: SessionProvider,
    private val createGroupUseCase: CreateGroupUseCase,
    private val updateGroupNameUseCase: UpdateGroupNameUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
    private val leaveGroupUseCase: LeaveGroupUseCase,
) : ViewModel() {

    private val _groupsRetryTrigger = MutableStateFlow(0)
    private val _membersRetryTrigger = MutableStateFlow(0)

    val groupsUiState: StateFlow<UiState<List<TravelGroup>>> = _groupsRetryTrigger
        .flatMapLatest {
            repository.observeGroups()
                .map<List<TravelGroup>, UiState<List<TravelGroup>>> { UiState.Success(it) }
                .catch { e -> emit(UiState.Error(e.message ?: "Error al cargar grupos")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    val groups: StateFlow<List<TravelGroup>> = groupsUiState
        .map { if (it is UiState.Success) it.data else emptyList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentGroup: StateFlow<TravelGroup?> = groups
        .map { it.firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Eagerly derived so selectedGroupId.value is always current without requiring a collector.
    val selectedGroupId: StateFlow<String?> = currentGroup
        .map { it?.id }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val members: StateFlow<List<GroupMember>> = combine(currentGroup, _membersRetryTrigger) { group, _ -> group?.id }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUserRole: StateFlow<MemberRole?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId }?.role }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        // Write to selectedGroupHolder so ExpenseViewModel and MainViewModel
        // (which still read from it) stay in sync with the one-group constraint.
        viewModelScope.launch {
            currentGroup.collect { group ->
                selectedGroupHolder.selectedGroupId.value = group?.id
            }
        }
    }

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _pendingKickMemberId = MutableStateFlow<String?>(null)
    val pendingKickMemberId: StateFlow<String?> = _pendingKickMemberId

    fun reloadGroups() {
        _groupsRetryTrigger.value++
    }

    fun reloadMembers() {
        _membersRetryTrigger.value++
    }

    fun clearMessage() {
        _message.value = null
    }

    fun createGroup(groupName: String) {
        viewModelScope.launch {
            if (groupName.isBlank()) {
                _message.value = "Completa el nombre del grupo"
                return@launch
            }
            val result = runCatching { createGroupUseCase(groupName) }
            if (result.isFailure) {
                _message.value = "Error al crear grupo"
                return@launch
            }
            reloadGroups()
            reloadMembers()
            _message.value = "Grupo creado"
        }
    }

    fun updateSelectedGroupName(name: String) {
        viewModelScope.launch {
            val groupId = currentGroup.value?.id
            if (groupId == null || name.isBlank()) {
                _message.value = "Selecciona grupo y nombre valido"
                return@launch
            }
            runCatching { updateGroupNameUseCase(groupId, name) }
                .onSuccess {
                    reloadGroups()
                    _message.value = "Nombre del grupo actualizado"
                }
                .onFailure { _message.value = "Error al actualizar nombre" }
        }
    }

    fun deleteSelectedGroup() {
        viewModelScope.launch {
            val groupId = currentGroup.value?.id ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            runCatching { deleteGroupUseCase(groupId) }
                .onSuccess {
                    reloadGroups()
                    reloadMembers()
                    _message.value = "Grupo eliminado"
                }
                .onFailure { _message.value = "Error al eliminar grupo" }
        }
    }

    fun leaveGroup() {
        val groupId = currentGroup.value?.id ?: run {
            _message.value = "Selecciona un grupo"
            return
        }
        val userId = sessionProvider.userId
        val currentMember = members.value.firstOrNull { it.userId == userId }
        if (currentMember?.role == MemberRole.ADMIN) {
            _message.value = "El administrador no puede abandonar el grupo"
            return
        }
        viewModelScope.launch {
            runCatching { leaveGroupUseCase(groupId) }
                .onSuccess {
                    reloadGroups()
                    reloadMembers()
                    _message.value = "Abandonaste el grupo"
                }
                .onFailure { _message.value = "Error al abandonar el grupo" }
        }
    }

    fun requestKickMember(memberId: String) {
        _pendingKickMemberId.value = memberId
    }

    fun confirmKick() {
        val memberId = _pendingKickMemberId.value ?: return
        _pendingKickMemberId.value = null
        deleteMember(memberId)
    }

    fun cancelKick() {
        _pendingKickMemberId.value = null
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            runCatching { deleteMemberUseCase(memberId) }
                .onSuccess {
                    reloadMembers()
                    _message.value = "Integrante eliminado"
                }
                .onFailure { _message.value = "Error al eliminar integrante" }
        }
    }
}
