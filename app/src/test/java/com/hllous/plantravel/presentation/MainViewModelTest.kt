package com.hllous.plantravel.presentation

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import com.hllous.plantravel.domain.settlement.AssignmentRejectionReason
import com.hllous.plantravel.domain.usecase.AddExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.AssignItemToMemberUseCase
import com.hllous.plantravel.domain.usecase.CalculateSettlementUseCase
import com.hllous.plantravel.domain.usecase.ConsumeInviteUseCase
import com.hllous.plantravel.domain.usecase.DeleteExpenseItemUseCase
import com.hllous.plantravel.domain.usecase.DeleteInviteUseCase
import com.hllous.plantravel.domain.usecase.GenerateInviteUseCase
import com.hllous.plantravel.presentation.group.SelectedGroupHolder
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class MainViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        repo: FakeTravelRepository = FakeTravelRepository(),
        session: FakeSessionProvider = FakeSessionProvider(userId = "user-1", displayName = "Test"),
        holder: SelectedGroupHolder = SelectedGroupHolder()
    ): MainViewModel {
        return MainViewModel(
            repository = repo,
            sessionProvider = session,
            selectedGroupHolder = holder,
            generateInviteUseCase = GenerateInviteUseCase(repo),
            deleteInviteUseCase = DeleteInviteUseCase(repo),
            consumeInviteUseCase = ConsumeInviteUseCase(repo, session),
            addExpenseItemUseCase = AddExpenseItemUseCase(repo),
            assignItemToMemberUseCase = AssignItemToMemberUseCase(repo),
            deleteExpenseItemUseCase = DeleteExpenseItemUseCase(repo),
            calculateSettlementUseCase = CalculateSettlementUseCase(repo)
        )
    }

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
}
