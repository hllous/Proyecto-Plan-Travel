package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.AssignmentRejectionReason
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CalculateSettlementUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

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
        calculateSettlementUseCase = CalculateSettlementUseCase(repo)
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
        val vm = viewModel(holder = holder)

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
        val vm = viewModel(repo = repo, holder = holder)

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
        val vm = viewModel(repo = repo, holder = holder)

        vm.deleteExpenseItem("item-10")

        assertTrue(repo.calculateSettlementCallCount >= 1)
        assertTrue(vm.settlements.value.isEmpty())
    }
}
