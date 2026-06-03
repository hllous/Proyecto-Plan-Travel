package com.hllous.plantravel.domain.settlement

import com.hllous.plantravel.domain.model.MemberSettlement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DebtSimplifierTest {

    private val simplifier = DebtSimplifier()

    private fun s(id: String, name: String, cents: Long) =
        MemberSettlement(memberId = id, memberName = name, amountCents = cents)

    @Test
    fun twoMemberCaseProducesOneDirect() {
        val debts = simplifier.simplify(listOf(s("a", "Alice", 100), s("b", "Bob", -100)))

        assertEquals(1, debts.size)
        assertEquals("a", debts[0].fromMemberId)
        assertEquals("b", debts[0].toMemberId)
        assertEquals(100L, debts[0].amountCents)
    }

    @Test
    fun threeMemberCaseProducesFewerTransfersThanPairs() {
        // A +50, B +50, C -100 → 2 debts (A→C and B→C), not 3
        val debts = simplifier.simplify(listOf(s("a", "Alice", 50), s("b", "Bob", 50), s("c", "Carol", -100)))

        assertEquals(2, debts.size)
        assertTrue(debts.all { it.toMemberId == "c" })
        assertEquals(100L, debts.sumOf { it.amountCents })
    }

    @Test
    fun memberWithZeroBalanceIsExcluded() {
        val debts = simplifier.simplify(listOf(s("a", "Alice", 0), s("b", "Bob", 100), s("c", "Carol", -100)))

        assertTrue(debts.none { it.fromMemberId == "a" || it.toMemberId == "a" })
        assertEquals(1, debts.size)
    }

    @Test
    fun allZeroSettlementsReturnsEmpty() {
        val debts = simplifier.simplify(listOf(s("a", "Alice", 0), s("b", "Bob", 0)))

        assertTrue(debts.isEmpty())
    }

    @Test
    fun singleMemberReturnsEmpty() {
        val debts = simplifier.simplify(listOf(s("a", "Alice", 100)))

        assertTrue(debts.isEmpty())
    }

    @Test
    fun roundingBalancesThatSumToZeroProduceNoLeftoverDebt() {
        val debts = simplifier.simplify(listOf(s("a", "Alice", 1), s("b", "Bob", -1)))

        assertEquals(1, debts.size)
        assertEquals(1L, debts[0].amountCents)
    }
}
