package com.hllous.plantravel.domain.expense

import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.PeerToPerDebt
import com.hllous.plantravel.domain.model.PeerToPerDebtUiModel
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.domain.repository.TravelRepository
import javax.inject.Inject

data class ExpenseDashboardMovement(
    val group: ExpenseGroup,
    val memberNetCents: Long,
)

data class ExpenseDashboardState(
    val totalCents: Long = 0,
    val pendingGroupsCount: Int = 0,
    val memberNetCents: Long = 0,
    val pinnedMovements: List<ExpenseDashboardMovement> = emptyList(),
    val recentMovements: List<ExpenseDashboardMovement> = emptyList(),
)

data class SettlementView(
    val settlements: List<MemberSettlement>,
    val warnings: List<SettlementWarning>,
    val debts: List<PeerToPerDebtUiModel>,
)

class ExpenseDashboardService @Inject constructor(
    private val repository: TravelRepository,
) {
    suspend fun computeDashboard(
        groups: List<ExpenseGroup>,
        currentMember: GroupMember?,
    ): ExpenseDashboardState {
        if (groups.isEmpty()) return ExpenseDashboardState()

        val allMovements = groups.map { group ->
            val memberNetCents = currentMember?.let { member ->
                runCatching {
                    val settlements = repository.calculateSettlement(group.id).memberSettlements
                    val payerMemberId = group.paidByMemberId
                    when {
                        payerMemberId == null -> 0L
                        member.id == payerMemberId ->
                            -settlements.filter { it.memberId != payerMemberId }.sumOf { it.amountCents }
                        else ->
                            settlements.firstOrNull { it.memberId == member.id }?.amountCents ?: 0L
                    }
                }.getOrDefault(0L)
            } ?: 0L
            ExpenseDashboardMovement(group = group, memberNetCents = memberNetCents)
        }.sortedWith(
            compareByDescending<ExpenseDashboardMovement> { it.group.pinnedAtMillis ?: Long.MIN_VALUE }
                .thenByDescending { it.group.createdAtMillis ?: Long.MIN_VALUE }
        )

        val pinnedMovements = allMovements.filter { it.group.pinnedAtMillis != null }
        val recentMovements = allMovements.filter { it.group.pinnedAtMillis == null }

        return ExpenseDashboardState(
            totalCents = groups.sumOf { it.totalPriceCents },
            pendingGroupsCount = groups.count { it.state == ExpenseGroupState.Open },
            memberNetCents = allMovements.sumOf { it.memberNetCents },
            pinnedMovements = pinnedMovements,
            recentMovements = recentMovements,
        )
    }

    suspend fun computeSettlementView(
        expenseGroupId: String,
        payerMemberId: String?,
        members: List<GroupMember>,
    ): SettlementView {
        val result = repository.calculateSettlement(expenseGroupId)
        val debts = buildDebtsFromPayerView(result.memberSettlements, payerMemberId)
        val uiModels = debts.map { debt ->
            val creditorUserId = members.firstOrNull { it.id == debt.toMemberId }?.userId
            val mpAlias = if (creditorUserId != null)
                runCatching { repository.getMpAlias(creditorUserId) }.getOrNull()
            else null
            val deepLink = mpAlias?.takeIf { it.isNotBlank() }?.let { alias ->
                val amountPesos = debt.amountCents / 100
                "mercadopago://send?amount=$amountPesos&alias=$alias"
            }
            val status = runCatching {
                repository.getPaymentStatus(debt.fromMemberId, debt.toMemberId, expenseGroupId)
            }.getOrNull()
            PeerToPerDebtUiModel(
                debt = debt,
                deepLink = deepLink,
                debtorConfirmed = status?.debtorConfirmed ?: false,
                creditorConfirmed = status?.creditorConfirmed ?: false,
            )
        }
        return SettlementView(
            settlements = result.memberSettlements,
            warnings = result.warnings,
            debts = uiModels,
        )
    }

    private fun buildDebtsFromPayerView(
        settlements: List<MemberSettlement>,
        payerMemberId: String?,
    ): List<PeerToPerDebt> {
        val payer = settlements.firstOrNull { it.memberId == payerMemberId } ?: return emptyList()
        return settlements
            .filter { it.memberId != payerMemberId && it.amountCents > 0 }
            .map { debtor ->
                PeerToPerDebt(
                    fromMemberId = debtor.memberId,
                    fromMemberName = debtor.memberName,
                    toMemberId = payer.memberId,
                    toMemberName = payer.memberName,
                    amountCents = debtor.amountCents,
                )
            }
    }
}
