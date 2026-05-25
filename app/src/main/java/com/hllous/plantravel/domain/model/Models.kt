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
