package com.hllous.plantravel.presentation.itinerary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.places.PlacesApiClient
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
import kotlinx.coroutines.flow.combine
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
    private val placesApiClient: PlacesApiClient,
    private val selectedGroupHolder: SelectedGroupHolder,
) : ViewModel() {

    private val _reloadTrigger = MutableStateFlow(0)
    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()
    private val _selectedPlaceDetails = MutableStateFlow<UiState<PlaceResult>?>(null)
    val selectedPlaceDetails: StateFlow<UiState<PlaceResult>?> = _selectedPlaceDetails.asStateFlow()

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

    val activityCandidates: StateFlow<List<PollCandidate>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeActiveActivityPolls(groupId)
                .flatMapLatest { polls ->
                    if (polls.isEmpty()) flowOf(emptyList())
                    else combine(polls.map { repository.observePollCandidates(it.id) }) { arrays ->
                        arrays.toList().flatten()
                    }
                }
        }
        .map { candidates ->
            candidates.onEach { candidate ->
                placesApiClient.rememberPlace(
                    PlaceResult(
                        placeId = candidate.placeId,
                        name = candidate.name,
                        photoUrl = candidate.photoUrl,
                        rating = 0.0,
                        reviewCount = 0,
                        address = "",
                        lat = candidate.lat,
                        lng = candidate.lng,
                    ),
                )
            }
            candidates
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val members: StateFlow<List<GroupMember>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId)
        }
        .catch { emit(emptyList()) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    fun loadPlaceDetails(place: PlaceResult?) {
        if (place == null || place.placeId.isBlank()) {
            _selectedPlaceDetails.value = null
            return
        }
        placesApiClient.rememberPlace(place)
        val cached = placesApiClient.getCachedPlace(place.placeId)
        if (cached != null && placesApiClient.isCachedPlaceDetailed(place.placeId)) {
            _selectedPlaceDetails.value = UiState.Success(cached)
            return
        }
        _selectedPlaceDetails.value = UiState.Loading
        viewModelScope.launch {
            _selectedPlaceDetails.value = runCatching { placesApiClient.fetchPlaceDetails(place.placeId) }.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Error al cargar la actividad") },
            )
        }
    }

    fun clearSelectedPlaceDetails() {
        _selectedPlaceDetails.value = null
    }

    fun buildEventFromPoi(place: PlaceResult): ItineraryEventDraft = ItineraryEventDraft(
        name = place.name,
        description = place.address,
        placeId = place.placeId,
        date = null,
    )

    fun getCachedPlace(placeId: String?): PlaceResult? =
        placeId?.takeIf { it.isNotBlank() }?.let(placesApiClient::getCachedPlace)

    fun reloadEvents() {
        _reloadTrigger.value++
    }
}
