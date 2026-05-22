package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.ItemAssignment

enum class AssignmentRejectionReason {
    NEGATIVE_QUANTITY,
    OVER_ASSIGNED
}

sealed class AssignmentValidationResult {
    data object Accepted : AssignmentValidationResult()
    data class Rejected(val reason: AssignmentRejectionReason) : AssignmentValidationResult()
}

class ExpenseAssignmentPolicy {
    fun validate(
        item: ExpenseItem,
        currentAssignments: List<ItemAssignment>,
        memberId: Long,
        requestedQuantity: Int
    ): AssignmentValidationResult {
        if (requestedQuantity < 0) {
            return AssignmentValidationResult.Rejected(AssignmentRejectionReason.NEGATIVE_QUANTITY)
        }

        val assignedToOthers = currentAssignments
            .filter { it.itemId == item.id && it.memberId != memberId }
            .sumOf { it.quantity.coerceAtLeast(0) }
        val requestedTotal = assignedToOthers + requestedQuantity

        return if (requestedTotal > item.quantity) {
            AssignmentValidationResult.Rejected(AssignmentRejectionReason.OVER_ASSIGNED)
        } else {
            AssignmentValidationResult.Accepted
        }
    }
}
