package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.SettlementWarning

class ExpenseSettlementCalculator {
    fun calculate(
        members: List<GroupMember>,
        items: List<ExpenseItem>,
        assignments: List<ItemAssignment>
    ): SettlementResult {
        if (members.isEmpty()) {
            return SettlementResult(memberSettlements = emptyList(), warnings = emptyList())
        }

        val memberIds = members.map { it.id }.toSet()
        val debts = members.associate { it.id to 0L }.toMutableMap()
        val warnings = mutableListOf<SettlementWarning>()
        val assignmentsByItem = assignments
            .filter { it.quantity > 0 && it.memberId in memberIds }
            .groupBy { it.itemId }

        for (item in items) {
            if (item.quantity <= 0 || item.totalPriceCents <= 0) continue

            val itemAssignments = assignmentsByItem[item.id].orEmpty()
            val assignedQuantity = itemAssignments.sumOf { it.quantity }
            val assignedAmountCents = allocateAssignedAmount(item, itemAssignments, debts)
            val unassignedQuantity = (item.quantity - assignedQuantity).coerceAtLeast(0)

            if (unassignedQuantity > 0) {
                warnings += SettlementWarning(
                    itemId = item.id,
                    itemName = item.name,
                    unassignedQuantity = unassignedQuantity,
                    unassignedAmountCents = item.totalPriceCents - assignedAmountCents
                )
            }
        }

        return SettlementResult(
            memberSettlements = members.map {
                MemberSettlement(
                    memberId = it.id,
                    memberName = it.name,
                    amountCents = debts.getOrDefault(it.id, 0L)
                )
            },
            warnings = warnings
        )
    }

    private fun allocateAssignedAmount(
        item: ExpenseItem,
        assignments: List<ItemAssignment>,
        debts: MutableMap<Long, Long>
    ): Long {
        if (assignments.isEmpty()) return 0L

        val orderedAssignments = assignments.sortedBy { it.memberId }
        val baseShares = orderedAssignments.map { assignment ->
            assignment to ((item.totalPriceCents * assignment.quantity) / item.quantity)
        }
        val assignedQuantity = orderedAssignments.sumOf { it.quantity }
        val assignedAmount = (item.totalPriceCents * assignedQuantity) / item.quantity
        var roundingCents = assignedAmount - baseShares.sumOf { it.second }
        var allocated = 0L

        for ((assignment, baseShare) in baseShares) {
            val extraCent = if (roundingCents > 0) 1L else 0L
            val share = baseShare + extraCent
            roundingCents -= extraCent
            allocated += share
            debts[assignment.memberId] = debts.getOrDefault(assignment.memberId, 0L) + share
        }

        return allocated
    }
}
