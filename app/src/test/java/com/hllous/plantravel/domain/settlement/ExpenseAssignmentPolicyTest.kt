package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.ItemAssignment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseAssignmentPolicyTest {
    private val policy = ExpenseAssignmentPolicy()

    @Test
    fun acceptsAssignmentWithinItemQuantity() {
        val result = policy.validate(
            item = item(quantity = 3),
            currentAssignments = listOf(ItemAssignment(itemId = "item-10", memberId = "member-1", quantity = 1)),
            memberId = "member-2",
            requestedQuantity = 2
        )

        assertTrue(result is AssignmentOutcome.Accepted)
    }

    @Test
    fun rejectsOverAssignedExpenseItem() {
        val result = policy.validate(
            item = item(quantity = 3),
            currentAssignments = listOf(ItemAssignment(itemId = "item-10", memberId = "member-1", quantity = 2)),
            memberId = "member-2",
            requestedQuantity = 2
        )

        assertEquals(
            AssignmentOutcome.Rejected(AssignmentRejectionReason.OVER_ASSIGNED),
            result
        )
    }

    @Test
    fun rejectsNegativeAssignedQuantity() {
        val result = policy.validate(
            item = item(quantity = 3),
            currentAssignments = emptyList(),
            memberId = "member-1",
            requestedQuantity = -1
        )

        assertEquals(
            AssignmentOutcome.Rejected(AssignmentRejectionReason.NEGATIVE_QUANTITY),
            result
        )
    }

    @Test
    fun ignoresAssignmentsForOtherExpenseItems() {
        val result = policy.validate(
            item = item(quantity = 3),
            currentAssignments = listOf(
                ItemAssignment(itemId = "item-99", memberId = "member-1", quantity = 99),
                ItemAssignment(itemId = "item-10", memberId = "member-1", quantity = 1)
            ),
            memberId = "member-2",
            requestedQuantity = 2
        )

        assertTrue(result is AssignmentOutcome.Accepted)
    }

    private fun item(quantity: Int) =
        ExpenseItem(id = "item-10", groupId = "group-1", expenseGroupId = "eg-1", name = "Tickets", totalPriceCents = 3000, quantity = quantity)
}
