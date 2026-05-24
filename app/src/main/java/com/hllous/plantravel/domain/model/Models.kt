package com.hllous.plantravel.domain.model

enum class MemberRole {
    ADMIN,
    USER
}

data class TravelGroup(
    val id: Long,
    val name: String,
    val adminMemberId: Long?
)

data class GroupMember(
    val id: Long,
    val groupId: Long,
    val name: String,
    val userId: String?,
    val role: MemberRole
)

sealed class ConsumeInviteFailure : Exception() {
    object Unauthenticated : ConsumeInviteFailure()
    object Expired : ConsumeInviteFailure()
    object AlreadyMember : ConsumeInviteFailure()
}

data class InviteToken(
    val code: String,
    val groupId: Long,
    val link: String,
    val expiresAtMillis: Long
)

data class DestinationRecommendation(
    val region: String,
    val destination: String,
    val recommendation: String
)

data class ExpenseItem(
    val id: Long,
    val groupId: Long,
    val name: String,
    val totalPriceCents: Long,
    val quantity: Int
)

data class ItemAssignment(
    val itemId: Long,
    val memberId: Long,
    val quantity: Int
)

data class MemberSettlement(
    val memberId: Long,
    val memberName: String,
    val amountCents: Long
)

data class SettlementWarning(
    val itemId: Long,
    val itemName: String,
    val unassignedQuantity: Int,
    val unassignedAmountCents: Long
)

data class SettlementResult(
    val memberSettlements: List<MemberSettlement>,
    val warnings: List<SettlementWarning>
)

