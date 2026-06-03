package com.hllous.plantravel.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.repository.TravelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
) : ViewModel() {

    private val _mpAlias = MutableStateFlow<String>("")
    val mpAlias: StateFlow<String> = _mpAlias

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message

    init {
        viewModelScope.launch {
            val userId = sessionProvider.userId ?: return@launch
            _mpAlias.value = runCatching { repository.getMpAlias(userId) }.getOrNull().orEmpty()
        }
    }

    fun updateMpAlias(alias: String) {
        viewModelScope.launch {
            val result = runCatching { repository.updateMpAlias(alias.trim()) }
            if (result.isSuccess) {
                _mpAlias.value = alias.trim()
                _message.value = "Alias actualizado"
            } else {
                _message.value = "Error al guardar alias"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
