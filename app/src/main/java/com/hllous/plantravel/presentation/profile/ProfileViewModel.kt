package com.hllous.plantravel.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.AuthRepository
import com.hllous.plantravel.domain.auth.ProfileDetails
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileEditorState(
    val displayName: String = "",
    val phone: String = "",
    val mpAlias: String = "",
    val isLoading: Boolean = true,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val authRepository: AuthRepository,
    private val sessionProvider: SessionProvider,
) : ViewModel() {

    private val _editorState = MutableStateFlow(ProfileEditorState())
    val editorState: StateFlow<ProfileEditorState> = _editorState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    private val _profileUpdated = MutableStateFlow(false)
    val profileUpdated: StateFlow<Boolean> = _profileUpdated.asStateFlow()

    init {
        viewModelScope.launch {
            val userId = sessionProvider.userId ?: return@launch
            val profileDetails = authRepository.getProfileDetails(userId)
            val alias = profileDetails?.mpAlias ?: runCatching { repository.getMpAlias(userId) }.getOrNull().orEmpty()
            _editorState.value = (profileDetails ?: ProfileDetails(
                displayName = sessionProvider.displayName.orEmpty(),
                phone = "",
                mpAlias = alias,
            )).toEditorState(aliasOverride = alias)
        }
    }

    fun saveProfile(displayName: String, phone: String, mpAlias: String) {
        val trimmedName = displayName.trim()
        val trimmedPhone = phone.trim()
        val trimmedAlias = mpAlias.trim()
        if (trimmedName.isBlank()) {
            _message.value = "El nombre no puede estar vacío"
            return
        }
        viewModelScope.launch {
            _editorState.value = _editorState.value.copy(isLoading = true)
            val result = authRepository.updateProfile(trimmedName, trimmedPhone, trimmedAlias)
            if (result.isSuccess) {
                runCatching { repository.broadcastDisplayNameChanged() }
                _editorState.value = ProfileEditorState(
                    displayName = trimmedName,
                    phone = trimmedPhone,
                    mpAlias = trimmedAlias,
                    isLoading = false,
                )
                _profileUpdated.value = true
                _message.value = "Perfil actualizado"
            } else {
                _editorState.value = _editorState.value.copy(isLoading = false)
                _message.value = "Error al guardar perfil"
            }
        }
    }

    fun clearProfileUpdated() {
        _profileUpdated.value = false
    }

    fun clearMessage() {
        _message.value = null
    }

    private fun ProfileDetails.toEditorState(aliasOverride: String = mpAlias) = ProfileEditorState(
        displayName = displayName,
        phone = phone,
        mpAlias = aliasOverride,
        isLoading = false,
    )
}
