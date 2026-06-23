package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.expense.ExpenseDashboardService
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CreateExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.FinalizeExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.SetExpenseGroupPinnedUseCase
import com.hllous.plantravel.domain.usecase.UpdateExpenseGroupNameUseCase
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Verifies that the ViewModel's observable flows continue receiving remote
 * updates (simulating Supabase Realtime events) after any local write.
 *
 * The risk being locked down: the retry-trigger pattern (reloadExpenseItems /
 * reloadAssignments) uses flatMapLatest, which tears down and rebuilds the
 * repository subscription on every local write. A missed Realtime event
 * during that teardown window would leave the UI stale without any error.
 */
class ExpenseViewModelRealtimeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository,
        holder: SelectedGroupHolder,
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
    ) = ExpenseViewModel(
        repository = repo,
        sessionProvider = session,
        selectedGroupHolder = holder,
        dashboardService = ExpenseDashboardService(repo),
        addExpenseItemUseCase = AddExpenseItemUseCase(repo),
        assignItemToMemberUseCase = AssignItemToMemberUseCase(repo),
        deleteExpenseItemUseCase = DeleteExpenseItemUseCase(repo),
        createExpenseGroupUseCase = CreateExpenseGroupUseCase(repo),
        updateExpenseGroupNameUseCase = UpdateExpenseGroupNameUseCase(repo),
        deleteExpenseGroupUseCase = DeleteExpenseGroupUseCase(repo),
        finalizeExpenseGroupUseCase = FinalizeExpenseGroupUseCase(repo),
        setExpenseGroupPinnedUseCase = SetExpenseGroupPinnedUseCase(repo),
    )

    // Starts a collector on the given suspend block and returns a scope to cancel it.
    // SharingStarted.WhileSubscribed only activates the upstream once there is
    // at least one active subscriber — this helper provides that subscriber.
    private fun warmUp(block: suspend () -> Unit): CoroutineScope {
        val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
        scope.launch { block() }
        return scope
    }

    // ── Slice 1: remote expense push propagates ───────────────────────────────

    @Test
    fun expenseItemsReflectsRemoteRealtimePush() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val scope = warmUp { vm.expenseItems.collect { } }

        val remoteItem = ExpenseItem("remote-1", "tg-1", "eg-1", "Taxi", 5000L, 1)
        repo.simulateRemoteExpenseItemPush("eg-1", listOf(remoteItem))

        assertEquals(listOf(remoteItem), vm.expenseItems.value)
        scope.cancel()
    }

    // ── Slice 2: push still received after local write + channel-churn ────────

    @Test
    fun expenseItemsStillUpdatedFromRemotePushAfterLocalWrite() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val scope = warmUp { vm.expenseItems.collect { } }

        // Local write triggers reloadExpenseItems → flatMapLatest rebuilds subscription
        vm.addExpenseItem("Pizza", "50.00", "2")

        // Simulate a Realtime event from another device arriving after the rebuild
        val remoteItem = ExpenseItem("remote-1", "tg-1", "eg-1", "Hotel", 20000L, 1)
        repo.simulateRemoteExpenseItemPush("eg-1", listOf(remoteItem))

        assertEquals(listOf(remoteItem), vm.expenseItems.value)
        scope.cancel()
    }

    // ── Slice 3: remote assignment push propagates ────────────────────────────

    @Test
    fun assignmentsReflectsRemoteRealtimePush() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val scope = warmUp { vm.assignments.collect { } }

        val remoteAssignment = ItemAssignment("item-1", "member-1", 2)
        repo.simulateRemoteAssignmentPush("eg-1", listOf(remoteAssignment))

        assertEquals(listOf(remoteAssignment), vm.assignments.value)
        scope.cancel()
    }

    // ── Slice 4: push still received after local assign + channel-churn ───────

    @Test
    fun assignmentsStillUpdatedFromRemotePushAfterLocalAssign() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val scope = warmUp { vm.assignments.collect { } }

        // Local assign triggers reloadAssignments → flatMapLatest rebuilds subscription
        vm.assignItem(itemId = "item-1", memberId = "member-1", quantityText = "1")

        // Simulate a Realtime event from another device arriving after the rebuild
        val remoteAssignment = ItemAssignment("item-2", "member-2", 3)
        repo.simulateRemoteAssignmentPush("eg-1", listOf(remoteAssignment))

        assertEquals(listOf(remoteAssignment), vm.assignments.value)
        scope.cancel()
    }

    // ── Slice 4b: push still received after local delete item + channel-churn ──

    @Test
    fun expenseItemsStillUpdatedFromRemotePushAfterLocalDelete() {
        val existing = ExpenseItem("item-1", "tg-1", "eg-1", "Taxi", 5000L, 1)
        val repo = FakeTravelRepository(initialExpenseItems = mapOf("eg-1" to listOf(existing)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val scope = warmUp { vm.expenseItems.collect { } }

        vm.deleteExpenseItem("item-1")

        val remoteItem = ExpenseItem("remote-1", "tg-1", "eg-1", "Hotel", 20000L, 1)
        repo.simulateRemoteExpenseItemPush("eg-1", listOf(remoteItem))

        assertEquals(listOf(remoteItem), vm.expenseItems.value)
        scope.cancel()
    }

    // ── Slice 5: remote expense group push propagates ─────────────────────────

    @Test
    fun expenseGroupsReflectsRemoteRealtimePush() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.expenseGroups.collect { } }

        val remoteGroup = ExpenseGroup("remote-eg-1", "tg-1", "Cena", ExpenseGroupState.Open, 5000L)
        repo.simulateRemoteExpenseGroupPush("tg-1", listOf(remoteGroup))

        assertEquals(listOf(remoteGroup), vm.expenseGroups.value)
        scope.cancel()
    }

    // ── Slice 5b: push still received after local delete group + churn ──────────

    @Test
    fun expenseGroupsStillUpdatedFromRemotePushAfterLocalDelete() {
        val existing = ExpenseGroup("eg-1", "tg-1", "Almuerzo", ExpenseGroupState.Open, 0L)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(existing)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.expenseGroups.collect { } }

        vm.deleteExpenseGroup("eg-1")

        val remoteGroup = ExpenseGroup("remote-eg-2", "tg-1", "Cena", ExpenseGroupState.Open, 10000L)
        repo.simulateRemoteExpenseGroupPush("tg-1", listOf(remoteGroup))

        assertEquals(listOf(remoteGroup), vm.expenseGroups.value)
        scope.cancel()
    }

    // ── Slice 6: push still received after local createExpenseGroup + churn ───

    @Test
    fun expenseGroupsStillUpdatedFromRemotePushAfterLocalCreate() {
        val repo = FakeTravelRepository()
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val scope = warmUp { vm.expenseGroups.collect { } }

        // Local create triggers reloadExpenseGroups → flatMapLatest rebuilds subscription
        vm.createExpenseGroup("Almuerzo")

        // Simulate a Realtime event from another device arriving after the rebuild
        val remoteGroup = ExpenseGroup("remote-eg-2", "tg-1", "Cena", ExpenseGroupState.Open, 10000L)
        repo.simulateRemoteExpenseGroupPush("tg-1", listOf(remoteGroup))

        assertEquals(listOf(remoteGroup), vm.expenseGroups.value)
        scope.cancel()
    }
}
