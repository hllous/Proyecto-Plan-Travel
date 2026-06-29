package com.hllous.plantravel.presentation.destination

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.data.destination.DestinationFallbackImage
import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.data.destination.ProvinceRegionMapper
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.destination.DestinationPhotoResolver
import com.hllous.plantravel.domain.model.DestinationDraft
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.domain.model.RankedRecommendations
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.domain.places.PlacesApiClient
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import kotlinx.coroutines.flow.update
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

data class HomeFeedItem(val place: PlaceResult, val category: String)

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class DestinationViewModel @Inject constructor(
    private val repository: TravelRepository,
    private val placesApiClient: PlacesApiClient,
    private val sessionProvider: SessionProvider,
    private val selectedGroupHolder: SelectedGroupHolder,
    private val ranker: PlaceRecommendationRanker,
    private val photoResolver: DestinationPhotoResolver,
) : ViewModel() {
    private val provinceRegionMapper = ProvinceRegionMapper.argentinaDefaults()
    private val photoLoadingDestinationKeys = mutableSetOf<String>()

    private val _regionDestinations = MutableStateFlow<List<StoredDestination>>(emptyList())
    val regionDestinations: StateFlow<List<StoredDestination>> = _regionDestinations.asStateFlow()

    private val _regionLoading = MutableStateFlow(false)
    val regionLoading: StateFlow<Boolean> = _regionLoading.asStateFlow()

    private val _searchDestinations = MutableStateFlow<List<StoredDestination>>(emptyList())
    val searchDestinations: StateFlow<List<StoredDestination>> = _searchDestinations.asStateFlow()

    private val _destinationPhotoUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val destinationPhotoUrls: StateFlow<Map<String, String>> = _destinationPhotoUrls.asStateFlow()

    private val _poisByCategory = MutableStateFlow<UiState<RankedRecommendations>>(UiState.Loading)
    val poisByCategory: StateFlow<UiState<RankedRecommendations>> = _poisByCategory.asStateFlow()

    private val _homeFeed = MutableStateFlow<UiState<List<HomeFeedItem>>>(UiState.Loading)
    val homeFeed: StateFlow<UiState<List<HomeFeedItem>>> = _homeFeed.asStateFlow()
    private var _lastLoadedFeedPlaceId: String? = null

    private val _recommendedDestinations = MutableStateFlow<UiState<List<StoredDestination>>>(UiState.Loading)
    val recommendedDestinations: StateFlow<UiState<List<StoredDestination>>> = _recommendedDestinations.asStateFlow()

    private val _pendingPoi = MutableStateFlow<PlaceResult?>(null)
    val pendingPoi: StateFlow<PlaceResult?> = _pendingPoi.asStateFlow()

    private val _pendingCategory = MutableStateFlow<String?>(null)
    val pendingCategory: StateFlow<String?> = _pendingCategory.asStateFlow()

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)

    val tripDestination: StateFlow<TripDestinationState> = combine(
        selectedGroupHolder.selectedGroupId,
        _reloadTrigger,
    ) { groupId, _ -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(TripDestinationState.None)
            else repository.observeGroups().map { groups ->
                val group = groups.firstOrNull { it.id == groupId }
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

    val activeDestPoll: StateFlow<Poll?> = combine(
        selectedGroupHolder.selectedGroupId,
        _reloadTrigger,
    ) { groupId, _ -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(null)
            else repository.observeActivePoll(groupId)
                .map { poll -> poll?.takeIf { it.state == PollState.OPEN } }
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeActivityPoll: StateFlow<Poll?> = combine(
        selectedGroupHolder.selectedGroupId,
        _reloadTrigger,
    ) { groupId, _ -> groupId }
        .flatMapLatest { groupId ->
            if (groupId == null) flowOf(null)
            else repository.observeActiveActivityPolls(groupId)
                .map { it.firstOrNull() }
                .catch { emit(null) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Any active poll (activity takes priority) — used for the tab bar indicator and banners.
    val activePoll: StateFlow<Poll?> = combine(activeDestPoll, activeActivityPoll) { dest, act ->
        act ?: dest
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectRegion(region: String) {
        viewModelScope.launch {
            _regionLoading.value = true
            try {
                val destinations = repository.browseDestinations(region)
                _regionDestinations.value = destinations
                loadPhotosFor(destinations)
            } finally {
                _regionLoading.value = false
            }
        }
    }

    fun search(query: String) {
        if (query.isBlank()) {
            _searchDestinations.value = emptyList()
            return
        }

        viewModelScope.launch {
            val stored = repository.searchDestinations(query)
            val combined = if (shouldUseGoogleFallback(stored, query)) {
                stored + fallbackGoogleDestinations(query, stored)
            } else {
                stored
            }
            _searchDestinations.value = combined
            loadPhotosFor(combined)
        }
    }

    private fun shouldUseGoogleFallback(
        stored: List<StoredDestination>,
        query: String,
    ): Boolean {
        if (stored.isEmpty()) return true
        val normalizedQuery = DestinationTextNormalizer.normalize(query)
        return stored.none {
            it.normalizedName.startsWith(normalizedQuery) ||
                DestinationTextNormalizer.normalize(it.province).startsWith(normalizedQuery)
        }
    }

    private suspend fun fallbackGoogleDestinations(
        query: String,
        stored: List<StoredDestination>,
    ): List<StoredDestination> = runCatching {
        placesApiClient.searchDestinations("$query, Argentina")
            .filter(::isLocalityLike)
            .mapNotNull(::toTransientDestination)
            .filterNot { fallback ->
                stored.any {
                    it.googlePlaceId == fallback.googlePlaceId ||
                        it.normalizedName == fallback.normalizedName
                }
            }
    }.getOrDefault(emptyList())

    private fun isLocalityLike(place: PlaceResult): Boolean {
        val types = buildSet {
            place.primaryType?.let(::add)
            addAll(place.types)
        }
        return types.any {
            it == "locality" ||
                it == "postal_town" ||
                it == "administrative_area_level_3" ||
                it == "sublocality"
        }
    }

    private fun toTransientDestination(place: PlaceResult): StoredDestination? {
        val province = extractProvince(place.address) ?: return null
        val region = provinceRegionMapper.regionFor(province) ?: return null
        return StoredDestination(
            id = "",
            source = "google",
            sourceId = place.placeId,
            name = place.name,
            normalizedName = DestinationTextNormalizer.normalize(place.name),
            province = province,
            region = region,
            countryCode = "AR",
            lat = place.lat,
            lng = place.lng,
            population = 0,
            googlePlaceId = place.placeId,
            googlePhotoUrl = place.photoUrl.ifBlank { null },
            displayPhotoUrl = place.photoUrl.ifBlank { null },
            isActive = true,
        )
    }

    private fun extractProvince(address: String): String? {
        val normalizedAddress = DestinationTextNormalizer.normalize(address)
        return argentinaProvinceNames.firstOrNull { province ->
            normalizedAddress.contains(DestinationTextNormalizer.normalize(province))
        }
    }

    private val argentinaProvinceNames = listOf(
        "Buenos Aires",
        "CABA",
        "Catamarca",
        "Chaco",
        "Chubut",
        "Córdoba",
        "Corrientes",
        "Entre Ríos",
        "Formosa",
        "Jujuy",
        "La Pampa",
        "La Rioja",
        "Mendoza",
        "Misiones",
        "Neuquén",
        "Río Negro",
        "Salta",
        "San Juan",
        "San Luis",
        "Santa Cruz",
        "Santa Fe",
        "Santiago del Estero",
        "Tierra del Fuego",
        "Tucumán",
    )

    private fun loadPhotosFor(destinations: List<StoredDestination>) {
        destinations.forEach { destination ->
            val key = destination.photoKey()
            val cached = _destinationPhotoUrls.value[key]
            val dbPhoto = destination.displayPhotoUrl
            val dbPhotoIsReal = !dbPhoto.isNullOrBlank() &&
                DestinationFallbackImage.regionSlugFromToken(dbPhoto) == null
            val shouldRetryFallback = cached != null &&
                DestinationFallbackImage.regionSlugFromToken(cached) != null &&
                !dbPhotoIsReal
            when {
                dbPhotoIsReal -> {
                    val displayUrl = if (dbPhoto!!.startsWith("places/")) {
                        placesApiClient.resolvePhotoUrl(dbPhoto)
                    } else {
                        dbPhoto
                    }
                    _destinationPhotoUrls.update { it + (key to displayUrl) }
                }

                (cached != null && !shouldRetryFallback) || key in photoLoadingDestinationKeys -> Unit

                else -> {
                    photoLoadingDestinationKeys += key
                    viewModelScope.launch {
                        try {
                            val url = photoResolver.resolve(destination) ?: return@launch
                            _destinationPhotoUrls.update { it + (key to url) }
                        } finally {
                            photoLoadingDestinationKeys -= key
                        }
                    }
                }
            }
        }
    }

    private fun scoreDestinationMatch(place: PlaceResult, destination: StoredDestination): Int {
        val name = DestinationTextNormalizer.normalize(place.name)
        val address = DestinationTextNormalizer.normalize(place.address)
        val destinationName = destination.normalizedName
        val province = DestinationTextNormalizer.normalize(destination.province)

        var score = 0
        if (name.contains(destinationName)) score += 3
        if (address.contains(destinationName)) score += 2
        if (address.contains(province)) score += 1
        return score
    }

    fun addDestinationToPoll(destination: StoredDestination, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val pollId = resolveActivePoll(expectedType = PollType.DESTINATION)?.id ?: run {
                onResult(false)
                return@launch
            }
            val result = runCatching {
                repository.addPollCandidate(
                    pollId = pollId,
                    placeId = destination.googlePlaceId ?: destination.sourceId,
                    name = destination.name,
                    photoUrl = destination.displayPhotoUrl.orEmpty(),
                    lat = destination.lat,
                    lng = destination.lng,
                )
            }
            onResult(result.isSuccess)
        }
    }

    fun addPoiToPoll(place: PlaceResult, onNavigate: () -> Unit) {
        viewModelScope.launch {
            val pollId = resolveActivePoll(expectedType = PollType.ACTIVITY)?.id ?: return@launch
            val result = runCatching {
                repository.addPollCandidate(
                    pollId = pollId,
                    placeId = place.placeId,
                    name = place.name,
                    photoUrl = place.photoUrl,
                    lat = place.lat,
                    lng = place.lng,
                )
            }
            if (result.isSuccess) onNavigate()
            else _message.value = "Error al agregar a la encuesta"
        }
    }

    fun createPollWithPoi(place: PlaceResult, onNavigate: () -> Unit) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            val existingActivityPoll = activeActivityPoll.value
            val pollId = when {
                existingActivityPoll == null -> runCatching {
                    repository.createPoll(groupId, PollType.ACTIVITY, "¿Qué hacemos?", null)
                }.onSuccess { reloadGroups() }.getOrNull()
                else -> existingActivityPoll.id
            } ?: return@launch
            onNavigate()
            runCatching {
                repository.addPollCandidate(
                    pollId = pollId,
                    placeId = place.placeId,
                    name = place.name,
                    photoUrl = place.photoUrl,
                    lat = place.lat,
                    lng = place.lng,
                )
            }
        }
    }

    fun createPollWithDestination(destination: StoredDestination, onNavigate: () -> Unit) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            val existingPoll = resolveActivePoll()
            val pollId = when {
                existingPoll == null -> runCatching {
                    repository.createPoll(groupId, PollType.DESTINATION, "¿A dónde vamos?", null)
                }.onSuccess { reloadGroups() }.getOrNull()
                existingPoll.type == PollType.DESTINATION -> existingPoll.id
                else -> null
            } ?: return@launch
            onNavigate()
            runCatching {
                repository.addPollCandidate(
                    pollId = pollId,
                    placeId = destination.googlePlaceId ?: destination.sourceId,
                    name = destination.name,
                    photoUrl = destination.displayPhotoUrl.orEmpty(),
                    lat = destination.lat,
                    lng = destination.lng,
                )
            }
        }
    }

    fun setTripDestination(destination: StoredDestination) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            _isSubmitting.value = true
            try {
                val storedDestination = if (destination.id.isBlank()) {
                    repository.upsertDestination(destination.toDraft())
                } else {
                    destination
                }

                val placeId = storedDestination.googlePlaceId
                    ?: resolveGooglePlaceId(storedDestination)
                    ?: storedDestination.sourceId.takeIf { storedDestination.source == "google" }
                    ?: storedDestination.name

                val result = runCatching {
                    repository.setTripDestination(
                        groupId = groupId,
                        placeId = placeId,
                        name = storedDestination.name,
                        lat = storedDestination.lat,
                        lng = storedDestination.lng,
                    )
                }
                _message.value = if (result.isSuccess) "Destino actualizado" else "Error al actualizar el destino"
                reloadGroups()
            } finally {
                _isSubmitting.value = false
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    private suspend fun resolveGooglePlaceId(destination: StoredDestination): String? = runCatching {
        placesApiClient.searchDestinations("${destination.name}, ${destination.province}, Argentina")
            .filter(::isLocalityLike)
            .sortedByDescending { scoreDestinationMatch(it, destination) }
            .firstOrNull()
            ?.placeId
    }.getOrNull()

    private fun StoredDestination.toDraft(): DestinationDraft = DestinationDraft(
        source = source,
        sourceId = sourceId,
        name = name,
        province = province,
        region = region,
        countryCode = countryCode,
        lat = lat,
        lng = lng,
        population = population,
        googlePlaceId = googlePlaceId,
        googlePhotoUrl = googlePhotoUrl,
        wikipediaTitle = wikipediaTitle,
        wikipediaPhotoUrl = wikipediaPhotoUrl,
        displayPhotoUrl = displayPhotoUrl,
        isActive = isActive,
    )

    private fun StoredDestination.photoKey(): String =
        if (id.isNotBlank()) id else "$source:$sourceId"

    fun loadRecommendedDestinations() {
        if (_recommendedDestinations.value is UiState.Success) return
        viewModelScope.launch {
            _recommendedDestinations.value = UiState.Loading
            val destinations = mutableListOf<StoredDestination>()
            runCatching { destinations.addAll(repository.browseDestinations("Patagonia")) }
            runCatching { destinations.addAll(repository.browseDestinations("Buenos Aires")) }
            val capped = destinations.take(10)
            loadPhotosFor(capped)
            _recommendedDestinations.value = UiState.Success(capped)
        }
    }

    fun loadHomeFeed() {
        val dest = tripDestination.value as? TripDestinationState.Set ?: return
        if (_homeFeed.value is UiState.Success && _lastLoadedFeedPlaceId == dest.placeId) return
        _lastLoadedFeedPlaceId = dest.placeId
        viewModelScope.launch {
            _homeFeed.value = UiState.Loading
            val categories = listOf("Alojamiento", "Gastronomía", "Actividades", "Naturaleza")
            val seenPlaceIds = mutableSetOf<String>()
            val result = mutableListOf<HomeFeedItem>()
            val placesByCategory = categories.map { cat ->
                async {
                    cat to runCatching { placesApiClient.searchPois(dest.lat, dest.lng, cat) }
                        .getOrDefault(emptyList())
                }
            }.awaitAll()
            for ((cat, places) in placesByCategory) {
                val ranked = ranker.rank(places)
                (ranked.top + ranked.others)
                    .filter { seenPlaceIds.add(it.placeId) }
                    .take(2)
                    .forEach { result.add(HomeFeedItem(it, cat)) }
            }
            _homeFeed.value = UiState.Success(result)
        }
    }

    fun requestOpenPoi(place: PlaceResult, category: String) {
        _pendingCategory.value = category
        _pendingPoi.value = place
    }

    fun clearPendingPoi() {
        _pendingPoi.value = null
        _pendingCategory.value = null
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

    private suspend fun resolveActivePoll(expectedType: PollType? = null): Poll? {
        when (expectedType) {
            PollType.ACTIVITY -> return activeActivityPoll.value
            PollType.DESTINATION -> activeDestPoll.value?.let { return it }
            null -> activePoll.value?.let { poll ->
                if (expectedType == null || poll.type == expectedType) return poll
            }
        }
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return null
        val resolvedPoll = repository.fetchActivePoll(groupId)?.takeIf { it.state == PollState.OPEN }
        if (resolvedPoll != null) reloadGroups()
        return resolvedPoll?.takeIf { expectedType == null || it.type == expectedType }
    }

    private companion object {
        private const val TAG = "DestinationViewModel"
    }
}
