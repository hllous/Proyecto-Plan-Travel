package com.hllous.plantravel.domain.model

enum class MemberRole {
    ADMIN,
    USER
}

data class TravelGroup(
    val id: String,
    val name: String,
    val adminUserId: String? = null,
    val memberCount: Int = 0,
    val tripDestinationPlaceId: String? = null,
    val tripDestinationName: String? = null,
    val tripDestinationLat: Double? = null,
    val tripDestinationLng: Double? = null,
)

data class DestinationCity(
    val name: String,
    val province: String,
    val region: String,
    val lat: Double,
    val lng: Double,
    val wikipediaTitle: String = "",
)

data class StoredDestination(
    val id: String,
    val source: String,
    val sourceId: String,
    val name: String,
    val normalizedName: String,
    val province: String,
    val region: String,
    val countryCode: String,
    val lat: Double,
    val lng: Double,
    val population: Int,
    val googlePlaceId: String? = null,
    val googlePhotoUrl: String? = null,
    val wikipediaTitle: String? = null,
    val wikipediaPhotoUrl: String? = null,
    val displayPhotoUrl: String? = null,
    val isActive: Boolean = true,
)

data class DestinationDraft(
    val source: String,
    val sourceId: String,
    val name: String,
    val province: String,
    val region: String,
    val countryCode: String = "AR",
    val lat: Double,
    val lng: Double,
    val population: Int = 0,
    val googlePlaceId: String? = null,
    val googlePhotoUrl: String? = null,
    val wikipediaTitle: String? = null,
    val wikipediaPhotoUrl: String? = null,
    val displayPhotoUrl: String? = null,
    val isActive: Boolean = true,
)

data class PlaceResult(
    val placeId: String,
    val name: String,
    val photoUrl: String,
    val rating: Double,
    val reviewCount: Int,
    val address: String,
    val lat: Double,
    val lng: Double,
    val primaryType: String? = null,
    val types: List<String> = emptyList(),
    val photoReference: String = "",
)

data class RankedRecommendations(
    val top: List<PlaceResult>,
    val others: List<PlaceResult>,
)

data class ItineraryEvent(
    val id: String,
    val groupId: String,
    val name: String,
    val date: String,
    val timeOfDay: String? = null,
    val description: String? = null,
    val placeId: String? = null,
    val createdByMemberId: String,
)

enum class PollType { DESTINATION, ACTIVITY }
enum class PollState { OPEN, CLOSED }

data class Poll(
    val id: String,
    val groupId: String,
    val type: PollType,
    val state: PollState,
    val expiresAt: String? = null,
    val winnerPlaceId: String? = null,
)

data class PollCandidate(
    val id: String,
    val pollId: String,
    val placeId: String,
    val name: String,
    val photoUrl: String,
    val addedByMemberId: String,
    val lat: Double = 0.0,
    val lng: Double = 0.0,
    val voteCount: Int = 0,
    val votedByCurrentMember: Boolean = false,
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
