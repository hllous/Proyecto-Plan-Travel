package com.hllous.plantravel.presentation.group

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.usecase.CreateGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteMemberUseCase
import com.hllous.plantravel.domain.usecase.UpdateGroupNameUseCase
import com.hllous.plantravel.presentation.UiState
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
class GroupViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val createGroupUseCase: CreateGroupUseCase,
    private val updateGroupNameUseCase: UpdateGroupNameUseCase,
    private val deleteGroupUseCase: DeleteGroupUseCase,
    private val deleteMemberUseCase: DeleteMemberUseCase,
) : ViewModel() {

    val selectedGroupId: StateFlow<String?> = selectedGroupHolder.selectedGroupId.asStateFlow()

    private val _groupsRetryTrigger = MutableStateFlow(0)

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

    val members: StateFlow<List<GroupMember>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    fun reloadGroups() {
        _groupsRetryTrigger.value++
    }

    fun selectGroup(groupId: String) {
        selectedGroupHolder.selectedGroupId.value = groupId
    }

    fun clearSelectedGroup() {
        selectedGroupHolder.selectedGroupId.value = null
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
            selectedGroupHolder.selectedGroupId.value = groupId
            _message.value = "Grupo creado"
        }
    }

    fun updateSelectedGroupName(name: String) {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value
            if (groupId == null || name.isBlank()) {
                _message.value = "Selecciona grupo y nombre valido"
                return@launch
            }
            updateGroupNameUseCase(groupId, name)
            _message.value = "Nombre del grupo actualizado"
        }
    }

    fun deleteSelectedGroup() {
        viewModelScope.launch {
            val groupId = selectedGroupHolder.selectedGroupId.value ?: run {
                _message.value = "Selecciona un grupo"
                return@launch
            }
            deleteGroupUseCase(groupId)
            selectedGroupHolder.selectedGroupId.value = null
            _message.value = "Grupo eliminado"
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            deleteMemberUseCase(memberId)
            _message.value = "Integrante eliminado"
        }
    }
}
