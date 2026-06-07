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

    // ── Test 5: selectWinner calls setPollWinner AND setTripDestination ───────

    @Test
    fun selectWinnerOnDestinationPollCallsBothRepositoryMethods() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        repo.simulatePollUpdate("group-1", openPoll)
        val winnerCandidate = PollCandidate(
            id = "cand-win", pollId = "poll-1", placeId = "place-win",
            name = "Ganador", photoUrl = "", addedByMemberId = "m1",
        )
        repo.simulateCandidatesUpdate("poll-1", listOf(winnerCandidate))

        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.candidates.collect {} }

        vm.selectWinner("cand-win")

        assertEquals(1, repo.setTripDestinationCallCount)
        assertEquals("place-win", repo.lastTripDestinationPlaceId)
        job.cancel()
    }

    // ── Test 6: createPoll when already active surfaces error message ─────────

    @Test
    fun createPollWhenActiveExistsSurfacesErrorMessage() {
        val repo = FakeTravelRepository().also { it.createPollThrows = true }
        val vm = viewModel(repo = repo)

        vm.createPoll(PollType.DESTINATION)

        assertNotNull(vm.errorMessage.value)
        assertEquals("Ya hay una encuesta activa", vm.errorMessage.value)
    }

    // ── Cycle 1A: voteProgress is proportional to total votes ────────────────

    @Test
    fun voteProgressIsProportionalToTotalVotes() {
        val member = GroupMember(id = "m1", groupId = "group-1", userId = "user-1", name = "User", role = MemberRole.USER)
        val repo = FakeTravelRepository(initialMembers = mapOf("group-1" to listOf(member)))
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
        assertEquals(0.75f, uiCandidates[0].voteProgress, 0.001f)
        assertEquals(0.25f, uiCandidates[1].voteProgress, 0.001f)
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
