package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import com.hllous.plantravel.presentation.poll.PollViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PollViewModelTest {

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

    private val openPoll = Poll(
        id = "poll-1",
        groupId = "group-1",
        type = PollType.DESTINATION,
        state = PollState.OPEN,
    )

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" },
    ) = PollViewModel(
        repository = repo,
        sessionProvider = session,
        selectedGroupHolder = holder,
    )

    // ── Test 1: addCandidate persists and appears in candidates state ─────────

    @Test
    fun addCandidatePersistsAndAppearsInCandidatesState() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        vm.addCandidate(fakePlace)

        val state = vm.candidates.value
        assertTrue(state is UiState.Success)
        val uiCandidates = (state as UiState.Success).data
        assertEquals(1, uiCandidates.size)
        assertEquals(fakePlace.placeId, uiCandidates[0].candidate.placeId)
        job.cancel()
    }

    // ── Test 2: toggleVote adds a vote when not yet voted ─────────────────────

    @Test
    fun toggleVoteAddsThumpsUpWhenNotYetVoted() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val candidate = PollCandidate(
            id = "cand-1", pollId = "poll-1", placeId = "place-1",
            name = "Hotel", photoUrl = "", addedByMemberId = "m1",
            voteCount = 0, votedByCurrentMember = false,
        )
        repo.simulateCandidatesUpdate("poll-1", listOf(candidate))

        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job1 = scope.launch { vm.currentMember.collect {} }
        val job2 = scope.launch { vm.candidates.collect {} }

        vm.toggleVote("cand-1")

        val uiCandidates = (vm.candidates.value as UiState.Success).data
        assertEquals(1, uiCandidates[0].voteCount)
        assertTrue(uiCandidates[0].votedByCurrentMember)
        job1.cancel()
        job2.cancel()
    }

    // ── Test 3: toggleVote removes vote when already voted ────────────────────

    @Test
    fun toggleVoteRemovesVoteWhenAlreadyVoted() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val candidate = PollCandidate(
            id = "cand-1", pollId = "poll-1", placeId = "place-1",
            name = "Hotel", photoUrl = "", addedByMemberId = "m1",
            voteCount = 1, votedByCurrentMember = true,
        )
        repo.simulateCandidatesUpdate("poll-1", listOf(candidate))

        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job1 = scope.launch { vm.currentMember.collect {} }
        val job2 = scope.launch { vm.candidates.collect {} }

        vm.toggleVote("cand-1")

        val uiCandidates = (vm.candidates.value as UiState.Success).data
        assertEquals(0, uiCandidates[0].voteCount)
        assertFalse(uiCandidates[0].votedByCurrentMember)
        job1.cancel()
        job2.cancel()
    }

    @Test
    fun toggleVoteRefreshesCandidatesWhenLocalWriteNeedsResubscribe() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        var subscriptions = 0
        val initial = PollCandidate(
            id = "cand-1", pollId = "poll-1", placeId = "place-1",
            name = "Hotel", photoUrl = "", addedByMemberId = "m1",
            voteCount = 0, votedByCurrentMember = false,
        )
        val updated = initial.copy(voteCount = 1, votedByCurrentMember = true)
        val repo = FakeTravelRepository(
            initialMembers = mapOf("group-1" to listOf(member)),
            customObservePollCandidates = {
                flow {
                    subscriptions++
                    emit(if (subscriptions >= 2) listOf(updated) else listOf(initial))
                }
            }
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)

        val vm = viewModel(repo = repo, holder = holder)
        val scope = CoroutineScope(UnconfinedTestDispatcher())
        val job1 = scope.launch { vm.currentMember.collect {} }
        val job2 = scope.launch { vm.candidates.collect {} }

        vm.toggleVote("cand-1")

        val uiCandidates = (vm.candidates.value as UiState.Success).data
        assertEquals(1, uiCandidates[0].voteCount)
        assertTrue(uiCandidates[0].votedByCurrentMember)
        job1.cancel()
        job2.cancel()
    }

    // ── Test 4: closePoll transitions poll state to CLOSED ────────────────────

    @Test
    fun closePollTransitionsPollStateToClosed() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        vm.closePoll()

        assertEquals(PollState.CLOSED, vm.poll.value?.state)
        job.cancel()
    }

    // ── Test 5: selectWinner calls setPollWinner only; setTripDestination is a separate step ──

    @Test
    fun selectWinnerCallsSetPollWinnerAndDoesNotAutoSetTripDestination() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val winnerCandidate = PollCandidate(
            id = "cand-win", pollId = "poll-1", placeId = "place-win",
            name = "Ganador", photoUrl = "http://photo.com/img.jpg", addedByMemberId = "m1",
        )
        repo.simulateCandidatesUpdate("poll-1", listOf(winnerCandidate))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        vm.selectWinner("cand-win")

        assertEquals(0, repo.setTripDestinationCallCount)
        val updatedPoll = repo.getPollsForGroup("group-1").firstOrNull { it.id == "poll-1" }
        assertEquals("place-win", updatedPoll?.winnerPlaceId)
        assertEquals("http://photo.com/img.jpg", updatedPoll?.winnerPhotoUrl)
        job.cancel()
    }

    @Test
    fun setWinnerAsDestinationCallsSetTripDestination() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        var result: Boolean? = null

        vm.setWinnerAsDestination(
            placeId = "place-win",
            name = "Bariloche",
            lat = -41.1,
            lng = -71.3,
            onResult = { result = it },
        )

        assertEquals(1, repo.setTripDestinationCallCount)
        assertEquals("place-win", repo.lastTripDestinationPlaceId)
        assertEquals(true, result)
    }

    @Test
    fun setWinnerAsDestinationReportsFailureThroughCallback() {
        val repo = FakeTravelRepository().also { it.setTripDestinationThrows = true }
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        var result: Boolean? = null

        vm.setWinnerAsDestination(
            placeId = "place-win",
            name = "Bariloche",
            lat = -41.1,
            lng = -71.3,
            onResult = { result = it },
        )

        assertEquals(false, result)
        assertEquals("Error al seleccionar destino", vm.errorMessage.value)
    }

    // ── Test 6: createPoll when already active surfaces error message ─────────

    @Test
    fun createPollWhenActiveExistsSurfacesErrorMessage() {
        val repo = FakeTravelRepository().also { it.createPollThrows = true }
        val vm = viewModel(repo = repo)

        vm.createPoll(PollType.DESTINATION, "¿A dónde vamos?")

        assertNotNull(vm.errorMessage.value)
        assertEquals("Ya hay una encuesta activa", vm.errorMessage.value)
    }

    // ── Cycle 1A: voteProgress uses member count as denominator ─────────────

    @Test
    fun voteProgressIsProportionalToMemberCount() {
        val members = (1..4).map { i ->
            GroupMember(id = "m$i", groupId = "group-1", userId = "user-$i", name = "User$i", role = MemberRole.USER)
        }
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to members))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val candidates = listOf(
            PollCandidate(id = "c1", pollId = "poll-1", placeId = "p1", name = "A", photoUrl = "", addedByMemberId = "m1", voteCount = 3, votedByCurrentMember = false),
            PollCandidate(id = "c2", pollId = "poll-1", placeId = "p2", name = "B", photoUrl = "", addedByMemberId = "m1", voteCount = 1, votedByCurrentMember = false),
        )
        repo.simulateCandidatesUpdate("poll-1", candidates)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        val uiCandidates = (vm.candidates.value as UiState.Success).data
        // 4 members: candidate A has 3 votes → 3/4 = 0.75; candidate B has 1 vote → 1/4 = 0.25
        assertEquals(0.75f, uiCandidates[0].voteProgress, 0.001f)
        assertEquals(0.25f, uiCandidates[1].voteProgress, 0.001f)
        job.cancel()
    }

    // ── New behaviors: multiple polls, naming, rename, isTied ────────────────

    @Test
    fun activeActivityPollsEmitsOnlyOpenActivityPolls() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val destPoll    = Poll(id = "dest-1", groupId = "group-1", type = PollType.DESTINATION, state = PollState.OPEN,   name = "¿A dónde vamos?")
        val actOpen     = Poll(id = "act-1",  groupId = "group-1", type = PollType.ACTIVITY,    state = PollState.OPEN,   name = "¿Qué hacemos?")
        val actClosed   = Poll(id = "act-2",  groupId = "group-1", type = PollType.ACTIVITY,    state = PollState.CLOSED, name = "Ya votamos")
        repo.simulatePollsUpdate("group-1", listOf(destPoll, actOpen, actClosed))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.activeActivityPolls.collect {} }

        val result = vm.activeActivityPolls.value
        assertEquals(1, result.size)
        assertEquals("act-1", result[0].id)
        assertEquals(PollType.ACTIVITY, result[0].type)
        assertEquals(PollState.OPEN, result[0].state)
        job.cancel()
    }

    @Test
    fun closedActivityPollsEmitsOnlyClosedActivityPolls() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val destPoll  = Poll(id = "dest-1", groupId = "group-1", type = PollType.DESTINATION, state = PollState.OPEN,   name = "¿A dónde vamos?")
        val actOpen   = Poll(id = "act-1",  groupId = "group-1", type = PollType.ACTIVITY,    state = PollState.OPEN,   name = "Activa")
        val actClosed = Poll(id = "act-2",  groupId = "group-1", type = PollType.ACTIVITY,    state = PollState.CLOSED, name = "Cerrada")
        repo.simulatePollsUpdate("group-1", listOf(destPoll, actOpen, actClosed))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.closedActivityPolls.collect {} }

        val result = vm.closedActivityPolls.value
        assertEquals(1, result.size)
        assertEquals("act-2", result[0].id)
        assertEquals(PollState.CLOSED, result[0].state)
        job.cancel()
    }

    @Test
    fun activePollIgnoresActivityPolls() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val actPoll = Poll(id = "act-1", groupId = "group-1", type = PollType.ACTIVITY, state = PollState.OPEN, name = "Actividad")
        repo.simulatePollsUpdate("group-1", listOf(actPoll))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.poll.collect {} }

        assertNull(vm.poll.value)
        job.cancel()
    }

    @Test
    fun createPollStoresNameInRepository() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.poll.collect {} }

        vm.createPoll(PollType.DESTINATION, "¿A dónde vamos?")

        assertEquals("¿A dónde vamos?", vm.poll.value?.name)
        job.cancel()
    }

    @Test
    fun renamePollCallsRepositoryWithCorrectArguments() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val vm = viewModel(repo = repo, holder = holder)

        vm.renamePoll("poll-1", "Nuevo nombre")

        assertEquals(1, repo.renamePollCallCount)
        assertEquals("poll-1", repo.lastRenamedPollId)
        assertEquals("Nuevo nombre", repo.lastRenamedPollName)
    }

    @Test
    fun selectWinnerOnActivityPollDoesNotCallSetTripDestination() {
        val activityPoll = Poll(id = "act-1", groupId = "group-1", type = PollType.ACTIVITY, state = PollState.CLOSED, name = "Actividades")
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollsUpdate("group-1", listOf(activityPoll))
        val winner = PollCandidate(id = "cand-win", pollId = "act-1", placeId = "place-win", name = "Kayak", photoUrl = "", addedByMemberId = "m1")
        repo.simulateCandidatesUpdate("act-1", listOf(winner))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }
        vm.setScreenPoll(activityPoll)

        vm.selectWinner("cand-win")

        assertEquals(0, repo.setTripDestinationCallCount)
        job.cancel()
    }

    @Test
    fun isTiedIsTrueWhenTopCandidatesShareMaxVotes() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val tiedCandidates = listOf(
            PollCandidate(id = "c1", pollId = "poll-1", placeId = "p1", name = "A", photoUrl = "", addedByMemberId = "m1", voteCount = 3),
            PollCandidate(id = "c2", pollId = "poll-1", placeId = "p2", name = "B", photoUrl = "", addedByMemberId = "m1", voteCount = 3),
            PollCandidate(id = "c3", pollId = "poll-1", placeId = "p3", name = "C", photoUrl = "", addedByMemberId = "m1", voteCount = 1),
        )
        repo.simulateCandidatesUpdate("poll-1", tiedCandidates)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch {
            vm.candidates.collect {}
            vm.isTied.collect {}
        }

        assertTrue(vm.isTied.value)
        job.cancel()
    }

    @Test
    fun isTiedIsFalseWhenThereIsAClearLeader() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val candidates = listOf(
            PollCandidate(id = "c1", pollId = "poll-1", placeId = "p1", name = "A", photoUrl = "", addedByMemberId = "m1", voteCount = 5),
            PollCandidate(id = "c2", pollId = "poll-1", placeId = "p2", name = "B", photoUrl = "", addedByMemberId = "m1", voteCount = 2),
        )
        repo.simulateCandidatesUpdate("poll-1", candidates)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch {
            vm.candidates.collect {}
            vm.isTied.collect {}
        }

        assertFalse(vm.isTied.value)
        job.cancel()
    }

    // ── Cycle 1B: voteProgress is 0 for all when total votes is zero ─────────

    @Test
    fun voteProgressIsZeroWhenNoVotesCast() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val candidates = listOf(
            PollCandidate(id = "c1", pollId = "poll-1", placeId = "p1", name = "A", photoUrl = "", addedByMemberId = "m1", voteCount = 0, votedByCurrentMember = false),
            PollCandidate(id = "c2", pollId = "poll-1", placeId = "p2", name = "B", photoUrl = "", addedByMemberId = "m1", voteCount = 0, votedByCurrentMember = false),
        )
        repo.simulateCandidatesUpdate("poll-1", candidates)

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        val uiCandidates = (vm.candidates.value as UiState.Success).data
        assertEquals(0f, uiCandidates[0].voteProgress, 0.001f)
        assertEquals(0f, uiCandidates[1].voteProgress, 0.001f)
        job.cancel()
    }
}
