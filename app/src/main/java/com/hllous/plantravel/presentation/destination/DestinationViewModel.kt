package com.hllous.plantravel.presentation.destination

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hllous.plantravel.data.destination.DestinationFallbackImage
import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.data.destination.ProvinceRegionMapper
import com.hllous.plantravel.domain.auth.SessionProvider
import com.hllous.plantravel.domain.model.DestinationDraft
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.RankedRecommendations
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.domain.places.PlacesApiClient
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

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
    private val provinceRegionMapper = ProvinceRegionMapper.argentinaDefaults()
    private val photoLoadingDestinationKeys = mutableSetOf<String>()

    private val _regionDestinations = MutableStateFlow<List<StoredDestination>>(emptyList())
    val regionDestinations: StateFlow<List<StoredDestination>> = _regionDestinations.asStateFlow()

    private val _searchDestinations = MutableStateFlow<List<StoredDestination>>(emptyList())
    val searchDestinations: StateFlow<List<StoredDestination>> = _searchDestinations.asStateFlow()

    private val _destinationPhotoUrls = MutableStateFlow<Map<String, String>>(emptyMap())
    val destinationPhotoUrls: StateFlow<Map<String, String>> = _destinationPhotoUrls.asStateFlow()

    private val _poisByCategory = MutableStateFlow<UiState<RankedRecommendations>>(UiState.Loading)
    val poisByCategory: StateFlow<UiState<RankedRecommendations>> = _poisByCategory.asStateFlow()

    private val _reloadTrigger = MutableStateFlow(0)

    internal var googleDestinationPhotoFetcher: suspend (StoredDestination) -> String? = { destination ->
        fetchGooglePhotoUrl(destination)
    }

    internal var wikipediaDestinationPhotoFetcher: suspend (StoredDestination) -> String? = { destination ->
        fetchWikipediaPhotoUrl(destination)
    }

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
        viewModelScope.launch {
            val destinations = repository.browseDestinations(region)
            _regionDestinations.value = destinations
            loadPhotosFor(destinations)
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
            val shouldRetryFallback = cached != null &&
                DestinationFallbackImage.regionSlugFromToken(cached) != null &&
                destination.displayPhotoUrl.isNullOrBlank()
            when {
                !destination.displayPhotoUrl.isNullOrBlank() -> {
                    _destinationPhotoUrls.update { it + (key to destination.displayPhotoUrl) }
                }

                (cached != null && !shouldRetryFallback) || key in photoLoadingDestinationKeys -> Unit

                else -> {
                    photoLoadingDestinationKeys += key
                    viewModelScope.launch {
                        try {
                            val url = resolveDestinationPhotoUrl(destination) ?: return@launch
                            _destinationPhotoUrls.update { it + (key to url) }
                        } finally {
                            photoLoadingDestinationKeys -= key
                        }
                    }
                }
            }
        }
    }

    private suspend fun resolveDestinationPhotoUrl(destination: StoredDestination): String? {
        val googleUrl = googleDestinationPhotoFetcher(destination)?.takeIf { it.isNotBlank() }
        if (googleUrl != null) {
            if (destination.id.isNotBlank()) {
                repository.updateDestinationPhoto(
                    destinationId = destination.id,
                    googlePhotoUrl = googleUrl,
                    displayPhotoUrl = googleUrl,
                )
            }
            return googleUrl
        }

        val wikipediaUrl = wikipediaDestinationPhotoFetcher(destination)?.takeIf { it.isNotBlank() }
        if (wikipediaUrl != null) {
            if (destination.id.isNotBlank()) {
                repository.updateDestinationPhoto(
                    destinationId = destination.id,
                    wikipediaTitle = destination.name,
                    wikipediaPhotoUrl = wikipediaUrl,
                    displayPhotoUrl = wikipediaUrl,
                )
            }
            return wikipediaUrl
        }

        return DestinationFallbackImage.tokenFor(destination)
    }

    private suspend fun fetchGooglePhotoUrl(destination: StoredDestination): String? {
        val query = "${destination.name}, ${destination.province}, Argentina"
        return runCatching {
            placesApiClient.searchDestinations(query)
                .asSequence()
                .filter(::isLocalityLike)
                .filter { it.photoUrl.isNotBlank() }
                .sortedByDescending { scoreDestinationMatch(it, destination) }
                .firstOrNull()
                ?.photoUrl
        }.getOrNull()
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

    private suspend fun fetchWikipediaPhotoUrl(destination: StoredDestination): String? = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: direct name lookup on Spanish Wikipedia.
            // On es.wikipedia.org, "Gaiman" = the Patagonian town, not Neil Gaiman (who is "Neil Gaiman").
            val direct = wikipediaThumbnail(destination.name)
            if (direct != null) return@runCatching direct

            // Step 2: parenthetical disambiguation form → "Cervantes (Río Negro)".
            // Needed when the bare name is a disambiguation page or maps to a different article.
            val parenthetical = wikipediaThumbnail("${destination.name} (${destination.province})")
            if (parenthetical != null) return@runCatching parenthetical

            // Step 3: coordinate geosearch — catches towns whose Spanish Wikipedia article
            // uses a different name or has no article at all; prefer titles that START WITH
            // the destination name to avoid picking train stations / museums over the town.
            wikipediaThumbnailByGeosearch(destination)
        }.getOrNull()
    }

    private fun wikipediaThumbnail(title: String): String? {
        val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "_")
        val conn = URL("https://es.wikipedia.org/api/rest_v1/page/summary/$encoded")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "PlanTravelApp/1.0 (Android)")
        conn.connect()
        if (conn.responseCode != 200) return null
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        if (json.optString("type") == "disambiguation") return null
        return json.optJSONObject("thumbnail")?.optString("source")?.takeIf { it.isNotBlank() }
    }

    private fun wikipediaThumbnailByGeosearch(destination: StoredDestination): String? {
        val conn = URL(
            "https://es.wikipedia.org/w/api.php?action=query&list=geosearch" +
                "&gscoord=${destination.lat}|${destination.lng}&gsradius=10000&gslimit=5&format=json"
        ).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "PlanTravelApp/1.0 (Android)")
        conn.connect()
        if (conn.responseCode != 200) return null

        val results = JSONObject(conn.inputStream.bufferedReader().readText())
            .optJSONObject("query")?.optJSONArray("geosearch") ?: return null
        val destNorm = DestinationTextNormalizer.normalize(destination.name)

        // Prefer titles that begin with the destination name (e.g. "Cervantes (Río Negro)")
        // over ones that merely contain it (e.g. "Estación Alejandro Magariños Cervantes").
        var startsWithMatch: String? = null
        var containsMatch: String? = null
        var firstTitle: String? = null
        for (i in 0 until results.length()) {
            val title = results.optJSONObject(i)?.optString("title") ?: continue
            val norm = DestinationTextNormalizer.normalize(title)
            if (firstTitle == null) firstTitle = title
            if (startsWithMatch == null && norm.startsWith(destNorm)) startsWithMatch = title
            if (containsMatch == null && norm.contains(destNorm)) containsMatch = title
        }
        val best = startsWithMatch ?: containsMatch ?: firstTitle ?: return null
        return wikipediaThumbnail(best)
    }

    fun setTripDestination(destination: StoredDestination) {
        val groupId = selectedGroupHolder.selectedGroupId.value ?: return
        viewModelScope.launch {
            val storedDestination = if (destination.id.isBlank()) {
                repository.upsertDestination(destination.toDraft())
            } else {
                destination
            }

            val placeId = storedDestination.googlePlaceId
                ?: resolveGooglePlaceId(storedDestination)
                ?: storedDestination.sourceId.takeIf { storedDestination.source == "google" }
                ?: storedDestination.name

            runCatching {
                repository.setTripDestination(
                    groupId = groupId,
                    placeId = placeId,
                    name = storedDestination.name,
                    lat = storedDestination.lat,
                    lng = storedDestination.lng,
                )
            }
            reloadGroups()
        }
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
