package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeDestinationPhotoResolver
import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.places.PlaceRecommendationRanker
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class DestinationViewModelRealtimeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository,
        holder: SelectedGroupHolder = SelectedGroupHolder(),
    ) = DestinationViewModel(
        repository = repo,
        placesApiClient = FakePlacesApiClient(),
        sessionProvider = FakeSessionProvider(userId = "user-1"),
        selectedGroupHolder = holder,
        ranker = PlaceRecommendationRanker(),
        photoResolver = FakeDestinationPhotoResolver(),
    )

    private fun warmUp(block: suspend () -> Unit): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch { block() }
        return scope
    }

    // ─── Remote destination push ─────────────────────────────────────────────────

    @Test
    fun tripDestinationUpdatesWhenRemoteGroupsPushed() {
        val groupId = "group-1"
        val group = TravelGroup(id = groupId, name = "Viaje")
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().apply { selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.tripDestination.collect { } }

        assertEquals(TripDestinationState.None, vm.tripDestination.value)

        repo.simulateRemoteGroupsPush(listOf(
            group.copy(
                tripDestinationPlaceId = "place-bari",
                tripDestinationName = "Bariloche",
                tripDestinationLat = -41.1335,
                tripDestinationLng = -71.3103,
            )
        ))

        val state = vm.tripDestination.value
        assertTrue("Expected TripDestinationState.Set but got $state", state is TripDestinationState.Set)
        assertEquals("Bariloche", (state as TripDestinationState.Set).name)

        scope.cancel()
    }

    @Test
    fun tripDestinationClearsWhenRemotePushRemovesDestination() {
        val groupId = "group-1"
        val group = TravelGroup(
            id = groupId,
            name = "Viaje",
            tripDestinationPlaceId = "place-bari",
            tripDestinationName = "Bariloche",
            tripDestinationLat = -41.1335,
            tripDestinationLng = -71.3103,
        )
        val repo = FakeTravelRepository(initialGroups = listOf(group))
        val holder = SelectedGroupHolder().apply { selectedGroupId.value = groupId }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.tripDestination.collect { } }

        assertTrue(vm.tripDestination.value is TripDestinationState.Set)

        repo.simulateRemoteGroupsPush(listOf(group.copy(
            tripDestinationPlaceId = null,
            tripDestinationName = null,
            tripDestinationLat = null,
            tripDestinationLng = null,
        )))

        assertEquals(TripDestinationState.None, vm.tripDestination.value)

        scope.cancel()
    }
}
