package com.hllous.plantravel.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.usecase.ConsumeInviteUseCase
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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val generateInviteUseCase: GenerateInviteUseCase,
    private val deleteInviteUseCase: DeleteInviteUseCase,
    private val consumeInviteUseCase: ConsumeInviteUseCase,
) : ViewModel() {

    val selectedGroupId: StateFlow<String?> = selectedGroupHolder.selectedGroupId.asStateFlow()

    private val _invitesRetryTrigger = MutableStateFlow(0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _regions = MutableStateFlow<List<String>>(emptyList())
    val regions: StateFlow<List<String>> = _regions

    private val _selectedRegion = MutableStateFlow<String?>(null)
    val selectedRegion: StateFlow<String?> = _selectedRegion

    private val _recommendations = MutableStateFlow<List<DestinationRecommendation>>(emptyList())
    val recommendations: StateFlow<List<DestinationRecommendation>> = _recommendations

    val invites: StateFlow<List<InviteToken>> = combine(
        selectedGroupHolder.selectedGroupId,
        _invitesRetryTrigger
    ) { id, _ -> id }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeInvites(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            runCatching { _regions.value = repository.getRegions() }
        }
    }

    fun reloadInvites() {
        _invitesRetryTrigger.value++
    }

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
            result.onSuccess { groupId -> selectedGroupHolder.selectedGroupId.value = groupId }
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
            runCatching { generateInviteUseCase(groupId) }
                .onSuccess {
                    reloadInvites()
                    _message.value = "Invitacion generada"
                }
                .onFailure { _message.value = "Error al generar invitacion" }
        }
    }

    fun deleteInvite(code: String) {
        viewModelScope.launch {
            runCatching { deleteInviteUseCase(code) }
                .onSuccess {
                    reloadInvites()
                    _message.value = "Invitacion eliminada"
                }
                .onFailure { _message.value = "Error al eliminar invitacion" }
        }
    }

    fun selectRegion(region: String) {
        _selectedRegion.value = region
        viewModelScope.launch {
            runCatching { _recommendations.value = repository.getRecommendationsByRegion(region) }
        }
    }
}
