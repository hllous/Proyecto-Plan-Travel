package com.hllous.plantravel.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class NeedsProfileSetup(val userId: String) : AuthState()
    data class Authenticated(val userId: String, val displayName: String) : AuthState()
    data class Error(val message: String) : AuthState()
}

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state

    val userEmail: StateFlow<String?> = authRepository.observeUserEmail()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _pendingInviteCode = MutableStateFlow<String?>(null)
    val pendingInviteCode: StateFlow<String?> = _pendingInviteCode

    init {
        viewModelScope.launch {
            authRepository.observeUserId().collect { userId ->
                if (userId == null) {
                    _state.value = AuthState.Unauthenticated
                    return@collect
                }
                val displayName = authRepository.getDisplayName(userId)
                _state.value = if (displayName != null) {
                    AuthState.Authenticated(userId, displayName)
                } else {
                    AuthState.NeedsProfileSetup(userId)
                }
            }
        }
    }

    fun login(email: String, password: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.login(email, password)
                .onFailure { _state.value = AuthState.Error(it.message ?: "Error al iniciar sesión") }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.register(email, password, displayName)
                .onFailure { _state.value = AuthState.Error(it.message ?: "Error al registrarse") }
        }
    }

    fun createProfile(displayName: String, phone: String) {
        val userId = (_state.value as? AuthState.NeedsProfileSetup)?.userId ?: return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.createProfile(userId, displayName, phone)
                .onSuccess { _state.value = AuthState.Authenticated(userId, displayName) }
                .onFailure { _state.value = AuthState.Error(it.message ?: "Error al guardar perfil") }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            authRepository.loginWithGoogle()
                .onFailure { _state.value = AuthState.Error(it.message ?: "Error al iniciar sesión con Google") }
        }
    }

    fun setPendingInviteCode(code: String) {
        _pendingInviteCode.value = code
    }

    fun clearPendingInviteCode() {
        _pendingInviteCode.value = null
    }

    fun clearError() {
        _state.value = AuthState.Unauthenticated
    }
}
