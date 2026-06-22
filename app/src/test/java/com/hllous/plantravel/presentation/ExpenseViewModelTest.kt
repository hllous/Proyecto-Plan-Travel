package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.AssignmentRejectionReason
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CalculateSettlementUseCase
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.usecase.CreateExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.FinalizeExpenseGroupUseCase
import com.hllous.plantravel.domain.usecase.SetExpenseGroupPayerUseCase
import com.hllous.plantravel.domain.usecase.SetExpenseGroupPinnedUseCase
import com.hllous.plantravel.domain.usecase.UpdateExpenseGroupNameUseCase
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
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
class ExpenseViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1"),
        holder: SelectedGroupHolder = SelectedGroupHolder()
    ) = ExpenseViewModel(
        repository = repo,
        sessionProvider = session,
        selectedGroupHolder = holder,
        addExpenseItemUseCase = AddExpenseItemUseCase(repo),
        assignItemToMemberUseCase = AssignItemToMemberUseCase(repo),
        deleteExpenseItemUseCase = DeleteExpenseItemUseCase(repo),
        calculateSettlementUseCase = CalculateSettlementUseCase(repo),
        createExpenseGroupUseCase = CreateExpenseGroupUseCase(repo),
        updateExpenseGroupNameUseCase = UpdateExpenseGroupNameUseCase(repo),
        deleteExpenseGroupUseCase = DeleteExpenseGroupUseCase(repo),
        finalizeExpenseGroupUseCase = FinalizeExpenseGroupUseCase(repo),
        setExpenseGroupPinnedUseCase = SetExpenseGroupPinnedUseCase(repo),
        setExpenseGroupPayerUseCase = SetExpenseGroupPayerUseCase(repo),
    )

    @Test
    fun assignItemSetsOverAssignedMessageWhenAssignmentIsRejected() {
        val repo = FakeTravelRepository(
            assignOutcome = AssignmentOutcome.Rejected(AssignmentRejectionReason.OVER_ASSIGNED)
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)

        vm.assignItem(itemId = "item-10", memberId = "member-1", quantityText = "5")

        assertEquals("La cantidad asignada supera la cantidad del item", vm.message.value)
    }

    @Test
    fun assignItemSetsNegativeQuantityMessageWhenAssignmentIsRejected() {
        val repo = FakeTravelRepository(
            assignOutcome = AssignmentOutcome.Rejected(AssignmentRejectionReason.NEGATIVE_QUANTITY)
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(repo = repo, holder = holder)

        vm.assignItem(itemId = "item-10", memberId = "member-1", quantityText = "2")

        assertEquals("Cantidad invalida", vm.message.value)
    }

    @Test
    fun addExpenseItemWithBlankNameShowsValidationMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val vm = viewModel(holder = holder).also { it.selectExpenseGroup("expense-group-1") }

        vm.addExpenseItem(name = "  ", unitPriceText = "100", quantityText = "2")

        assertEquals("Carga item, precio unitario y cantidad validos", vm.message.value)
    }

    @Test
    fun addingItemTriggersSettlementRecalculation() {
        val expectedSettlement = SettlementResult(
            memberSettlements = listOf(MemberSettlement(memberId = "m1", memberName = "Nico", amountCents = 5000)),
            warnings = emptyList()
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(settlementResult = expectedSettlement)
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("expense-group-1") }

        vm.addExpenseItem(name = "Taxi", unitPriceText = "50", quantityText = "1")

        assertEquals(expectedSettlement.memberSettlements, vm.settlements.value)
        assertTrue(repo.calculateSettlementCallCount >= 1)
    }

    @Test
    fun deletingItemTriggersSettlementRecalculation() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(
            settlementResult = SettlementResult(emptyList(), emptyList())
        )
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("expense-group-1") }

        vm.deleteExpenseItem("item-10")

        assertTrue(repo.calculateSettlementCallCount >= 1)
        assertTrue(vm.settlements.value.isEmpty())
    }

    @Test
    fun addExpenseItemWithNoGroupSelectedShowsMessage() {
        val vm = viewModel()

        vm.addExpenseItem(name = "Taxi", unitPriceText = "50", quantityText = "1")

        assertEquals("Selecciona un grupo", vm.message.value)
    }

    @Test
    fun addExpenseItemSuccessAddsItemToRepo() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("expense-group-1") }

        vm.addExpenseItem(name = "Taxi", unitPriceText = "50", quantityText = "1")

        assertTrue(repo.calculateSettlementCallCount >= 1)
        assertEquals("Item agregado", vm.message.value)
    }

    @Test
    fun addExpenseItemNetworkErrorShowsErrorMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(addExpenseItemThrows = true)
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("expense-group-1") }

        vm.addExpenseItem(name = "Taxi", unitPriceText = "50", quantityText = "1")

        assertEquals("Error al agregar gasto", vm.message.value)
    }

    @Test
    fun deleteExpenseItemNetworkErrorShowsErrorMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(deleteExpenseItemThrows = true)
        val vm = viewModel(repo = repo, holder = holder)

        vm.deleteExpenseItem("item-10")

        assertEquals("Error al eliminar gasto", vm.message.value)
    }

    @Test
    fun refreshSettlementNetworkErrorShowsErrorMessage() {
        val repo = FakeTravelRepository(calculateSettlementThrows = true)
        val vm = viewModel(repo = repo).also { it.selectExpenseGroup("expense-group-1") }

        vm.refreshSettlement()

        assertEquals("Error al calcular liquidacion", vm.message.value)
    }

    @Test
    fun assignItemWithNonIseNetworkErrorShowsErrorMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "group-1" }
        val repo = FakeTravelRepository(assignItemThrows = true)
        val vm = viewModel(repo = repo, holder = holder)

        vm.assignItem(itemId = "item-10", memberId = "member-1", quantityText = "1")

        assertEquals("Error al asignar", vm.message.value)
    }

    // ── Expense Group tests ─────────────────────────────────────────────────

    @Test
    fun expenseGroupsAreEmptyWhenNoTravelGroupSelected() {
        val vm = viewModel() // no travel group selected

        assertEquals(emptyList<ExpenseGroup>(), vm.expenseGroups.value)
    }

    @Test
    fun expenseGroupsLoadWhenTravelGroupSelected() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 5000)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(group)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect { } }

        assertEquals(listOf(group), vm.expenseGroups.value)
        job.cancel()
    }

    @Test
    fun dashboardAggregatesExpenseTotalsPendingCountAndCurrentMemberNetBalance() {
        // eg-2: m-2 is payer. m-1 consumed 1500 (owes 1500 to m-2). memberNetCents for m-1 = +1500.
        // eg-1: m-1 is payer. m-2 consumed 1000 (owes 1000 to m-1). memberNetCents for m-1 = -1000.
        // Total memberNetCents for m-1 = 1500 + (-1000) = 500.
        val recentGroup = ExpenseGroup(
            id = "eg-2",
            groupId = "tg-1",
            name = "Combustible",
            state = ExpenseGroupState.Open,
            totalPriceCents = 8000,
            createdAtMillis = 200L,
            paidByMemberId = "m-2",
        )
        val olderGroup = ExpenseGroup(
            id = "eg-1",
            groupId = "tg-1",
            name = "Cena",
            state = ExpenseGroupState.Finalized,
            totalPriceCents = 6000,
            createdAtMillis = 100L,
            paidByMemberId = "m-1",
        )
        val currentMember = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.USER)
        val otherMember = GroupMember(id = "m-2", groupId = "tg-1", name = "Sofi", userId = "user-2", role = MemberRole.USER)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(olderGroup, recentGroup)),
            initialMembers = mapOf("tg-1" to listOf(currentMember, otherMember)),
            settlementResultsByExpenseGroupId = mapOf(
                "eg-2" to SettlementResult(
                    memberSettlements = listOf(
                        MemberSettlement("m-1", "Nico", 1500),
                        MemberSettlement("m-2", "Sofi", 6500),
                    ),
                    warnings = emptyList(),
                ),
                "eg-1" to SettlementResult(
                    memberSettlements = listOf(
                        MemberSettlement("m-1", "Nico", 5000),
                        MemberSettlement("m-2", "Sofi", 1000),
                    ),
                    warnings = emptyList(),
                ),
            ),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMembers = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }
        val jobDashboard = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.dashboardState.collect {} }
        var dashboardReady = false
        for (attempt in 0 until 100) {
            val state = vm.dashboardState.value
            if (state.totalCents == 14000L && state.recentMovements.size == 2) {
                dashboardReady = true
                break
            }
            Thread.sleep(20)
        }

        assertTrue("Dashboard state never reached the expected aggregated values", dashboardReady)

        assertEquals(14000L, vm.dashboardState.value.totalCents)
        assertEquals(1, vm.dashboardState.value.pendingGroupsCount)
        assertEquals(500L, vm.dashboardState.value.memberNetCents)
        assertTrue(vm.dashboardState.value.pinnedMovements.isEmpty())
        assertEquals(listOf("eg-2", "eg-1"), vm.dashboardState.value.recentMovements.map { it.group.id })
        assertTrue(repo.calculateSettlementCallCount >= 2)
        jobGroups.cancel()
        jobMembers.cancel()
        jobDashboard.cancel()
    }

    @Test
    fun createExpenseGroupAddsGroupToList() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect { } }

        vm.createExpenseGroup("Cenas")

        assertEquals(1, vm.expenseGroups.value.size)
        assertEquals("Cenas", vm.expenseGroups.value.first().name)
        assertNull(vm.message.value)
        job.cancel()
    }

    @Test
    fun createExpenseGroupNetworkErrorShowsMessage() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val repo = FakeTravelRepository(createExpenseGroupThrows = true)
        val vm = viewModel(repo = repo, holder = holder)

        vm.createExpenseGroup("Cenas")

        assertEquals("Error al crear gasto", vm.message.value)
    }

    @Test
    fun createExpenseGroupPassesCategoryAndInvokesSuccessCallback() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val repo = FakeTravelRepository()
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect { } }
        var callbackCalled = false

        vm.createExpenseGroup("Peajes", "transporte") { callbackCalled = true }

        assertEquals(1, repo.createExpenseGroupCallCount)
        assertEquals("transporte", repo.lastCreatedExpenseGroupCategory)
        assertEquals("transporte", vm.expenseGroups.value.first().category)
        assertTrue(callbackCalled)
        job.cancel()
    }

    @Test
    fun createExpenseGroupDoesNotInvokeSuccessCallbackOnFailure() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val repo = FakeTravelRepository(createExpenseGroupThrows = true)
        val vm = viewModel(repo = repo, holder = holder)
        var callbackCalled = false

        vm.createExpenseGroup("Peajes", "transporte") { callbackCalled = true }

        assertTrue(!callbackCalled)
    }

    @Test
    fun deleteExpenseGroupRemovesGroupFromList() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(group)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect { } }

        vm.deleteExpenseGroup("eg-1")

        assertEquals(emptyList<ExpenseGroup>(), vm.expenseGroups.value)
        job.cancel()
    }

    @Test
    fun deleteExpenseGroupNetworkErrorShowsMessage() {
        val repo = FakeTravelRepository(deleteExpenseGroupThrows = true)
        val vm = viewModel(repo = repo)

        vm.deleteExpenseGroup("eg-1")

        assertEquals("Error al eliminar grupo de gastos", vm.message.value)
    }

    // ── Drill-in state tests ────────────────────────────────────────────────

    @Test
    fun expenseItemsAreEmptyWhenNoExpenseGroupSelected() {
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseItems.collect {} }

        assertEquals(emptyList<ExpenseItem>(), vm.expenseItems.value)
        job.cancel()
    }

    @Test
    fun expenseItemsLoadWhenDrillInEntered() {
        val item = ExpenseItem(id = "item-1", groupId = "tg-1", expenseGroupId = "eg-1", name = "Pizza", totalPriceCents = 1000, quantity = 2)
        val repo = FakeTravelRepository(initialExpenseItems = mapOf("eg-1" to listOf(item)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseItems.collect {} }

        vm.selectExpenseGroup("eg-1")

        assertEquals(listOf(item), vm.expenseItems.value)
        job.cancel()
    }

    // ── Finalized write-lock tests ──────────────────────────────────────────

    @Test
    fun addExpenseItemIsNoOpForFinalizedGroup() {
        val finalizedGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Finalized, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(finalizedGroup)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.addExpenseItem(name = "Taxi", unitPriceText = "50", quantityText = "1")

        assertEquals(0, repo.addExpenseItemCallCount)
        assertNull(vm.message.value)
        job.cancel()
    }

    @Test
    fun deleteExpenseItemIsNoOpForFinalizedGroup() {
        val finalizedGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Finalized, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(finalizedGroup)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.deleteExpenseItem("item-1")

        assertEquals(0, repo.deleteExpenseItemCallCount)
        job.cancel()
    }

    @Test
    fun assignItemIsNoOpForFinalizedGroup() {
        val finalizedGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Finalized, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(finalizedGroup)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.assignItem(itemId = "item-1", memberId = "member-1", quantityText = "1")

        assertEquals(0, repo.assignItemCallCount)
        job.cancel()
    }

    // ── Finalize Expense Group tests (#41) ──────────────────────────────────

    @Test
    fun finalizeExpenseGroupSucceedsForAdmin() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0, paidByMemberId = "m-1")
        val admin = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(admin)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMember = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals(1, repo.finalizeExpenseGroupCallCount)
        assertEquals(ExpenseGroupState.Finalized, vm.expenseGroups.value.first().state)
        assertNull(vm.message.value)
        jobGroups.cancel()
        jobMember.cancel()
    }

    @Test
    fun finalizeExpenseGroupReloadsSnapshotBackedGroups() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0, paidByMemberId = "m-1")
        lateinit var repo: FakeTravelRepository
        repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(
                GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
            )),
            customObserveExpenseGroups = { groupId -> kotlinx.coroutines.flow.flowOf(repo.getExpenseGroupsSnapshot(groupId)) },
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMember = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals(ExpenseGroupState.Finalized, vm.expenseGroups.value.first().state)
        jobGroups.cancel()
        jobMember.cancel()
    }

    @Test
    fun renameExpenseGroupUpdatesName() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(group)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.renameExpenseGroup("eg-1", "Cena final")

        assertEquals("Cena final", vm.expenseGroups.value.first().name)
        job.cancel()
    }

    @Test
    fun setExpenseGroupPinnedMovesGroupIntoPinnedSection() {
        val pinnedGroup = ExpenseGroup(
            id = "eg-1",
            groupId = "tg-1",
            name = "Cenas",
            state = ExpenseGroupState.Open,
            totalPriceCents = 4000,
            createdAtMillis = 100L,
        )
        val regularGroup = ExpenseGroup(
            id = "eg-2",
            groupId = "tg-1",
            name = "Peajes",
            state = ExpenseGroupState.Open,
            totalPriceCents = 2000,
            createdAtMillis = 200L,
        )
        val currentMember = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.USER)
        val otherMember = GroupMember(id = "m-2", groupId = "tg-1", name = "Sofi", userId = "user-2", role = MemberRole.USER)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(pinnedGroup, regularGroup)),
            initialMembers = mapOf("tg-1" to listOf(currentMember, otherMember)),
            settlementResultsByExpenseGroupId = mapOf(
                "eg-1" to SettlementResult(listOf(MemberSettlement("m-1", "Nico", 4000), MemberSettlement("m-2", "Sofi", 0)), emptyList()),
                "eg-2" to SettlementResult(listOf(MemberSettlement("m-1", "Nico", 2000), MemberSettlement("m-2", "Sofi", 0)), emptyList()),
            ),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder)
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMembers = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }
        val jobDashboard = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.dashboardState.collect {} }

        vm.setExpenseGroupPinned("eg-1", true)

        assertEquals(listOf("eg-1"), vm.dashboardState.value.pinnedMovements.map { it.group.id })
        assertEquals(listOf("eg-2"), vm.dashboardState.value.recentMovements.map { it.group.id })
        jobGroups.cancel()
        jobMembers.cancel()
        jobDashboard.cancel()
    }

    @Test
    fun finalizeExpenseGroupIsNoOpForUser() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
        val user = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.USER)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(user)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals(0, repo.finalizeExpenseGroupCallCount)
        job.cancel()
    }

    @Test
    fun peerToPerDebtsWithLinksIncludesDeepLinkWhenAliasAvailable() {
        // Bob (m-b) is the payer; Alice (m-a) consumed 6000 and owes Bob.
        val settlementResult = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement("m-a", "Alice", 6000),
                MemberSettlement("m-b", "Bob", 0),
            ),
            warnings = emptyList()
        )
        val expenseGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 6000, paidByMemberId = "m-b")
        val members = mapOf("tg-1" to listOf(
            GroupMember(id = "m-a", groupId = "tg-1", name = "Alice", userId = "user-a", role = MemberRole.USER),
            GroupMember(id = "m-b", groupId = "tg-1", name = "Bob", userId = "user-b", role = MemberRole.USER),
        ))
        val repo = FakeTravelRepository(
            settlementResult = settlementResult,
            initialMembers = members,
            initialExpenseGroups = mapOf("tg-1" to listOf(expenseGroup)),
            mpAliasByUserId = mapOf("user-b" to "bob.mp"),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val jobMembers = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.members.collect {} }
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.refreshSettlement()

        val uiModels = vm.peerToPerDebtsWithLinks.value
        assertEquals(1, uiModels.size)
        assertEquals("mercadopago://send?amount=60&alias=bob.mp", uiModels.first().deepLink)
        jobMembers.cancel()
        jobGroups.cancel()
    }

    @Test
    fun peerToPerDebtsAreDerivedFromSettlementResult() {
        // Bob (m-b) is the payer; Alice (m-a) consumed 6000 and owes Bob directly.
        val settlementResult = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement("m-a", "Alice", 6000),
                MemberSettlement("m-b", "Bob", 0),
            ),
            warnings = emptyList()
        )
        val expenseGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 6000, paidByMemberId = "m-b")
        val repo = FakeTravelRepository(
            settlementResult = settlementResult,
            initialExpenseGroups = mapOf("tg-1" to listOf(expenseGroup)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.refreshSettlement()

        val debts = vm.peerToPerDebts.value
        assertEquals(1, debts.size)
        assertEquals("m-a", debts.first().fromMemberId)
        assertEquals("m-b", debts.first().toMemberId)
        assertEquals(6000L, debts.first().amountCents)
        job.cancel()
    }

    @Test
    fun finalizeExpenseGroupNetworkErrorShowsMessage() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0, paidByMemberId = "m-1")
        val admin = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(admin)),
            finalizeExpenseGroupThrows = true,
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals("Error al finalizar grupo de gastos", vm.message.value)
        job.cancel()
    }

    // ── Payer tests (#48) ────────────────────────────────────────────────────

    @Test
    fun setPayerUpdatesExpenseGroupPaidByMemberId() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
        val repo = FakeTravelRepository(initialExpenseGroups = mapOf("tg-1" to listOf(group)))
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.setPayer("m-1")

        assertEquals(1, repo.setExpenseGroupPayerCallCount)
        assertEquals("m-1", vm.expenseGroups.value.first().paidByMemberId)
        assertNull(vm.message.value)
        job.cancel()
    }

    @Test
    fun setPayerNetworkErrorShowsMessage() {
        val repo = FakeTravelRepository(setExpenseGroupPayerThrows = true)
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }

        vm.setPayer("m-1")

        assertEquals("Error al seleccionar pagador", vm.message.value)
    }

    @Test
    fun finalizeExpenseGroupIsBlockedWhenNoPayer() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0, paidByMemberId = null)
        val admin = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(admin)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMember = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals(0, repo.finalizeExpenseGroupCallCount)
        assertEquals("Seleccioná quién pagó antes de finalizar", vm.message.value)
        jobGroups.cancel()
        jobMember.cancel()
    }

    @Test
    fun finalizeExpenseGroupIsBlockedWhenItemsAreUnassigned() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 1200, paidByMemberId = "m-1")
        val admin = GroupMember(id = "m-1", groupId = "tg-1", name = "Nico", userId = "user-1", role = MemberRole.ADMIN)
        val item = ExpenseItem(id = "item-1", groupId = "tg-1", expenseGroupId = "eg-1", name = "Pizza", totalPriceCents = 1200, quantity = 4)
        val repo = FakeTravelRepository(
            initialExpenseGroups = mapOf("tg-1" to listOf(group)),
            initialMembers = mapOf("tg-1" to listOf(admin)),
            initialExpenseItems = mapOf("eg-1" to listOf(item)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val jobGroups = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }
        val jobMember = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.currentMember.collect {} }
        val jobItems = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseItems.collect {} }

        vm.finalizeExpenseGroup()

        assertEquals(0, repo.finalizeExpenseGroupCallCount)
        assertEquals("Todos los items deben estar asignados antes de finalizar", vm.message.value)
        jobGroups.cancel()
        jobMember.cancel()
        jobItems.cancel()
    }

    @Test
    fun peerToPerDebtsPointToPayerForEachNonPayerMember() {
        // Alice (m-a) is payer. Bob (m-b) consumed 4000, Carol (m-c) consumed 2000.
        val settlementResult = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement("m-a", "Alice", 0),
                MemberSettlement("m-b", "Bob", 4000),
                MemberSettlement("m-c", "Carol", 2000),
            ),
            warnings = emptyList()
        )
        val expenseGroup = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 6000, paidByMemberId = "m-a")
        val repo = FakeTravelRepository(
            settlementResult = settlementResult,
            initialExpenseGroups = mapOf("tg-1" to listOf(expenseGroup)),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.expenseGroups.collect {} }

        vm.refreshSettlement()

        val debts = vm.peerToPerDebts.value
        assertEquals(2, debts.size)
        assertTrue(debts.all { it.toMemberId == "m-a" })
        assertEquals(4000L, debts.first { it.fromMemberId == "m-b" }.amountCents)
        assertEquals(2000L, debts.first { it.fromMemberId == "m-c" }.amountCents)
        job.cancel()
    }
}
