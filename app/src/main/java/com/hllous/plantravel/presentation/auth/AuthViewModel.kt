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

private fun Throwable.toFriendlyAuthMessage(): String {
    val raw = message?.lowercase() ?: return "Ocurrió un error inesperado"
    return when {
        "weak_password" in raw                         -> "La contraseña debe tener al menos 6 caracteres"
        "invalid_credentials" in raw                   -> "Email o contraseña incorrectos"
        "invalid login credentials" in raw             -> "Email o contraseña incorrectos"
        "email_not_confirmed" in raw                   -> "Confirmá tu email antes de iniciar sesión"
        "user_already_exists" in raw                   -> "Ya existe una cuenta con ese email"
        "already registered" in raw                    -> "Ya existe una cuenta con ese email"
        "email_address_invalid" in raw                 -> "El email ingresado no es válido"
        "invalid_email" in raw                         -> "El email ingresado no es válido"
        "over_request_rate_limit" in raw               -> "Demasiados intentos. Esperá unos minutos e intentá de nuevo"
        "rate limit" in raw                            -> "Demasiados intentos. Esperá unos minutos e intentá de nuevo"
        "email_address_not_authorized" in raw          -> "Este email no está autorizado para registrarse"
        "signup_disabled" in raw                       -> "El registro está deshabilitado en este momento"
        "user_not_found" in raw                        -> "No existe una cuenta con ese email"
        "network" in raw || "unable to resolve" in raw -> "Sin conexión a internet. Verificá tu red"
        "timeout" in raw                               -> "La solicitud tardó demasiado. Intentá de nuevo"
        else                                           -> "Ocurrió un error inesperado. Intentá de nuevo"
    }
}

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
                .onFailure { _state.value = AuthState.Error(it.toFriendlyAuthMessage()) }
        }
    }

    fun register(email: String, password: String, displayName: String) {
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.register(email, password, displayName)
                .onFailure { _state.value = AuthState.Error(it.toFriendlyAuthMessage()) }
        }
    }

    fun createProfile(displayName: String, phone: String) {
        val userId = (_state.value as? AuthState.NeedsProfileSetup)?.userId ?: return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            authRepository.createProfile(userId, displayName, phone)
                .onSuccess { _state.value = AuthState.Authenticated(userId, displayName) }
                .onFailure { _state.value = AuthState.Error(it.toFriendlyAuthMessage()) }
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }

    fun loginWithGoogle() {
        viewModelScope.launch {
            authRepository.loginWithGoogle()
                .onFailure { _state.value = AuthState.Error(it.toFriendlyAuthMessage()) }
        }
    }

    fun setPendingInviteCode(code: String) {
        _pendingInviteCode.value = code
    }

    fun clearPendingInviteCode() {
        _pendingInviteCode.value = null
    }

    fun refreshDisplayName() {
        val userId = (_state.value as? AuthState.Authenticated)?.userId ?: return
        viewModelScope.launch {
            val name = authRepository.getDisplayName(userId) ?: return@launch
            _state.value = AuthState.Authenticated(userId, name)
        }
    }

    fun clearError() {
        _state.value = AuthState.Unauthenticated
    }
}
