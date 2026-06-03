package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.PeerToPerDebt

class DebtSimplifier {

    fun simplify(settlements: List<MemberSettlement>): List<PeerToPerDebt> {
        if (settlements.size <= 1) return emptyList()

        val balances = settlements
            .filter { it.amountCents != 0L }
            .map { Balance(it, it.amountCents) }
            .toMutableList()

        val result = mutableListOf<PeerToPerDebt>()

        while (balances.any { it.cents > 0L } && balances.any { it.cents < 0L }) {
            val debtor = balances.maxByOrNull { it.cents }!!
            val creditor = balances.minByOrNull { it.cents }!!

            val amount = minOf(debtor.cents, -creditor.cents)
            result.add(
                PeerToPerDebt(
                    fromMemberId = debtor.settlement.memberId,
                    fromMemberName = debtor.settlement.memberName,
                    toMemberId = creditor.settlement.memberId,
                    toMemberName = creditor.settlement.memberName,
                    amountCents = amount,
                )
            )

            debtor.cents -= amount
            creditor.cents += amount
            balances.removeAll { it.cents == 0L }
        }

        return result
    }

    private data class Balance(val settlement: MemberSettlement, var cents: Long)
}
