package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import com.hllous.plantravel.presentation.itinerary.ItineraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ItineraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fakePlace = PlaceResult(
        placeId = "place-1",
        name = "Hotel Las Rocas",
        photoUrl = "https://example.com/photo.jpg",
        rating = 4.5,
        reviewCount = 100,
        address = "Av. Principal 123, Bariloche",
        lat = -41.1335,
        lng = -71.3103,
    )

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        places: FakePlacesApiClient = FakePlacesApiClient(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" },
    ) = ItineraryViewModel(
        repository = repo,
        placesApiClient = places,
        selectedGroupHolder = holder,
    )

    // ── Test 1: createEvent persists and appears grouped by date ─────────────

    @Test
    fun createEventPersistsAndAppearsGroupedByDate() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.events.collect {} }

        vm.createEvent(name = "Visitar centro", date = "2025-06-12")

        val state = vm.events.value
        assertTrue(state is UiState.Success)
        val days = (state as UiState.Success).data
        assertEquals(1, days.size)
        assertEquals("2025-06-12", days[0].date)
        assertEquals("Visitar centro", days[0].events[0].name)
        job.cancel()
    }

    // ── Test 2: Realtime push propagates into events ─────────────────────────

    @Test
    fun realtimePushPropagatesIntoEventsState() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.events.collect {} }

        val pushed = listOf(
            ItineraryEvent(id = "e1", groupId = "group-1", name = "Cena en La Fonda", date = "2025-06-13", createdByMemberId = "m1")
        )
        repo.simulateItineraryEventPush("group-1", pushed)

        val state = vm.events.value
        assertTrue(state is UiState.Success)
        val days = (state as UiState.Success).data
        assertEquals(1, days.size)
        assertEquals("Cena en La Fonda", days[0].events[0].name)
        job.cancel()
    }

    // ── Test 3: Events grouped by date and sorted chronologically ────────────

    @Test
    fun eventsGroupedByDateSortedChronologically() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.events.collect {} }

        repo.simulateItineraryEventPush("group-1", listOf(
            ItineraryEvent(id = "e3", groupId = "group-1", name = "Noche", date = "2025-06-14", timeOfDay = "20:00", createdByMemberId = "m1"),
            ItineraryEvent(id = "e1", groupId = "group-1", name = "Mañana", date = "2025-06-12", timeOfDay = "09:00", createdByMemberId = "m1"),
            ItineraryEvent(id = "e2", groupId = "group-1", name = "Tarde", date = "2025-06-12", timeOfDay = "15:00", createdByMemberId = "m1"),
        ))

        val days = (vm.events.value as UiState.Success).data
        assertEquals(2, days.size)
        assertEquals("2025-06-12", days[0].date)
        assertEquals("2025-06-14", days[1].date)
        assertEquals("Mañana", days[0].events[0].name)
        assertEquals("Tarde", days[0].events[1].name)
        job.cancel()
    }

    // ── Test 4: buildEventFromPoi maps fields correctly ──────────────────────

    @Test
    fun buildEventFromPoiMapsFieldsCorrectly() {
        val vm = viewModel()

        val draft = vm.buildEventFromPoi(fakePlace)

        assertEquals(fakePlace.name, draft.name)
        assertEquals(fakePlace.address, draft.description)
        assertEquals(fakePlace.placeId, draft.placeId)
        assertNull(draft.date)
    }

    // ── Test 5: deleteEvent removes event from state ─────────────────────────

    @Test
    fun deleteEventRemovesEventFromState() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.events.collect {} }

        vm.createEvent(name = "Evento a borrar", date = "2025-06-15")
        val eventId = (vm.events.value as UiState.Success).data[0].events[0].id

        vm.deleteEvent(eventId)

        val remaining = (vm.events.value as UiState.Success).data
        assertTrue(remaining.isEmpty())
        job.cancel()
    }

    // ── Test 6: createEvent from poll candidate draft persists name + placeId ──

    @Test
    fun createEventFromPollCandidateDraftPersistsNameAndPlaceId() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.events.collect {} }

        vm.createEvent(name = "Teatro Colón", date = "2025-07-01", placeId = "place-teatro-colon")

        val state = vm.events.value
        assertTrue(state is UiState.Success)
        val event = (state as UiState.Success).data[0].events[0]
        assertEquals("Teatro Colón", event.name)
        assertEquals("place-teatro-colon", event.placeId)
        job.cancel()
    }
}
