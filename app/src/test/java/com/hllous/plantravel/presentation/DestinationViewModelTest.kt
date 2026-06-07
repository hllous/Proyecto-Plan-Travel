package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.data.destination.DestinationFallbackImage
import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DestinationViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakePlace = PlaceResult(
        placeId = "place-1",
        name = "Bariloche",
        photoUrl = "https://example.com/photo.jpg",
        rating = 4.8,
        reviewCount = 200,
        address = "Bariloche, Río Negro, Argentina",
        lat = -41.1335,
        lng = -71.3103,
        primaryType = "locality",
        types = listOf("locality"),
    )

    private fun storedDestination(
        id: String,
        name: String,
        region: String,
        province: String,
        population: Int = 1000,
        googlePlaceId: String? = null,
        displayPhotoUrl: String? = null,
        lat: Double = -41.0,
        lng: Double = -71.0,
    ) = StoredDestination(
        id = id,
        source = "geonames",
        sourceId = id,
        name = name,
        normalizedName = DestinationTextNormalizer.normalize(name),
        province = province,
        region = region,
        countryCode = "AR",
        lat = lat,
        lng = lng,
        population = population,
        googlePlaceId = googlePlaceId,
        displayPhotoUrl = displayPhotoUrl,
    )

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        places: FakePlacesApiClient = FakePlacesApiClient(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder(),
        ranker: PlaceRecommendationRanker = PlaceRecommendationRanker(),
    ) = DestinationViewModel(
        repository = repo,
        placesApiClient = places,
        sessionProvider = session,
        selectedGroupHolder = holder,
        ranker = ranker,
    )

    // ── Google Places photo fetcher ───────────────────────────────────────────

    @Test
    fun googlePhotoFetcherPicksLocalityAndIgnoresNonLocalityResults() = runTest {
        // Uses real fetchGooglePhotoUrl (googleDestinationPhotoFetcher not overridden).
        // Verifies client-side isLocalityLike filter — not dependent on server-side includedType.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-bari", "Bariloche", "Patagonia", "Río Negro"),
            ),
        )
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-museum",
                    name = "Museo Patagónico",
                    photoUrl = "https://example.com/museum.jpg",
                    rating = 4.5, reviewCount = 50,
                    address = "Bariloche, Río Negro, Argentina",
                    lat = -41.13, lng = -71.31,
                    primaryType = "museum",
                    types = listOf("museum", "tourist_attraction"),
                ),
                PlaceResult(
                    placeId = "place-city",
                    name = "Bariloche",
                    photoUrl = "https://example.com/bariloche.jpg",
                    rating = 4.9, reviewCount = 1000,
                    address = "Bariloche, Río Negro, Argentina",
                    lat = -41.13, lng = -71.31,
                    primaryType = "locality",
                    types = listOf("locality"),
                ),
            ),
        )
        val vm = viewModel(repo = repo, places = places).apply {
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals("https://example.com/bariloche.jpg", vm.destinationPhotoUrls.value["dest-bari"])
    }

    @Test
    fun googlePhotoFetcherAcceptsAdministrativeAreaLevel3ForSmallTowns() = runTest {
        // Small Argentine towns are often tagged administrative_area_level_3, not locality.
        // The client-side isLocalityLike filter must accept this type.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-cerv", "Cervantes", "Patagonia", "Río Negro",
                    lat = -39.28, lng = -66.99),
            ),
        )
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-cerv",
                    name = "Cervantes",
                    photoUrl = "https://example.com/cervantes-town.jpg",
                    rating = 4.2, reviewCount = 30,
                    address = "Cervantes, Río Negro, Argentina",
                    lat = -39.28, lng = -66.99,
                    primaryType = "administrative_area_level_3",
                    types = listOf("administrative_area_level_3", "political"),
                ),
            ),
        )
        val vm = viewModel(repo = repo, places = places).apply {
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(
            "https://example.com/cervantes-town.jpg",
            vm.destinationPhotoUrls.value["dest-cerv"],
        )
    }

    @Test
    fun googlePhotoFetcherFallsThroughToWikipediaWhenPlacesApiThrows() = runTest {
        // Uses real fetchGooglePhotoUrl — verifies runCatching swallows API errors
        // and the resolution chain continues to Wikipedia.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "Bariloche", "Patagonia", "Río Negro"),
            ),
        )
        val places = FakePlacesApiClient(searchDestinationsThrows = true)
        val vm = viewModel(repo = repo, places = places).apply {
            wikipediaDestinationPhotoFetcher = { "https://wikipedia.example/${it.name}.jpg" }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals("https://wikipedia.example/Bariloche.jpg", vm.destinationPhotoUrls.value["dest-1"])
    }

    @Test
    fun googleFetcherNotCalledForDestinationsWithStoredWikipediaPhoto() = runTest {
        // Destinations that already have a real displayPhotoUrl (including Wikipedia) must
        // not trigger a Google fetch — avoids burning API quota on already-resolved destinations.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(
                    "dest-wiki", "El Chaltén", "Patagonia", "Santa Cruz",
                    displayPhotoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/chalten.jpg",
                ),
            ),
        )
        var googleCallCount = 0
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { googleCallCount++; "https://google.example/chalten.jpg" }
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(0, googleCallCount)
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/chalten.jpg",
            vm.destinationPhotoUrls.value["dest-wiki"],
        )
    }

    @Test
    fun photoFetcherDoesNotStartDuplicateRequestForInFlightDestination() = runTest {
        // If loadPhotosFor is called twice before the first fetch completes, the fetcher
        // must only be called once (photoLoadingDestinationKeys deduplication).
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "Ushuaia", "Patagonia", "Tierra del Fuego"),
            ),
        )
        var fetchCount = 0
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { fetchCount++; "https://example.com/ushuaia.jpg" }
            wikipediaDestinationPhotoFetcher = { null }
        }

        // Two rapid loads before coroutines settle
        vm.selectRegion("Patagonia")
        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(1, fetchCount)
        assertEquals("https://example.com/ushuaia.jpg", vm.destinationPhotoUrls.value["dest-1"])
    }

    // ── Region / search ───────────────────────────────────────────────────────

    @Test
    fun selectRegionUsesStoredDestinationsFromRepository() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("1", "Bariloche", "Patagonia", "Río Negro", population = 135000),
                storedDestination("2", "Ushuaia", "Patagonia", "Tierra del Fuego", population = 82615),
            ),
        )
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { "https://example.com/${it.name}.jpg" }
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals("Patagonia", repo.lastBrowsedDestinationRegion)
        assertEquals(listOf("Bariloche", "Ushuaia"), vm.regionDestinations.value.map { it.name })
        assertEquals("https://example.com/Bariloche.jpg", vm.destinationPhotoUrls.value["1"])
    }

    @Test
    fun searchReturnsStoredResultsFirst() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("1", "Malargüe", "Cuyo", "Mendoza", population = 23000),
            ),
        )
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-google",
                    name = "Malargüe Centro Cívico",
                    photoUrl = "",
                    rating = 4.0,
                    reviewCount = 10,
                    address = "Malargüe, Mendoza, Argentina",
                    lat = -35.47,
                    lng = -69.58,
                    primaryType = "tourist_attraction",
                    types = listOf("tourist_attraction"),
                ),
            ),
        )
        val vm = viewModel(repo = repo, places = places)

        vm.search("malargue")
        advanceUntilIdle()

        assertEquals("malargue", repo.lastDestinationSearchQuery)
        assertEquals(listOf("Malargüe"), vm.searchDestinations.value.map { it.name })
    }

    @Test
    fun emptyStoredSearchTriggersGoogleLocalityFallback() = runTest {
        val repo = FakeTravelRepository()
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-locality",
                    name = "Belgrano",
                    photoUrl = "https://example.com/belgrano.jpg",
                    rating = 4.8,
                    reviewCount = 200,
                    address = "Belgrano, CABA, Argentina",
                    lat = -34.56,
                    lng = -58.46,
                    primaryType = "locality",
                    types = listOf("locality"),
                ),
                PlaceResult(
                    placeId = "place-poi",
                    name = "Barrio Chino",
                    photoUrl = "",
                    rating = 4.9,
                    reviewCount = 2000,
                    address = "Belgrano, Buenos Aires, Argentina",
                    lat = -34.56,
                    lng = -58.45,
                    primaryType = "tourist_attraction",
                    types = listOf("tourist_attraction"),
                ),
            ),
        )
        val vm = viewModel(repo = repo, places = places)

        vm.search("belgrano")
        advanceUntilIdle()

        assertTrue(places.lastSearchedRegion!!.startsWith("belgrano"))
        assertEquals(listOf("Belgrano"), vm.searchDestinations.value.map { it.name })
        assertEquals("place-locality", vm.searchDestinations.value.single().googlePlaceId)
    }

    @Test
    fun selectingGoogleFallbackDestinationStoresItAndSetsTripDestination() = runTest {
        val group = TravelGroup(id = "group-1", name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-pehuenia",
                    name = "Villa Pehuenia",
                    photoUrl = "https://example.com/pehuenia.jpg",
                    rating = 4.7,
                    reviewCount = 120,
                    address = "Villa Pehuenia, Neuquén, Argentina",
                    lat = -38.88,
                    lng = -71.21,
                    primaryType = "locality",
                    types = listOf("locality"),
                ),
            ),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }

        vm.search("pehuenia")
        advanceUntilIdle()
        vm.setTripDestination(vm.searchDestinations.value.single())
        advanceUntilIdle()

        assertEquals(1, repo.upsertDestinationCallCount)
        assertEquals("Villa Pehuenia", repo.lastUpsertedDestination?.name)
        assertEquals(1, repo.setTripDestinationCallCount)
        assertEquals("place-pehuenia", repo.lastTripDestinationPlaceId)
        assertTrue(vm.tripDestination.value is TripDestinationState.Set)
    }

    @Test
    fun photoResolutionFallsBackToWikipediaAndCachesSuccess() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "El Chaltén", "Patagonia", "Santa Cruz"),
            ),
        )
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { null }
            wikipediaDestinationPhotoFetcher = { dest -> "https://wikipedia.example/${dest.name}.jpg" }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals("dest-1", repo.lastUpdatedDestinationPhotoId)
        assertEquals(
            "https://wikipedia.example/El Chaltén.jpg",
            vm.destinationPhotoUrls.value["dest-1"],
        )
    }

    @Test
    fun wikipediaFetcherReceivesFullDestinationEnablingProvinceDisambiguation() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-gaiman", "Gaiman", "Patagonia", "Chubut"),
            ),
        )
        var capturedName: String? = null
        var capturedProvince: String? = null
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { null }
            wikipediaDestinationPhotoFetcher = { dest ->
                capturedName = dest.name
                capturedProvince = dest.province
                "https://es.wikipedia.example/${dest.name}-${dest.province}.jpg"
            }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals("Gaiman", capturedName)
        assertEquals("Chubut", capturedProvince)
        assertEquals(
            "https://es.wikipedia.example/Gaiman-Chubut.jpg",
            vm.destinationPhotoUrls.value["dest-gaiman"],
        )
    }

    @Test
    fun wikipediaFetcherReceivesCoordinatesForGeographicDisambiguation() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(
                    "dest-cervantes", "Cervantes", "Patagonia", "Río Negro",
                    lat = -39.123, lng = -67.456,
                ),
            ),
        )
        var capturedLat: Double? = null
        var capturedLng: Double? = null
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { null }
            wikipediaDestinationPhotoFetcher = { dest ->
                capturedLat = dest.lat
                capturedLng = dest.lng
                "https://es.wikipedia.example/geo-${dest.lat}_${dest.lng}.jpg"
            }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(-39.123, capturedLat!!, 0.0001)
        assertEquals(-67.456, capturedLng!!, 0.0001)
        assertEquals(
            "https://es.wikipedia.example/geo--39.123_-67.456.jpg",
            vm.destinationPhotoUrls.value["dest-cervantes"],
        )
    }

    @Test
    fun photoResolutionRetriesAfterInitialFailure() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "El Chaltén", "Patagonia", "Santa Cruz"),
            ),
        )
        var attempts = 0
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = {
                attempts++
                if (attempts == 1) null else "https://example.com/el-chalten.jpg"
            }
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()
        assertEquals(
            DestinationFallbackImage.tokenForRegion("Patagonia"),
            vm.destinationPhotoUrls.value["dest-1"],
        )

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(2, attempts)
        assertEquals("https://example.com/el-chalten.jpg", vm.destinationPhotoUrls.value["dest-1"])
    }

    @Test
    fun photoResolutionFallsBackToRegionArtworkAndPersistsToken() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "Lago Puelo", "Patagonia", "Chubut"),
            ),
        )
        val vm = viewModel(repo = repo).apply {
            googleDestinationPhotoFetcher = { null }
            wikipediaDestinationPhotoFetcher = { null }
        }

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertNull(repo.lastUpdatedDestinationPhotoId)
        assertEquals(
            DestinationFallbackImage.tokenForRegion("Patagonia"),
            vm.destinationPhotoUrls.value["dest-1"],
        )
    }

    @Test
    fun tripDestinationObservedFromRepositoryPropagatesToState() = runTest {
        val group = TravelGroup(
            id = "group-1",
            name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Ushuaia",
            tripDestinationLat = -54.8019,
            tripDestinationLng = -68.3030,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        val state = vm.tripDestination.value

        assertTrue(state is TripDestinationState.Set)
        state as TripDestinationState.Set
        assertEquals("place-99", state.placeId)
        assertEquals("Ushuaia", state.name)
    }

    @Test
    fun selectPoiCategoryCallsSearchPoisAndEmitsRankedRecommendations() = runTest {
        val topPlace = fakePlace.copy(placeId = "top", rating = 4.5, reviewCount = 100)
        val otherPlace = fakePlace.copy(placeId = "other", rating = 3.0, reviewCount = 10)
        val places = FakePlacesApiClient(poiResults = listOf(topPlace, otherPlace))
        val group = TravelGroup(
            id = "group-1", name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1335,
            tripDestinationLng = -71.3103,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        backgroundScope.launch { vm.poisByCategory.collect {} }
        advanceUntilIdle()

        vm.selectPoiCategory("Alojamiento")
        advanceUntilIdle()

        assertEquals(-41.1335, places.lastSearchedLat!!, 0.0001)
        assertEquals(-71.3103, places.lastSearchedLng!!, 0.0001)
        assertEquals("Alojamiento", places.lastSearchedType)

        val state = vm.poisByCategory.value
        assertTrue(state is UiState.Success)
        val ranked = (state as UiState.Success).data
        assertEquals(listOf(topPlace), ranked.top)
        assertEquals(listOf(otherPlace), ranked.others)
    }

    @Test
    fun selectPoiCategoryWhenDestinationNoneDoesNothing() = runTest {
        val places = FakePlacesApiClient()
        val vm = viewModel(places = places)

        vm.selectPoiCategory("Naturaleza")
        advanceUntilIdle()

        assertNull(places.lastSearchedType)
        assertTrue(vm.poisByCategory.value is UiState.Loading)
    }

    @Test
    fun poiApiErrorEmitsUiStateErrorIntoPoisByCategory() = runTest {
        val places = FakePlacesApiClient(searchPoisThrows = true)
        val group = TravelGroup(
            id = "group-1", name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1335,
            tripDestinationLng = -71.3103,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        backgroundScope.launch { vm.poisByCategory.collect {} }
        advanceUntilIdle()

        vm.selectPoiCategory("Gastronomía")
        advanceUntilIdle()

        assertTrue(vm.poisByCategory.value is UiState.Error)
    }

    // ── Bug 4: createPollWithPoi / createPollWithDestination ──────────────────

    @Test
    fun createPollWithPoiDoesNotNavigateWhenPollCreationFails() = runTest {
        val repo = FakeTravelRepository().also { it.createPollThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithPoi(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
    }

    @Test
    fun createPollWithDestinationDoesNotNavigateWhenPollCreationFails() = runTest {
        val dest = storedDestination("d1", "Bariloche", "Patagonia", "Río Negro")
        val repo = FakeTravelRepository().also { it.createPollThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithDestination(dest) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
    }

    // ── Fix A: onNavigate gated on addPollCandidate success ──────────────────

    @Test
    fun addPoiToPollDoesNotNavigateWhenCandidateInsertFails() = runTest {
        val repo = FakeTravelRepository().also { it.addPollCandidateThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        repo.simulatePollUpdate("group-1", com.hllous.plantravel.domain.model.Poll(
            id = "poll-1", groupId = "group-1",
            type = com.hllous.plantravel.domain.model.PollType.DESTINATION,
            state = com.hllous.plantravel.domain.model.PollState.OPEN,
        ))
        advanceUntilIdle()

        var navigateCalled = false
        vm.addPoiToPoll(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
    }

    @Test
    fun createPollWithPoiDoesNotNavigateWhenCandidateInsertFails() = runTest {
        val repo = FakeTravelRepository().also { it.addPollCandidateThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithPoi(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
    }

    @Test
    fun createPollWithDestinationDoesNotNavigateWhenCandidateInsertFails() = runTest {
        val dest = storedDestination("d1", "Bariloche", "Patagonia", "Río Negro")
        val repo = FakeTravelRepository().also { it.addPollCandidateThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithDestination(dest) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
    }

    // ── Fix B: tripDestination reacts to group switches ───────────────────────

    @Test
    fun tripDestinationUpdatesWhenSelectedGroupChanges() = runTest {
        val g1 = TravelGroup(
            id = "group-1", name = "G1",
            tripDestinationPlaceId = "place-bari", tripDestinationName = "Bariloche",
            tripDestinationLat = -41.0, tripDestinationLng = -71.0,
        )
        val g2 = TravelGroup(
            id = "group-2", name = "G2",
            tripDestinationPlaceId = "place-ushu", tripDestinationName = "Ushuaia",
            tripDestinationLat = -54.8, tripDestinationLng = -68.3,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(g1, g2))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        assertEquals("Bariloche", (vm.tripDestination.value as TripDestinationState.Set).name)

        holder.selectedGroupId.value = "group-2"
        advanceUntilIdle()

        val state = vm.tripDestination.value
        assertTrue(state is TripDestinationState.Set)
        assertEquals("Ushuaia", (state as TripDestinationState.Set).name)
        assertEquals("place-ushu", state.placeId)
    }

    // ── Bug 3: tripDestination shows selected group, not first group ──────────

    @Test
    fun tripDestinationShowsSelectedGroupNotFirstGroup() = runTest {
        val g1 = TravelGroup(
            id = "group-1", name = "G1",
            tripDestinationPlaceId = "place-bari", tripDestinationName = "Bariloche",
            tripDestinationLat = -41.0, tripDestinationLng = -71.0,
        )
        val g2 = TravelGroup(
            id = "group-2", name = "G2",
            tripDestinationPlaceId = "place-ushu", tripDestinationName = "Ushuaia",
            tripDestinationLat = -54.8, tripDestinationLng = -68.3,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(g1, g2))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-2" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        val state = vm.tripDestination.value
        assertTrue(state is TripDestinationState.Set)
        assertEquals("Ushuaia", (state as TripDestinationState.Set).name)
        assertEquals("place-ushu", state.placeId)
    }
}
