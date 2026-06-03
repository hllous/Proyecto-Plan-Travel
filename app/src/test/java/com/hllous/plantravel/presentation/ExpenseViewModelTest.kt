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
        deleteExpenseGroupUseCase = DeleteExpenseGroupUseCase(repo),
        finalizeExpenseGroupUseCase = FinalizeExpenseGroupUseCase(repo),
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
        assertEquals(null, vm.message.value)
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

        assertEquals("Error al crear grupo de gastos", vm.message.value)
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
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
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
        val settlementResult = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement("m-a", "Alice", 6000),
                MemberSettlement("m-b", "Bob", 0),
            ),
            warnings = emptyList()
        )
        // m-b is the creditor; their userId must be in mpAliasByUserId
        val members = mapOf("tg-1" to listOf(
            GroupMember(id = "m-a", groupId = "tg-1", name = "Alice", userId = "user-a", role = MemberRole.USER),
            GroupMember(id = "m-b", groupId = "tg-1", name = "Bob", userId = "user-b", role = MemberRole.USER),
        ))
        val repo = FakeTravelRepository(
            settlementResult = settlementResult,
            initialMembers = members,
            mpAliasByUserId = mapOf("user-b" to "bob.mp"),
        )
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }
        val job = CoroutineScope(UnconfinedTestDispatcher()).launch { vm.members.collect {} }

        vm.refreshSettlement()

        val uiModels = vm.peerToPerDebtsWithLinks.value
        assertEquals(1, uiModels.size)
        assertEquals("mercadopago://send?amount=30&alias=bob.mp", uiModels.first().deepLink)
        job.cancel()
    }

    @Test
    fun peerToPerDebtsAreDerivedFromSettlementResult() {
        val settlementResult = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement("m-a", "Alice", 6000),
                MemberSettlement("m-b", "Bob", 0),
            ),
            warnings = emptyList()
        )
        val repo = FakeTravelRepository(settlementResult = settlementResult)
        val holder = SelectedGroupHolder().also { it.selectedGroupId.value = "tg-1" }
        val vm = viewModel(repo = repo, holder = holder).also { it.selectExpenseGroup("eg-1") }

        vm.refreshSettlement()

        val debts = vm.peerToPerDebts.value
        assertEquals(1, debts.size)
        assertEquals("m-a", debts.first().fromMemberId)
        assertEquals("m-b", debts.first().toMemberId)
        assertEquals(3000L, debts.first().amountCents)
    }

    @Test
    fun finalizeExpenseGroupNetworkErrorShowsMessage() {
        val group = ExpenseGroup(id = "eg-1", groupId = "tg-1", name = "Cenas", state = ExpenseGroupState.Open, totalPriceCents = 0)
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
}
