package com.hllous.plantravel.presentation.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.repository.TravelRepository
import kotlinx.serialization.Serializable
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
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

data class ItineraryEventByDay(
    val date: String,
    val events: List<ItineraryEvent>,
)

@Serializable
data class ItineraryEventDraft(
    val name: String,
    val description: String?,
    val placeId: String?,
    val date: String? = null,
)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
) : ViewModel() {

    private val _reloadTrigger = MutableStateFlow(0)
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    val events: StateFlow<UiState<List<ItineraryEventByDay>>> = _reloadTrigger
        .flatMapLatest {
            val groupId = selectedGroupHolder.selectedGroupId.value
                ?: return@flatMapLatest flowOf(UiState.Success(emptyList()))
            repository.observeItineraryEvents(groupId)
                .map<List<ItineraryEvent>, UiState<List<ItineraryEventByDay>>> { list ->
                    val grouped = list
                        .groupBy { it.date }
                        .entries
                        .sortedBy { it.key }
                        .map { (date, dayEvents) ->
                            ItineraryEventByDay(
                                date = date,
                                events = dayEvents.sortedWith(compareBy(nullsLast()) { it.timeOfDay }),
                            )
                        }
                    UiState.Success(grouped)
                }
                .catch { emit(UiState.Error(it.message ?: "Error al cargar itinerario")) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState.Loading)

    fun createEvent(
        name: String,
        date: String,
        timeOfDay: String? = null,
        description: String? = null,
        placeId: String? = null,
        endDate: String? = null,
    ) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = runCatching { repository.createItineraryEvent(groupId, name, date, timeOfDay, description, placeId, endDate) }
                _message.value = if (result.isSuccess) "Evento guardado" else "Error al guardar el evento"
                reloadEvents()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun updateEvent(
        eventId: String,
        name: String,
        date: String,
        timeOfDay: String? = null,
        description: String? = null,
        endDate: String? = null,
    ) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = runCatching { repository.updateItineraryEvent(eventId, name, date, timeOfDay, description, endDate) }
                _message.value = if (result.isSuccess) "Evento guardado" else "Error al guardar el evento"
                reloadEvents()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val result = runCatching { repository.deleteItineraryEvent(eventId) }
                _message.value = if (result.isSuccess) "Evento eliminado" else "Error al eliminar el evento"
                reloadEvents()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun buildEventFromPoi(place: PlaceResult): ItineraryEventDraft = ItineraryEventDraft(
        name = place.name,
        description = place.address,
        placeId = place.placeId,
        date = null,
    )

    fun reloadEvents() {
        _reloadTrigger.value++
    }
}
