package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeDestinationPhotoResolver
import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.data.destination.DestinationFallbackImage
import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.domain.destination.DestinationPhotoResolver
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
        photoResolver: DestinationPhotoResolver = FakeDestinationPhotoResolver(),
    ) = DestinationViewModel(
        repository = repo,
        placesApiClient = places,
        sessionProvider = session,
        selectedGroupHolder = holder,
        ranker = ranker,
        photoResolver = photoResolver,
    )

    // ── Photo caching / deduplication ────────────────────────────────────────

    @Test
    fun googleFetcherNotCalledForDestinationsWithStoredWikipediaPhoto() = runTest {
        // Destinations that already have a real displayPhotoUrl must not trigger a resolver
        // call — avoids burning API quota on already-resolved destinations.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(
                    "dest-wiki", "El Chaltén", "Patagonia", "Santa Cruz",
                    displayPhotoUrl = "https://upload.wikimedia.org/wikipedia/commons/thumb/chalten.jpg",
                ),
            ),
        )
        val resolver = FakeDestinationPhotoResolver { "https://google.example/chalten.jpg" }
        val vm = viewModel(repo = repo, photoResolver = resolver)

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(0, resolver.resolveCallCount)
        assertEquals(
            "https://upload.wikimedia.org/wikipedia/commons/thumb/chalten.jpg",
            vm.destinationPhotoUrls.value["dest-wiki"],
        )
    }

    @Test
    fun photoFetcherDoesNotStartDuplicateRequestForInFlightDestination() = runTest {
        // If loadPhotosFor is called twice before the first fetch completes, the resolver
        // must only be called once (photoLoadingDestinationKeys deduplication).
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("dest-1", "Ushuaia", "Patagonia", "Tierra del Fuego"),
            ),
        )
        val resolver = FakeDestinationPhotoResolver { "https://example.com/ushuaia.jpg" }
        val vm = viewModel(repo = repo, photoResolver = resolver)

        // Two rapid loads before coroutines settle
        vm.selectRegion("Patagonia")
        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(1, resolver.resolveCallCount)
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
        val vm = viewModel(
            repo = repo,
            photoResolver = FakeDestinationPhotoResolver { "https://example.com/${it.name}.jpg" },
        )

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
        val vm = viewModel(
            repo = repo,
            photoResolver = FakeDestinationPhotoResolver { dest -> "https://wikipedia.example/${dest.name}.jpg" },
        )

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        assertEquals(
            "https://wikipedia.example/El Chaltén.jpg",
            vm.destinationPhotoUrls.value["dest-1"],
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
        val dest = storedDestination("dest-1", "El Chaltén", "Patagonia", "Santa Cruz")
        val resolver = FakeDestinationPhotoResolver { d ->
            attempts++
            if (attempts == 1) DestinationFallbackImage.tokenFor(d) else "https://example.com/el-chalten.jpg"
        }
        val vm = viewModel(repo = repo, photoResolver = resolver)

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
        val vm = viewModel(
            repo = repo,
            photoResolver = FakeDestinationPhotoResolver { d -> DestinationFallbackImage.tokenFor(d) },
        )

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

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
        assertEquals(listOf("lodging"), places.lastSearchedTypes)

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

        assertNull(places.lastSearchedTypes)
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

    @Test
    fun loadHomeFeedStartsPoiRequestsInParallel() = runTest {
        val group = TravelGroup(
            id = "group-1", name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1335,
            tripDestinationLng = -71.3103,
        )
        var searchCallCount = 0
        var inFlight = 0
        var maxInFlight = 0
        val releaseSearches = Channel<Unit>(capacity = 4)
        val places = FakePlacesApiClient(
            poiResults = listOf(fakePlace),
            beforeSearchPois = { _ ->
                searchCallCount++
                inFlight++
                maxInFlight = maxOf(maxInFlight, inFlight)
                releaseSearches.receive()
            },
            afterSearchPois = {
                inFlight--
            },
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()
        assertTrue(vm.tripDestination.value is TripDestinationState.Set)

        vm.loadHomeFeed()
        advanceUntilIdle()

        assertEquals(4, searchCallCount)
        assertTrue(maxInFlight > 1)

        repeat(4) { releaseSearches.trySend(Unit) }
        advanceUntilIdle()

        assertTrue(vm.homeFeed.value is UiState.Success)
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
    fun createPollWithPoiNavigatesEvenWhenCandidateInsertFails() = runTest {
        // Navigation happens as soon as the poll is created; candidate insertion is best-effort.
        val repo = FakeTravelRepository().also { it.addPollCandidateThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithPoi(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertTrue(navigateCalled)
    }

    @Test
    fun createPollWithDestinationNavigatesEvenWhenCandidateInsertFails() = runTest {
        // Navigation happens as soon as the poll is created; candidate insertion is best-effort.
        val dest = storedDestination("d1", "Bariloche", "Patagonia", "Río Negro")
        val repo = FakeTravelRepository().also { it.addPollCandidateThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithDestination(dest) { navigateCalled = true }
        advanceUntilIdle()

        assertTrue(navigateCalled)
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

    @Test
    fun setTripDestinationRefreshesStateWhenLocalWriteNeedsResubscribe() = runTest {
        val before = TravelGroup(id = "group-1", name = "Viaje")
        val after = TravelGroup(
            id = "group-1",
            name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Ushuaia",
            tripDestinationLat = -54.8019,
            tripDestinationLng = -68.3030,
        )
        var subscriptions = 0
        val repo = FakeTravelRepository(
            initialGroups = listOf(before),
            customObserveGroups = {
                flow {
                    subscriptions++
                    emit(if (subscriptions >= 2) listOf(after) else listOf(before))
                }
            },
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        vm.setTripDestination(storedDestination("d1", "Ushuaia", "Patagonia", "Tierra del Fuego"))
        advanceUntilIdle()

        val state = vm.tripDestination.value
        assertTrue(state is TripDestinationState.Set)
        assertEquals("Ushuaia", (state as TripDestinationState.Set).name)
    }

    @Test
    fun createPollWithPoiRefreshesActivePollWhenLocalWriteNeedsResubscribe() = runTest {
        val createdPoll = com.hllous.plantravel.domain.model.Poll(
            id = "poll-1",
            groupId = "group-1",
            type = com.hllous.plantravel.domain.model.PollType.ACTIVITY,
            state = com.hllous.plantravel.domain.model.PollState.OPEN,
        )
        var subscriptions = 0
        val repo = FakeTravelRepository(
            customObserveActivePoll = {
                flow {
                    subscriptions++
                    emit(if (subscriptions >= 2) createdPoll else null)
                }
            },
            customObserveActiveActivityPolls = { flowOf(emptyList()) },
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        var navigateCalled = false
        vm.createPollWithPoi(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertTrue(navigateCalled)
        assertEquals(createdPoll.id, vm.activePoll.value?.id)
    }

    @Test
    fun reloadGroupsRefreshesActivePollWhenPollWasCreatedOffScreen() = runTest {
        val createdPoll = com.hllous.plantravel.domain.model.Poll(
            id = "poll-1",
            groupId = "group-1",
            type = com.hllous.plantravel.domain.model.PollType.DESTINATION,
            state = com.hllous.plantravel.domain.model.PollState.OPEN,
        )
        var subscriptions = 0
        val repo = FakeTravelRepository(
            customObserveActivePoll = {
                flow {
                    subscriptions++
                    emit(if (subscriptions >= 2) createdPoll else null)
                }
            },
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        assertNull(vm.activePoll.value)

        vm.reloadGroups()
        advanceUntilIdle()

        assertEquals(createdPoll.id, vm.activePoll.value?.id)
    }

    @Test
    fun addDestinationToPollUsesRepositoryFallbackWhenLocalActivePollStateIsStale() = runTest {
        val createdPoll = com.hllous.plantravel.domain.model.Poll(
            id = "poll-1",
            groupId = "group-1",
            type = com.hllous.plantravel.domain.model.PollType.DESTINATION,
            state = com.hllous.plantravel.domain.model.PollState.OPEN,
        )
        // observeActivePoll always returns null (stale local state); fetchActivePoll returns the real poll (fresh DB fetch)
        val repo = FakeTravelRepository(
            customObserveActivePoll = { flowOf(null) },
            customFetchActivePoll = { createdPoll },
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        advanceUntilIdle()

        val destination = storedDestination("d1", "Bariloche", "Patagonia", "Río Negro")
        vm.addDestinationToPoll(destination)
        advanceUntilIdle()

        assertEquals(1, repo.addPollCandidateCallCount)
        assertEquals(createdPoll.id, repo.lastAddedPollCandidatePollId)
    }

    @Test
    fun addDestinationToPollDoesNothingWhenOnlyActivityPollIsActive() = runTest {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        repo.simulatePollUpdate(
            "group-1",
            com.hllous.plantravel.domain.model.Poll(
                id = "poll-1",
                groupId = "group-1",
                type = com.hllous.plantravel.domain.model.PollType.ACTIVITY,
                state = com.hllous.plantravel.domain.model.PollState.OPEN,
            ),
        )
        advanceUntilIdle()

        vm.addDestinationToPoll(storedDestination("d1", "Bariloche", "Patagonia", "Río Negro"))
        advanceUntilIdle()

        assertEquals(0, repo.addPollCandidateCallCount)
    }

    @Test
    fun addPoiToPollDoesNotNavigateWhenOnlyDestinationPollIsActive() = runTest {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        backgroundScope.launch { vm.activePoll.collect {} }
        repo.simulatePollUpdate(
            "group-1",
            com.hllous.plantravel.domain.model.Poll(
                id = "poll-1",
                groupId = "group-1",
                type = com.hllous.plantravel.domain.model.PollType.DESTINATION,
                state = com.hllous.plantravel.domain.model.PollState.OPEN,
            ),
        )
        advanceUntilIdle()

        var navigateCalled = false
        vm.addPoiToPoll(fakePlace) { navigateCalled = true }
        advanceUntilIdle()

        assertFalse(navigateCalled)
        assertEquals(0, repo.addPollCandidateCallCount)
    }

    // ── Curated destinations ──────────────────────────────────────────────────

    @Test
    fun selectRegion_prependsCuratedEntries() = runTest {
        // Esquel has higher population than Bariloche in DB → default sort puts Esquel first.
        // Curated list has Bariloche first → merged result must put Bariloche first.
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("esq", "Esquel", "Patagonia", "Chubut", population = 45000),
                storedDestination("bar", "Bariloche", "Patagonia", "Río Negro", population = 135000),
                storedDestination("bol", "El Bolsón", "Patagonia", "Río Negro", population = 20000),
            ),
        )
        val vm = viewModel(repo = repo, photoResolver = FakeDestinationPhotoResolver())

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        val names = vm.regionDestinations.value.map { it.name }
        // Bariloche and El Bolsón are in CuratedDestinations for Patagonia → must appear first in that order
        assertEquals("Bariloche", names[0])
        assertEquals("El Bolsón", names[1])
    }

    @Test
    fun deduplication_curatedNameAppearsOnlyOnce() = runTest {
        val repo = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination("bar", "Bariloche", "Patagonia", "Río Negro", population = 135000),
                storedDestination("ushu", "Ushuaia", "Patagonia", "Tierra del Fuego", population = 82615),
            ),
        )
        val vm = viewModel(repo = repo, photoResolver = FakeDestinationPhotoResolver())

        vm.selectRegion("Patagonia")
        advanceUntilIdle()

        val ids = vm.regionDestinations.value.map { it.id }
        assertEquals(ids.distinct(), ids)
        assertEquals(1, ids.count { it == "bar" })
    }

    // ── Chip expansion ────────────────────────────────────────────────────────

    @Test
    fun selectTurismoChip_callsSearchPoisWithTurismoTypes() = runTest {
        val group = TravelGroup(
            id = "group-1", name = "Viaje",
            tripDestinationPlaceId = "dest-1", tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1, tripDestinationLng = -71.3,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val places = FakePlacesApiClient()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        vm.selectPoiCategory("Turismo")
        advanceUntilIdle()

        assertEquals(listOf("tourist_attraction", "amusement_park", "zoo"), places.lastSearchedTypes)
    }

    // ── Level 2 search bar ────────────────────────────────────────────────────

    @Test
    fun searchNearby_callsSearchNearbyWithCoordinates() = runTest {
        val group = TravelGroup(
            id = "group-1", name = "Viaje",
            tripDestinationPlaceId = "dest-1", tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1, tripDestinationLng = -71.3,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val places = FakePlacesApiClient()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, places = places, holder = holder)
        backgroundScope.launch { vm.tripDestination.collect {} }
        advanceUntilIdle()

        vm.searchNearby("museo")
        advanceUntilIdle()

        assertEquals("museo", places.lastNearbyQuery)
        assertEquals(-41.1, places.lastNearbyLat!!, 0.001)
        assertEquals(-71.3, places.lastNearbyLng!!, 0.001)
    }
}
