package com.hllous.plantravel.domain.expense

import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.PaymentStatus
import com.hllous.plantravel.domain.model.SettlementResult
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseDashboardServiceTest {

    private fun expenseGroup(
        id: String,
        totalPriceCents: Long = 0L,
        state: ExpenseGroupState = ExpenseGroupState.Open,
        pinnedAtMillis: Long? = null,
        createdAtMillis: Long? = null,
        paidByMemberId: String? = null,
    ) = ExpenseGroup(
        id = id,
        groupId = "group-1",
        name = "Group $id",
        state = state,
        totalPriceCents = totalPriceCents,
        pinnedAtMillis = pinnedAtMillis,
        createdAtMillis = createdAtMillis,
        paidByMemberId = paidByMemberId,
    )

    private fun groupMember(id: String, userId: String = "user-$id") = GroupMember(
        id = id,
        groupId = "group-1",
        name = "Member $id",
        userId = userId,
        role = MemberRole.USER,
    )

    // ── computeDashboard ─────────────────────────────────────────────────────

    @Test
    fun `computeDashboard with no groups returns zero state`() = runTest {
        val service = ExpenseDashboardService(FakeTravelRepository())
        val result = service.computeDashboard(emptyList(), null)
        assertEquals(ExpenseDashboardState(), result)
    }

    @Test
    fun `computeDashboard aggregates totalCents across groups`() = runTest {
        val groups = listOf(
            expenseGroup("g1", totalPriceCents = 1000L),
            expenseGroup("g2", totalPriceCents = 2000L),
        )
        val service = ExpenseDashboardService(FakeTravelRepository())
        val result = service.computeDashboard(groups, null)
        assertEquals(3000L, result.totalCents)
    }

    @Test
    fun `computeDashboard splits movements into pinned and recent`() = runTest {
        val pinned = expenseGroup("g1", pinnedAtMillis = 1000L)
        val recent = expenseGroup("g2", pinnedAtMillis = null)
        val service = ExpenseDashboardService(FakeTravelRepository())
        val result = service.computeDashboard(listOf(pinned, recent), null)
        assertEquals(1, result.pinnedMovements.size)
        assertEquals("g1", result.pinnedMovements.single().group.id)
        assertEquals(1, result.recentMovements.size)
        assertEquals("g2", result.recentMovements.single().group.id)
    }

    @Test
    fun `computeDashboard derives memberNetCents from payer perspective`() = runTest {
        val payerMemberId = "member-1"
        val debtorMemberId = "member-2"
        val group = expenseGroup("eg1", paidByMemberId = payerMemberId)
        val settlements = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement(payerMemberId, "Payer", 0L),
                MemberSettlement(debtorMemberId, "Debtor", 3000L),
            ),
            warnings = emptyList(),
        )
        val repo = FakeTravelRepository(
            settlementResultsByExpenseGroupId = mapOf("eg1" to settlements),
        )
        val debtor = groupMember(id = debtorMemberId)
        val service = ExpenseDashboardService(repo)
        val result = service.computeDashboard(listOf(group), debtor)
        assertEquals(3000L, result.memberNetCents)
    }

    // ── computeSettlementView ─────────────────────────────────────────────────

    @Test
    fun `computeSettlementView returns empty debts when no payer set`() = runTest {
        val service = ExpenseDashboardService(FakeTravelRepository())
        val result = service.computeSettlementView(
            expenseGroupId = "eg1",
            payerMemberId = null,
            members = emptyList(),
        )
        assertTrue(result.debts.isEmpty())
    }

    @Test
    fun `computeSettlementView builds PeerToPerDebt from settlement`() = runTest {
        val payerMemberId = "member-1"
        val debtorMemberId = "member-2"
        val settlements = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement(payerMemberId, "Payer", 0L),
                MemberSettlement(debtorMemberId, "Debtor", 3000L),
            ),
            warnings = emptyList(),
        )
        val repo = FakeTravelRepository(settlementResult = settlements)
        val service = ExpenseDashboardService(repo)
        val result = service.computeSettlementView(
            expenseGroupId = "eg1",
            payerMemberId = payerMemberId,
            members = emptyList(),
        )
        assertEquals(1, result.debts.size)
        assertEquals(debtorMemberId, result.debts.single().debt.fromMemberId)
        assertEquals(payerMemberId, result.debts.single().debt.toMemberId)
        assertEquals(3000L, result.debts.single().debt.amountCents)
    }

    @Test
    fun `computeSettlementView enriches debt with MP alias deep link`() = runTest {
        val payerMemberId = "member-1"
        val payerUserId = "user-1"
        val debtorMemberId = "member-2"
        val settlements = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement(payerMemberId, "Payer", 0L),
                MemberSettlement(debtorMemberId, "Debtor", 5000L),
            ),
            warnings = emptyList(),
        )
        val repo = FakeTravelRepository(
            settlementResult = settlements,
            mpAliasByUserId = mapOf(payerUserId to "payer.alias"),
        )
        val members = listOf(groupMember(id = payerMemberId, userId = payerUserId))
        val service = ExpenseDashboardService(repo)
        val result = service.computeSettlementView(
            expenseGroupId = "eg1",
            payerMemberId = payerMemberId,
            members = members,
        )
        assertEquals("mercadopago://send?amount=50&alias=payer.alias", result.debts.single().deepLink)
    }

    @Test
    fun `computeSettlementView includes payment status from repository`() = runTest {
        val payerMemberId = "member-1"
        val debtorMemberId = "member-2"
        val expenseGroupId = "eg1"
        val settlements = SettlementResult(
            memberSettlements = listOf(
                MemberSettlement(payerMemberId, "Payer", 0L),
                MemberSettlement(debtorMemberId, "Debtor", 1000L),
            ),
            warnings = emptyList(),
        )
        val status = PaymentStatus(
            fromMemberId = debtorMemberId,
            toMemberId = payerMemberId,
            expenseGroupId = expenseGroupId,
            debtorConfirmed = true,
            creditorConfirmed = false,
        )
        val repo = FakeTravelRepository(
            settlementResult = settlements,
            paymentStatusByKey = mapOf(Triple(debtorMemberId, payerMemberId, expenseGroupId) to status),
        )
        val service = ExpenseDashboardService(repo)
        val result = service.computeSettlementView(
            expenseGroupId = expenseGroupId,
            payerMemberId = payerMemberId,
            members = emptyList(),
        )
        assertTrue(result.debts.single().debtorConfirmed)
        assertFalse(result.debts.single().creditorConfirmed)
    }
}
