package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
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
        address = "Bariloche, Río Negro",
        lat = -41.1335,
        lng = -71.3103,
    )

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        places: FakePlacesApiClient = FakePlacesApiClient(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder(),
    ) = DestinationViewModel(
        repository = repo,
        placesApiClient = places,
        sessionProvider = session,
        selectedGroupHolder = holder,
    )

    // ── Test 1: selectRegion maps chip name to query and emits results ──────

    @Test
    fun selectRegionTriggersCorrectQueryAndEmitsResults() {
        val places = FakePlacesApiClient(destinationResults = listOf(fakePlace))
        val vm = viewModel(places = places)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.regionResults.collect {} }

        vm.selectRegion("Patagonia")

        assertEquals("turismo Patagonia Argentina", places.lastSearchedRegion)
        val state = vm.regionResults.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(fakePlace), (state as UiState.Success).data)
        job.cancel()
    }

    // ── Test 2: search emits results into searchResults ──────────────────────

    @Test
    fun searchQueryEmitsResultsIntoSearchResults() {
        val places = FakePlacesApiClient(destinationResults = listOf(fakePlace))
        val vm = viewModel(places = places)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.searchResults.collect {} }

        vm.search("Mendoza")

        assertEquals("Mendoza", places.lastSearchedRegion)
        val state = vm.searchResults.value
        assertTrue(state is UiState.Success)
        assertEquals(listOf(fakePlace), (state as UiState.Success).data)
        job.cancel()
    }

    // ── Test 3: setTripDestination persists and updates tripDestination ───────

    @Test
    fun setTripDestinationCallsRepositoryAndUpdatesTripDestinationState() {
        val group = TravelGroup(id = "group-1", name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.tripDestination.collect {} }

        vm.setTripDestination(fakePlace)

        assertEquals(1, repo.setTripDestinationCallCount)
        assertEquals(fakePlace.placeId, repo.lastTripDestinationPlaceId)
        val state = vm.tripDestination.value
        assertTrue(state is TripDestinationState.Set)
        state as TripDestinationState.Set
        assertEquals(fakePlace.placeId, state.placeId)
        assertEquals(fakePlace.name, state.name)
        assertEquals(fakePlace.lat, state.lat, 0.0001)
        assertEquals(fakePlace.lng, state.lng, 0.0001)
        job.cancel()
    }

    // ── Test 4: existing destination in repository propagates to state ────────

    @Test
    fun tripDestinationObservedFromRepositoryPropagatesToState() {
        val group = TravelGroup(
            id = "group-1",
            name = "Viaje",
            tripDestinationPlaceId = "place-99",
            tripDestinationName = "Ushuaia",
            tripDestinationLat = -54.8019,
            tripDestinationLng = -68.3030,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val vm = viewModel(repo = repo)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.tripDestination.collect {} }

        val state = vm.tripDestination.value

        assertTrue(state is TripDestinationState.Set)
        state as TripDestinationState.Set
        assertEquals("place-99", state.placeId)
        assertEquals("Ushuaia", state.name)
        job.cancel()
    }

    // ── Test 5: API error emits UiState.Error into regionResults ─────────────

    @Test
    fun apiErrorEmitsUiStateErrorIntoRegionResults() {
        val places = FakePlacesApiClient(searchDestinationsThrows = true)
        val vm = viewModel(places = places)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.regionResults.collect {} }

        vm.selectRegion("Cuyo")

        val state = vm.regionResults.value
        assertTrue(state is UiState.Error)
        job.cancel()
    }
}
