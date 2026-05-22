package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseSettlementCalculatorTest {
    private val calculator = ExpenseSettlementCalculator()

    @Test
    fun unassignedQuantityCreatesWarningAndDoesNotChargeAdmin() {
        val admin = member(id = 1, name = "Admin", role = MemberRole.ADMIN)
        val member = member(id = 2, name = "Nico")
        val item = item(id = 10, name = "Tickets", totalPriceCents = 6000, quantity = 6)

        val result = calculator.calculate(
            members = listOf(admin, member),
            items = listOf(item),
            assignments = listOf(ItemAssignment(itemId = item.id, memberId = member.id, quantity = 4))
        )

        assertEquals(0, result.memberSettlements.single { it.memberId == admin.id }.amountCents)
        assertEquals(4000, result.memberSettlements.single { it.memberId == member.id }.amountCents)
        assertEquals(1, result.warnings.size)
        assertEquals(2, result.warnings.single().unassignedQuantity)
        assertEquals(2000, result.warnings.single().unassignedAmountCents)
    }

    @Test
    fun fullyAssignedExpenseItemHasNoSettlementWarning() {
        val first = member(id = 1, name = "Nico")
        val second = member(id = 2, name = "Juli")
        val item = item(id = 10, name = "Dinner", totalPriceCents = 9000, quantity = 3)

        val result = calculator.calculate(
            members = listOf(first, second),
            items = listOf(item),
            assignments = listOf(
                ItemAssignment(itemId = item.id, memberId = first.id, quantity = 1),
                ItemAssignment(itemId = item.id, memberId = second.id, quantity = 2)
            )
        )

        assertTrue(result.warnings.isEmpty())
        assertEquals(3000, result.memberSettlements.single { it.memberId == first.id }.amountCents)
        assertEquals(6000, result.memberSettlements.single { it.memberId == second.id }.amountCents)
    }

    @Test
    fun roundingCentsAreSpreadByAscendingMemberId() {
        val lowerId = member(id = 1, name = "Nico")
        val middleId = member(id = 2, name = "Juli")
        val higherId = member(id = 3, name = "Ana")
        val item = item(id = 10, name = "Taxi", totalPriceCents = 100, quantity = 3)

        val result = calculator.calculate(
            members = listOf(higherId, lowerId, middleId),
            items = listOf(item),
            assignments = listOf(
                ItemAssignment(itemId = item.id, memberId = higherId.id, quantity = 1),
                ItemAssignment(itemId = item.id, memberId = middleId.id, quantity = 1),
                ItemAssignment(itemId = item.id, memberId = lowerId.id, quantity = 1)
            )
        )

        assertEquals(34, result.memberSettlements.single { it.memberId == lowerId.id }.amountCents)
        assertEquals(33, result.memberSettlements.single { it.memberId == middleId.id }.amountCents)
        assertEquals(33, result.memberSettlements.single { it.memberId == higherId.id }.amountCents)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun emptyInputsProduceEmptySettlementResult() {
        val result = calculator.calculate(
            members = emptyList(),
            items = emptyList(),
            assignments = emptyList()
        )

        assertTrue(result.memberSettlements.isEmpty())
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun assignmentsForMissingMembersDoNotCreateStaleMemberSettlements() {
        val remainingMember = member(id = 1, name = "Nico")
        val item = item(id = 10, name = "Tickets", totalPriceCents = 2000, quantity = 2)

        val result = calculator.calculate(
            members = listOf(remainingMember),
            items = listOf(item),
            assignments = listOf(ItemAssignment(itemId = item.id, memberId = 99, quantity = 1))
        )

        assertEquals(listOf(remainingMember.id), result.memberSettlements.map { it.memberId })
        assertEquals(0, result.memberSettlements.single().amountCents)
        assertEquals(2, result.warnings.single().unassignedQuantity)
        assertEquals(2000, result.warnings.single().unassignedAmountCents)
    }

    @Test
    fun reducingAssignedQuantityCreatesOrIncreasesSettlementWarning() {
        val member = member(id = 1, name = "Nico")
        val expenseItem = item(id = 10, name = "Tickets", totalPriceCents = 6000, quantity = 6)

        val fullResult = calculator.calculate(
            members = listOf(member),
            items = listOf(expenseItem),
            assignments = listOf(ItemAssignment(itemId = expenseItem.id, memberId = member.id, quantity = 6))
        )
        assertTrue(fullResult.warnings.isEmpty())
        assertEquals(6000, fullResult.memberSettlements.single().amountCents)

        val reducedResult = calculator.calculate(
            members = listOf(member),
            items = listOf(expenseItem),
            assignments = listOf(ItemAssignment(itemId = expenseItem.id, memberId = member.id, quantity = 4))
        )
        assertEquals(1, reducedResult.warnings.size)
        assertEquals(2, reducedResult.warnings.single().unassignedQuantity)
        assertEquals(4000, reducedResult.memberSettlements.single().amountCents)

        val furtherReducedResult = calculator.calculate(
            members = listOf(member),
            items = listOf(expenseItem),
            assignments = listOf(ItemAssignment(itemId = expenseItem.id, memberId = member.id, quantity = 2))
        )
        assertEquals(1, furtherReducedResult.warnings.size)
        assertEquals(4, furtherReducedResult.warnings.single().unassignedQuantity)
        assertEquals(2000, furtherReducedResult.memberSettlements.single().amountCents)
    }

    @Test
    fun deletedExpenseItemIsRemovedFromSettlementCalculation() {
        val member = member(id = 1, name = "Nico")
        val deletedItem = item(id = 10, name = "Tickets", totalPriceCents = 6000, quantity = 6)
        val remainingItem = item(id = 20, name = "Dinner", totalPriceCents = 3000, quantity = 3)

        val result = calculator.calculate(
            members = listOf(member),
            items = listOf(remainingItem),
            assignments = listOf(
                ItemAssignment(itemId = deletedItem.id, memberId = member.id, quantity = 6),
                ItemAssignment(itemId = remainingItem.id, memberId = member.id, quantity = 3)
            )
        )

        assertEquals(3000, result.memberSettlements.single().amountCents)
        assertTrue(result.warnings.isEmpty())
    }

    private fun member(id: Long, name: String, role: MemberRole = MemberRole.USER) =
        GroupMember(id = id, groupId = 1, name = name, role = role)

    private fun item(id: Long, name: String, totalPriceCents: Long, quantity: Int) =
        ExpenseItem(id = id, groupId = 1, name = name, totalPriceCents = totalPriceCents, quantity = quantity)
}
