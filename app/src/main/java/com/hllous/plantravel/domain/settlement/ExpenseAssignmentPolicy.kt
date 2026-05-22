package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.ItemAssignment

enum class AssignmentRejectionReason {
    NEGATIVE_QUANTITY,
    OVER_ASSIGNED
}

sealed class AssignmentOutcome {
    data object Accepted : AssignmentOutcome()
    data class Rejected(val reason: AssignmentRejectionReason) : AssignmentOutcome()
}

class ExpenseAssignmentPolicy {
    fun validate(
        item: ExpenseItem,
        currentAssignments: List<ItemAssignment>,
        memberId: Long,
        requestedQuantity: Int
    ): AssignmentOutcome {
        if (requestedQuantity < 0) {
            return AssignmentOutcome.Rejected(AssignmentRejectionReason.NEGATIVE_QUANTITY)
        }

        val assignedToOthers = currentAssignments
            .filter { it.itemId == item.id && it.memberId != memberId }
            .sumOf { it.quantity.coerceAtLeast(0) }
        val requestedTotal = assignedToOthers + requestedQuantity

        return if (requestedTotal > item.quantity) {
            AssignmentOutcome.Rejected(AssignmentRejectionReason.OVER_ASSIGNED)
        } else {
            AssignmentOutcome.Accepted
        }
    }
}
