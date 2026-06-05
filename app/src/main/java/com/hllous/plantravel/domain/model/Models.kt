package com.hllous.plantravel.domain.model

enum class MemberRole {
    ADMIN,
    USER
}

data class TravelGroup(
    val id: String,
    val name: String,
    val adminUserId: String? = null,
    val memberCount: Int = 0
)

data class GroupMember(
    val id: String,
    val groupId: String,
    val name: String,
    val userId: String,
    val role: MemberRole
)

sealed class ConsumeInviteFailure : Exception() {
    object Unauthenticated : ConsumeInviteFailure() { override val message = "Debes iniciar sesion" }
    object NotFound : ConsumeInviteFailure() { override val message = "Codigo de invitacion invalido" }
    object Expired : ConsumeInviteFailure() { override val message = "El codigo de invitacion vencio" }
    object AlreadyMember : ConsumeInviteFailure() { override val message = "Ya sos miembro de este grupo" }
}

data class InviteToken(
    val code: String,
    val groupId: String,
    val link: String,
    val expiresAtMillis: Long
)

data class DestinationRecommendation(
    val region: String,
    val destination: String,
    val recommendation: String
)

data class ExpenseItem(
    val id: String,
    val groupId: String,
    val expenseGroupId: String,
    val name: String,
    val totalPriceCents: Long,
    val quantity: Int
)

data class ItemAssignment(
    val itemId: String,
    val memberId: String,
    val quantity: Int
)

data class MemberSettlement(
    val memberId: String,
    val memberName: String,
    val amountCents: Long
)

data class SettlementWarning(
    val itemId: String,
    val itemName: String,
    val unassignedQuantity: Int,
    val unassignedAmountCents: Long
)

data class SettlementResult(
    val memberSettlements: List<MemberSettlement>,
    val warnings: List<SettlementWarning>
)

sealed class ExpenseGroupState {
    object Open : ExpenseGroupState()
    object Finalized : ExpenseGroupState()
}

data class PeerToPerDebt(
    val fromMemberId: String,
    val fromMemberName: String,
    val toMemberId: String,
    val toMemberName: String,
    val amountCents: Long
)

data class PaymentStatus(
    val fromMemberId: String,
    val toMemberId: String,
    val expenseGroupId: String,
    val debtorConfirmed: Boolean,
    val creditorConfirmed: Boolean,
)

data class PeerToPerDebtUiModel(
    val debt: PeerToPerDebt,
    val deepLink: String?,
    val debtorConfirmed: Boolean,
    val creditorConfirmed: Boolean,
)

data class ExpenseGroup(
    val id: String,
    val groupId: String,
    val name: String,
    val state: ExpenseGroupState,
    val totalPriceCents: Long,
    val category: String? = null,
    val createdAtMillis: Long? = null,
    val pinnedAtMillis: Long? = null,
    val paidByMemberId: String? = null,
)
