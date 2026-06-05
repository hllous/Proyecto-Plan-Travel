package com.hllous.plantravel.presentation.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.RankedRecommendations
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.domain.places.PlacesApiClient
import com.hllous.plantravel.domain.repository.TravelRepository
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

sealed class TripDestinationState {
    object None : TripDestinationState()
    data class Set(
        val placeId: String,
        val name: String,
        val lat: Double,
        val lng: Double,
    ) : TripDestinationState()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DestinationViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val placesApiClient: PlacesApiClient,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val ranker: PlaceRecommendationRanker,
) : ViewModel() {

    companion object {
        private val REGION_QUERIES = mapOf(
            "Patagonia" to "turismo Patagonia Argentina",
            "Cuyo" to "turismo Cuyo Argentina",
            "Noroeste" to "turismo Noroeste Argentina",
            "Litoral" to "turismo Litoral Argentina",
            "Buenos Aires" to "turismo Buenos Aires Argentina",
            "Córdoba" to "turismo Córdoba Argentina",
        )
    }

    private val _regionResults = MutableStateFlow<UiState<List<PlaceResult>>>(UiState.Loading)
    val regionResults: StateFlow<UiState<List<PlaceResult>>> = _regionResults.asStateFlow()

    private val _searchResults = MutableStateFlow<UiState<List<PlaceResult>>>(UiState.Loading)
    val searchResults: StateFlow<UiState<List<PlaceResult>>> = _searchResults.asStateFlow()

    private val _poisByCategory = MutableStateFlow<UiState<RankedRecommendations>>(UiState.Loading)
    val poisByCategory: StateFlow<UiState<RankedRecommendations>> = _poisByCategory.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)

    val tripDestination: StateFlow<TripDestinationState> = _reloadTrigger
        .flatMapLatest { repository.observeGroups() }
        .map { groups ->
            val group = groups.firstOrNull()
            val placeId = group?.tripDestinationPlaceId
            val name = group?.tripDestinationName
            val lat = group?.tripDestinationLat
            val lng = group?.tripDestinationLng
            if (placeId != null && name != null && lat != null && lng != null) {
                TripDestinationState.Set(placeId = placeId, name = name, lat = lat, lng = lng)
            } else {
                TripDestinationState.None
            }
        }
        .catch { emit(TripDestinationState.None) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TripDestinationState.None)

    val members: StateFlow<List<GroupMember>> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(emptyList())
            else repository.observeMembers(groupId).catch { emit(emptyList()) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMember: StateFlow<GroupMember?> = members
        .map { list -> list.firstOrNull { it.userId == sessionProvider.userId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activePoll: StateFlow<Poll?> = selectedGroupHolder.selectedGroupId
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(null)
            else repository.observeActivePoll(groupId).catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectRegion(region: String) {
        val query = REGION_QUERIES[region] ?: "turismo $region Argentina"
        _regionResults.value = UiState.Loading
        viewModelScope.launch {
            val result = runCatching { placesApiClient.searchDestinations(query) }
            _regionResults.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Error al cargar destinos") },
            )
        }
    }

    fun search(query: String) {
        _searchResults.value = UiState.Loading
        viewModelScope.launch {
            val result = runCatching { placesApiClient.searchDestinations(query) }
            _searchResults.value = result.fold(
                onSuccess = { UiState.Success(it) },
                onFailure = { UiState.Error(it.message ?: "Error al buscar destinos") },
            )
        }
    }

    fun setTripDestination(placeResult: PlaceResult) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            runCatching {
                repository.setTripDestination(
                    groupId = groupId,
                    placeId = placeResult.placeId,
                    name = placeResult.name,
                    lat = placeResult.lat,
                    lng = placeResult.lng,
                )
            }
            reloadGroups()
        }
    }

    fun selectPoiCategory(category: String) {
        val dest = tripDestination.value as? TripDestinationState.Set ?: return
        _poisByCategory.value = UiState.Loading
        viewModelScope.launch {
            val result = runCatching { placesApiClient.searchPois(dest.lat, dest.lng, category) }
            _poisByCategory.value = result.fold(
                onSuccess = { UiState.Success(ranker.rank(it)) },
                onFailure = { UiState.Error(it.message ?: "Error al cargar recomendaciones") },
            )
        }
    }

    fun reloadGroups() {
        _reloadTrigger.value++
    }
}
