package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import com.hllous.plantravel.presentation.itinerary.ItineraryViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ItineraryViewModelRealtimeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository,
        holder: SelectedGroupHolder,
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
    ) = ItineraryViewModel(
        repository = repo,
        sessionProvider = session,
        selectedGroupHolder = holder,
    )

    private fun warmUp(block: suspend () -> Unit): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch { block() }
        return scope
    }

    @Test
    fun eventsStillUpdatedFromRemotePushAfterLocalCreate() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.events.collect { } }

        vm.createEvent(name = "Evento local", date = "2025-06-12")

        val remoteEvents = listOf(
            ItineraryEvent(
                id = "remote-1",
                groupId = "group-1",
                name = "Excursion remota",
                date = "2025-06-13",
                createdByMemberId = "member-2",
            )
        )
        repo.simulateItineraryEventPush("group-1", remoteEvents)

        val state = vm.events.value as UiState.Success
        assertEquals("Excursion remota", state.data.single().events.single().name)
        scope.cancel()
    }
}
