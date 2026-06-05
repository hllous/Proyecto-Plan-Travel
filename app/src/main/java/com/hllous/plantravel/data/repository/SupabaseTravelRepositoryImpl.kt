package com.hllous.plantravel.data.repository

import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PaymentStatus
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import com.hllous.plantravel.domain.settlement.ExpenseAssignmentPolicy
import com.hllous.plantravel.domain.settlement.ExpenseSettlementCalculator
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

internal fun isMissingExpenseGroupsColumnError(message: String?, column: String): Boolean {
    val normalized = message?.lowercase() ?: return false
    return "expense_groups" in normalized &&
        column.lowercase() in normalized &&
        (
            "column" in normalized ||
                "schema cache" in normalized ||
                "could not find" in normalized
            )
}

internal fun isMissingExpenseGroupsCategoryColumnError(message: String?): Boolean =
    isMissingExpenseGroupsColumnError(message, "category")

internal fun isMissingExpenseGroupsPinnedAtColumnError(message: String?): Boolean =
    isMissingExpenseGroupsColumnError(message, "pinned_at")

internal fun isMissingExpenseGroupsPaidByMemberIdColumnError(message: String?): Boolean =
    isMissingExpenseGroupsColumnError(message, "paid_by_member_id")

@Singleton
class SupabaseTravelRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TravelRepository {

    // ─── DTOs ───────────────────────────────────────────────────────────────

    @Serializable
    private data class TravelGroupDto(
        val id: String,
        val name: String,
        @SerialName("admin_user_id") val adminUserId: String? = null,
        @SerialName("trip_destination_place_id") val tripDestinationPlaceId: String? = null,
        @SerialName("trip_destination_name") val tripDestinationName: String? = null,
        @SerialName("trip_destination_lat") val tripDestinationLat: Double? = null,
        @SerialName("trip_destination_lng") val tripDestinationLng: Double? = null,
    ) {
        fun toDomain() = TravelGroup(
            id = id,
            name = name,
            adminUserId = adminUserId,
            tripDestinationPlaceId = tripDestinationPlaceId,
            tripDestinationName = tripDestinationName,
            tripDestinationLat = tripDestinationLat,
            tripDestinationLng = tripDestinationLng,
        )
    }

    @Serializable
    private data class GroupMemberDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("user_id") val userId: String,
        val role: String,
        val profiles: ProfileDto? = null
    ) {
        fun toDomain() = GroupMember(
            id = id,
            groupId = groupId,
            name = profiles?.displayName ?: "Usuario",
            userId = userId,
            role = if (role == MemberRole.ADMIN.name) MemberRole.ADMIN else MemberRole.USER
        )
    }

    @Serializable
    private data class ProfileDto(@SerialName("display_name") val displayName: String)

    @Serializable
    private data class GroupMembershipDto(@SerialName("group_id") val groupId: String)

    @Serializable
    private data class InsertGroupDto(
        val id: String,
        val name: String,
        @SerialName("admin_user_id") val adminUserId: String
    )

    @Serializable
    private data class InsertMemberDto(
        @SerialName("group_id") val groupId: String,
        @SerialName("user_id") val userId: String,
        val role: String
    )

    @Serializable
    private data class InviteTokenDto(
        val code: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("expires_at") val expiresAt: String
    ) {
        fun toDomain(): InviteToken {
            val millis = runCatching {
                OffsetDateTime.parse(expiresAt).toInstant().toEpochMilli()
            }.getOrElse { Instant.now().toEpochMilli() }
            return InviteToken(
                code = code,
                groupId = groupId,
                link = "plantravel://invite/$code",
                expiresAtMillis = millis
            )
        }
    }

    @Serializable
    private data class InsertInviteDto(
        val code: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("expires_at") val expiresAt: String
    )

    @Serializable
    private data class MembershipCheckDto(@SerialName("group_id") val groupId: String)

    @Serializable
    private data class ExpenseGroupRefDto(@SerialName("group_id") val groupId: String)

    @Serializable
    private data class DeletedIdDto(val id: String)

    @Serializable
    private data class ExpenseItemSumDto(
        @SerialName("total_price_cents") val totalPriceCents: Long
    )

    @Serializable
    private data class ExpenseGroupDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val name: String,
        val state: String,
        val category: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
        @SerialName("pinned_at") val pinnedAt: String? = null,
        @SerialName("paid_by_member_id") val paidByMemberId: String? = null,
        @SerialName("expense_items") val expenseItems: List<ExpenseItemSumDto> = emptyList()
    ) {
        fun toDomain() = ExpenseGroup(
            id = id,
            groupId = groupId,
            name = name,
            state = if (state == "finalized") ExpenseGroupState.Finalized else ExpenseGroupState.Open,
            totalPriceCents = expenseItems.sumOf { it.totalPriceCents },
            category = category,
            createdAtMillis = createdAt?.let {
                runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
            },
            pinnedAtMillis = pinnedAt?.let {
                runCatching { OffsetDateTime.parse(it).toInstant().toEpochMilli() }.getOrNull()
            },
            paidByMemberId = paidByMemberId,
        )
    }

    @Serializable
    private data class InsertExpenseGroupDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val name: String,
        val category: String? = null,
    )

    @Serializable
    private data class ExpenseItemDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        @SerialName("expense_group_id") val expenseGroupId: String? = null,
        val name: String,
        @SerialName("total_price_cents") val totalPriceCents: Long,
        val quantity: Int
    ) {
        fun toDomain() = ExpenseItem(id = id, groupId = groupId, expenseGroupId = expenseGroupId.orEmpty(), name = name, totalPriceCents = totalPriceCents, quantity = quantity)
    }

    @Serializable
    private data class InsertExpenseItemDto(
        @SerialName("group_id") val groupId: String,
        @SerialName("expense_group_id") val expenseGroupId: String,
        val name: String,
        @SerialName("total_price_cents") val totalPriceCents: Long,
        val quantity: Int
    )

    @Serializable
    private data class ItemAssignmentDto(
        @SerialName("item_id") val itemId: String,
        @SerialName("member_id") val memberId: String,
        val quantity: Int
    ) {
        fun toDomain() = ItemAssignment(itemId = itemId, memberId = memberId, quantity = quantity)
    }

    @Serializable
    private data class UpsertAssignmentDto(
        @SerialName("item_id") val itemId: String,
        @SerialName("member_id") val memberId: String,
        @SerialName("group_id") val groupId: String,
        val quantity: Int
    )

    @Serializable
    private data class ItineraryEventDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val name: String,
        val date: String,
        @SerialName("time_of_day") val timeOfDay: String? = null,
        val description: String? = null,
        @SerialName("place_id") val placeId: String? = null,
        @SerialName("created_by_member_id") val createdByMemberId: String,
    ) {
        fun toDomain() = ItineraryEvent(id, groupId, name, date, timeOfDay, description, placeId, createdByMemberId)
    }

    @Serializable
    private data class InsertItineraryEventDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val name: String,
        val date: String,
        @SerialName("time_of_day") val timeOfDay: String? = null,
        val description: String? = null,
        @SerialName("place_id") val placeId: String? = null,
        @SerialName("created_by_member_id") val createdByMemberId: String,
    )

    @Serializable
    private data class PollDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val type: String,
        val state: String,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("winner_place_id") val winnerPlaceId: String? = null,
    ) {
        fun toDomain() = Poll(
            id = id, groupId = groupId,
            type = if (type == "DESTINATION") PollType.DESTINATION else PollType.ACTIVITY,
            state = if (state == "CLOSED") PollState.CLOSED else PollState.OPEN,
            expiresAt = expiresAt, winnerPlaceId = winnerPlaceId,
        )
    }

    @Serializable
    private data class InsertPollDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val type: String,
        @SerialName("expires_at") val expiresAt: String? = null,
    )

    @Serializable
    private data class PollCandidateDto(
        val id: String,
        @SerialName("poll_id") val pollId: String,
        @SerialName("place_id") val placeId: String,
        val name: String,
        @SerialName("photo_url") val photoUrl: String,
        @SerialName("added_by_member_id") val addedByMemberId: String,
    ) {
        fun toDomain() = PollCandidate(id = id, pollId = pollId, placeId = placeId,
            name = name, photoUrl = photoUrl, addedByMemberId = addedByMemberId)
    }

    @Serializable
    private data class InsertPollCandidateDto(
        val id: String,
        @SerialName("poll_id") val pollId: String,
        @SerialName("place_id") val placeId: String,
        val name: String,
        @SerialName("photo_url") val photoUrl: String,
        @SerialName("added_by_member_id") val addedByMemberId: String,
    )

    @Serializable
    private data class PollVoteDto(
        @SerialName("candidate_id") val candidateId: String,
        @SerialName("member_id") val memberId: String,
    )

    // ─── Fetch helpers ───────────────────────────────────────────────────────

    private val settlementCalculator = ExpenseSettlementCalculator()
    private val assignmentPolicy = ExpenseAssignmentPolicy()

    private suspend fun fetchItineraryEvents(groupId: String): List<ItineraryEvent> =
        supabase.from("itinerary_events")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<ItineraryEventDto>()
            .map { it.toDomain() }

    private suspend fun fetchActivePoll(groupId: String): Poll? =
        supabase.from("group_polls")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<PollDto>()
            .firstOrNull()?.toDomain()

    private suspend fun fetchPollCandidatesWithVotes(pollId: String, currentMemberId: String?): List<PollCandidate> {
        val candidates = supabase.from("poll_candidates")
            .select { filter { eq("poll_id", pollId) } }
            .decodeList<PollCandidateDto>()
            .map { it.toDomain() }
        if (candidates.isEmpty()) return emptyList()
        val votes = supabase.from("poll_votes")
            .select { filter { isIn("candidate_id", candidates.map { it.id }) } }
            .decodeList<PollVoteDto>()
        return candidates.map { c ->
            val votesForCandidate = votes.filter { it.candidateId == c.id }
            c.copy(
                voteCount = votesForCandidate.size,
                votedByCurrentMember = votesForCandidate.any { it.memberId == currentMemberId },
            )
        }
    }

    private suspend fun fetchExpenseItems(expenseGroupId: String): List<ExpenseItem> =
        supabase.from("expense_items")
            .select { filter { eq("expense_group_id", expenseGroupId) } }
            .decodeList<ExpenseItemDto>()
            .map { it.toDomain() }

    private suspend fun fetchExpenseGroupTravelGroupId(expenseGroupId: String): String =
        supabase.from("expense_groups")
            .select(Columns.list("group_id")) { filter { eq("id", expenseGroupId) } }
            .decodeSingle<ExpenseGroupRefDto>()
            .groupId

    private suspend fun fetchAssignmentsByItemIds(itemIds: List<String>): List<ItemAssignment> {
        if (itemIds.isEmpty()) return emptyList()
        return supabase.from("item_assignments")
            .select { filter { isIn("item_id", itemIds) } }
            .decodeList<ItemAssignmentDto>()
            .map { it.toDomain() }
    }

    private suspend fun fetchAssignments(expenseGroupId: String): List<ItemAssignment> {
        val items = fetchExpenseItems(expenseGroupId)
        return fetchAssignmentsByItemIds(items.map { it.id })
    }

    private suspend fun fetchAssignmentsForItem(itemId: String): List<ItemAssignment> =
        supabase.from("item_assignments")
            .select { filter { eq("item_id", itemId) } }
            .decodeList<ItemAssignmentDto>()
            .map { it.toDomain() }

    private suspend fun fetchGroupsForUser(userId: String): List<TravelGroup> {
        val memberships = supabase.from("group_members")
            .select(Columns.list("group_id")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<GroupMembershipDto>()
        val groupIds = memberships.map { it.groupId }
        if (groupIds.isEmpty()) return emptyList()
        val countByGroupId = supabase.from("group_members")
            .select(Columns.list("group_id")) {
                filter { isIn("group_id", groupIds) }
            }
            .decodeList<GroupMembershipDto>()
            .groupingBy { it.groupId }
            .eachCount()
        return supabase.from("travel_groups")
            .select {
                filter { isIn("id", groupIds) }
            }
            .decodeList<TravelGroupDto>()
            .map { it.toDomain().copy(memberCount = countByGroupId[it.id] ?: 0) }
    }

    private suspend fun fetchInvites(groupId: String): List<InviteToken> {
        return supabase.from("invite_tokens")
            .select {
                filter { eq("group_id", groupId) }
            }
            .decodeList<InviteTokenDto>()
            .map { it.toDomain() }
    }

    private suspend fun fetchMembers(groupId: String): List<GroupMember> {
        return supabase.from("group_members")
            .select(Columns.raw("id, group_id, user_id, role, profiles!user_id(display_name)")) {
                filter { eq("group_id", groupId) }
            }
            .decodeList<GroupMemberDto>()
            .map { it.toDomain() }
    }

    // ─── Observe ─────────────────────────────────────────────────────────────

    override fun observeGroups(): Flow<List<TravelGroup>> = channelFlow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@channelFlow

        send(fetchGroupsForUser(userId))

        val channel = supabase.channel("groups-for-$userId-${UUID.randomUUID()}")
        val memberChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }
        val groupChanges = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "travel_groups"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            merge(memberChanges, groupChanges).collect { send(fetchGroupsForUser(userId)) }
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    override fun observeMembers(groupId: String): Flow<List<GroupMember>> = channelFlow {
        send(fetchMembers(groupId))

        // Postgres Changes: detects local inserts/updates/deletes (same-user actions work reliably)
        val pgChannel = supabase.channel("members-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }

        // Broadcast: fallback for cross-user inserts that Postgres Changes RLS may block
        val bcChannel = supabase.channel("members-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "member_joined")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchMembers(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun broadcastMemberJoined(groupId: String) {
        val channel = supabase.channel("members-broadcast-$groupId")
        try {
            channel.subscribe(blockUntilSubscribed = true)
            channel.broadcast(event = "member_joined", message = buildJsonObject {})
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    // ─── Group CRUD ──────────────────────────────────────────────────────────

    override suspend fun createGroup(groupName: String): String {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated to create a group")
        val groupId = UUID.randomUUID().toString()
        supabase.from("travel_groups")
            .insert(InsertGroupDto(id = groupId, name = groupName, adminUserId = userId))
        supabase.from("group_members")
            .insert(InsertMemberDto(groupId = groupId, userId = userId, role = MemberRole.ADMIN.name))
        return groupId
    }

    override suspend fun updateGroupName(groupId: String, name: String) {
        supabase.from("travel_groups").update({ set("name", name) }) {
            filter { eq("id", groupId) }
        }
    }

    override suspend fun deleteMember(memberId: String) {
        supabase.from("group_members").delete {
            filter { eq("id", memberId) }
        }
    }

    override suspend fun leaveGroup(groupId: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.from("group_members").delete {
            filter {
                eq("group_id", groupId)
                eq("user_id", userId)
            }
        }
    }

    override suspend fun deleteGroup(groupId: String) {
        // FK cascade on group_id → travel_groups.id handles members, invites, expense_items, assignments
        supabase.from("travel_groups").delete {
            filter { eq("id", groupId) }
        }
    }

    // ─── Stubs (implemented in subsequent slices) ─────────────────────────────

    override fun observeInvites(groupId: String): Flow<List<InviteToken>> = channelFlow {
        send(fetchInvites(groupId))

        val channel = supabase.channel("invites-$groupId-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "invite_tokens"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            changes.collect { send(fetchInvites(groupId)) }
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    override suspend fun generateInvite(groupId: String): InviteToken {
        val code = UUID.randomUUID().toString().replace("-", "").take(8).uppercase()
        val expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(24)
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
        val dto = supabase.from("invite_tokens")
            .insert(InsertInviteDto(code = code, groupId = groupId, expiresAt = expiresAt)) {
                select()
            }
            .decodeSingle<InviteTokenDto>()
        return dto.toDomain()
    }

    override suspend fun deleteInvite(code: String) {
        supabase.from("invite_tokens").delete {
            filter { eq("code", code) }
        }
    }

    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String> {
        val token = runCatching {
            supabase.from("invite_tokens")
                .select { filter { eq("code", code) } }
                .decodeList<InviteTokenDto>()
                .firstOrNull()
        }.getOrNull() ?: return Result.failure(ConsumeInviteFailure.NotFound)

        val nowMillis = Instant.now().toEpochMilli()
        val expiryMillis = runCatching {
            OffsetDateTime.parse(token.expiresAt).toInstant().toEpochMilli()
        }.getOrElse { 0L }
        if (nowMillis > expiryMillis) return Result.failure(ConsumeInviteFailure.Expired)

        val existing = supabase.from("group_members")
            .select(Columns.list("group_id")) {
                filter {
                    eq("group_id", token.groupId)
                    eq("user_id", userId)
                }
            }
            .decodeList<MembershipCheckDto>()
        if (existing.isNotEmpty()) return Result.failure(ConsumeInviteFailure.AlreadyMember)

        supabase.from("group_members")
            .insert(InsertMemberDto(groupId = token.groupId, userId = userId, role = MemberRole.USER.name))
        supabase.from("invite_tokens").delete { filter { eq("code", code) } }

        return Result.success(token.groupId)
    }

    override suspend fun setTripDestination(groupId: String, placeId: String, name: String, lat: Double, lng: Double) {
        supabase.from("travel_groups").update({
            set("trip_destination_place_id", placeId)
            set("trip_destination_name", name)
            set("trip_destination_lat", lat)
            set("trip_destination_lng", lng)
        }) { filter { eq("id", groupId) } }
    }

    override fun observeItineraryEvents(groupId: String): Flow<List<ItineraryEvent>> = channelFlow {
        send(fetchItineraryEvents(groupId))

        val pgChannel = supabase.channel("itinerary-events-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "itinerary_events"
        }

        val bcChannel = supabase.channel("itinerary-events-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "itinerary_event_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchItineraryEvents(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun createItineraryEvent(
        groupId: String, name: String, date: String, timeOfDay: String?, description: String?, placeId: String?
    ): String {
        val memberId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val id = UUID.randomUUID().toString()
        supabase.from("itinerary_events")
            .insert(InsertItineraryEventDto(
                id = id, groupId = groupId, name = name, date = date,
                timeOfDay = timeOfDay, description = description, placeId = placeId,
                createdByMemberId = memberId,
            ))
        val bcChannel = supabase.channel("itinerary-events-broadcast-$groupId")
        runCatching {
            bcChannel.subscribe(blockUntilSubscribed = true)
            bcChannel.broadcast(event = "itinerary_event_changed", message = buildJsonObject {})
        }
        runCatching { supabase.realtime.removeChannel(bcChannel) }
        return id
    }

    override suspend fun updateItineraryEvent(
        eventId: String, name: String, date: String, timeOfDay: String?, description: String?
    ) {
        supabase.from("itinerary_events").update({
            set("name", name)
            set("date", date)
            set<String?>("time_of_day", timeOfDay)
            set<String?>("description", description)
        }) { filter { eq("id", eventId) } }
    }

    override suspend fun deleteItineraryEvent(eventId: String) {
        supabase.from("itinerary_events").delete {
            filter { eq("id", eventId) }
        }
    }

    override fun observeActivePoll(groupId: String): Flow<Poll?> = channelFlow {
        send(fetchActivePoll(groupId))

        val pgChannel = supabase.channel("group-polls-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_polls"
        }
        val bcChannel = supabase.channel("group-polls-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "poll_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchActivePoll(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun createPoll(groupId: String, type: PollType, expiresAt: String?): String {
        val id = UUID.randomUUID().toString()
        supabase.from("group_polls")
            .insert(InsertPollDto(id = id, groupId = groupId, type = type.name, expiresAt = expiresAt))
        return id
    }

    override suspend fun addPollCandidate(pollId: String, placeId: String, name: String, photoUrl: String): String {
        val memberId = supabase.auth.currentUserOrNull()?.id ?: error("Not authenticated")
        val id = UUID.randomUUID().toString()
        supabase.from("poll_candidates")
            .insert(InsertPollCandidateDto(id = id, pollId = pollId, placeId = placeId,
                name = name, photoUrl = photoUrl, addedByMemberId = memberId))
        return id
    }

    override suspend fun toggleVote(candidateId: String, memberId: String) {
        val existing = supabase.from("poll_votes")
            .select { filter { eq("candidate_id", candidateId); eq("member_id", memberId) } }
            .decodeList<PollVoteDto>()
        if (existing.isNotEmpty()) {
            supabase.from("poll_votes").delete {
                filter { eq("candidate_id", candidateId); eq("member_id", memberId) }
            }
        } else {
            supabase.from("poll_votes")
                .insert(PollVoteDto(candidateId = candidateId, memberId = memberId))
        }
    }

    override suspend fun closePoll(pollId: String) {
        supabase.from("group_polls").update({ set("state", "CLOSED") }) {
            filter { eq("id", pollId) }
        }
    }

    override suspend fun setPollWinner(pollId: String, placeId: String) {
        supabase.from("group_polls").update({ set("winner_place_id", placeId) }) {
            filter { eq("id", pollId) }
        }
    }

    override fun observePollCandidates(pollId: String): Flow<List<PollCandidate>> = channelFlow {
        val memberId = supabase.auth.currentUserOrNull()?.id
        send(fetchPollCandidatesWithVotes(pollId, memberId))

        val pgChannel = supabase.channel("poll-candidates-$pollId-${UUID.randomUUID()}")
        val candidateChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "poll_candidates"
        }
        val voteChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "poll_votes"
        }

        val bcChannel = supabase.channel("poll-candidates-broadcast-$pollId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "poll_candidate_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(candidateChanges.map { Unit }, voteChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchPollCandidatesWithVotes(pollId, memberId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    private suspend fun fetchExpenseGroups(groupId: String): List<ExpenseGroup> {
        val withPayer = runCatching {
            supabase.from("expense_groups")
                .select(Columns.raw("id, group_id, name, state, category, created_at, pinned_at, paid_by_member_id, expense_items(total_price_cents)")) {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<ExpenseGroupDto>()
                .map { it.toDomain() }
        }
        if (withPayer.isSuccess) return withPayer.getOrThrow()
        val topError = withPayer.exceptionOrNull()!!
        if (!isMissingExpenseGroupsPaidByMemberIdColumnError(topError.message)) {
            // Not a missing paid_by_member_id error — fall through to existing category/pinned_at chain
        }
        // Fallback: existing chain without paid_by_member_id (DTO default = null)
        return runCatching {
            supabase.from("expense_groups")
                .select(Columns.raw("id, group_id, name, state, category, created_at, pinned_at, expense_items(total_price_cents)")) {
                    filter { eq("group_id", groupId) }
                }
                .decodeList<ExpenseGroupDto>()
                .map { it.toDomain() }
            }
            .getOrElse { error ->
                when {
                    isMissingExpenseGroupsPinnedAtColumnError(error.message) -> {
                        runCatching {
                            supabase.from("expense_groups")
                                .select(Columns.raw("id, group_id, name, state, category, created_at, expense_items(total_price_cents)")) {
                                    filter { eq("group_id", groupId) }
                                }
                                .decodeList<ExpenseGroupDto>()
                                .map { it.toDomain() }
                        }.getOrElse { fallbackError ->
                            if (!isMissingExpenseGroupsCategoryColumnError(fallbackError.message)) throw fallbackError
                            supabase.from("expense_groups")
                                .select(Columns.raw("id, group_id, name, state, created_at, expense_items(total_price_cents)")) {
                                    filter { eq("group_id", groupId) }
                                }
                                .decodeList<ExpenseGroupDto>()
                                .map { it.toDomain() }
                        }
                    }
                    isMissingExpenseGroupsCategoryColumnError(error.message) -> {
                        runCatching {
                            supabase.from("expense_groups")
                                .select(Columns.raw("id, group_id, name, state, created_at, pinned_at, expense_items(total_price_cents)")) {
                                    filter { eq("group_id", groupId) }
                                }
                                .decodeList<ExpenseGroupDto>()
                                .map { it.toDomain() }
                        }.getOrElse { fallbackError ->
                            if (!isMissingExpenseGroupsPinnedAtColumnError(fallbackError.message)) throw fallbackError
                            supabase.from("expense_groups")
                                .select(Columns.raw("id, group_id, name, state, created_at, expense_items(total_price_cents)")) {
                                    filter { eq("group_id", groupId) }
                                }
                                .decodeList<ExpenseGroupDto>()
                                .map { it.toDomain() }
                        }
                    }
                    else -> throw error
                }
            }
    }

    override fun observeExpenseGroups(groupId: String): Flow<List<ExpenseGroup>> = channelFlow {
        send(fetchExpenseGroups(groupId))

        val pgChannel = supabase.channel("expense-groups-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_groups"
        }

        // Broadcast fallback: RLS blocks Postgres Changes for cross-user INSERTs
        val bcChannel = supabase.channel("expense-groups-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "expense_group_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchExpenseGroups(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun createExpenseGroup(groupId: String, name: String, category: String?): String {
        val id = UUID.randomUUID().toString()
        runCatching {
            supabase.from("expense_groups")
                .insert(InsertExpenseGroupDto(id = id, groupId = groupId, name = name, category = category))
        }.getOrElse { error ->
            if (!isMissingExpenseGroupsCategoryColumnError(error.message)) throw error
            supabase.from("expense_groups")
                .insert(InsertExpenseGroupDto(id = id, groupId = groupId, name = name))
        }
        val bcChannel = supabase.channel("expense-groups-broadcast-$groupId")
        runCatching {
            bcChannel.subscribe(blockUntilSubscribed = true)
            bcChannel.broadcast(event = "expense_group_changed", message = buildJsonObject {})
        }
        runCatching { supabase.realtime.removeChannel(bcChannel) }
        return id
    }

    override suspend fun updateExpenseGroupName(expenseGroupId: String, name: String) {
        supabase.from("expense_groups").update({ set("name", name) }) {
            filter { eq("id", expenseGroupId) }
        }
    }

    override suspend fun deleteExpenseGroup(expenseGroupId: String) {
        val deleted = supabase.from("expense_groups").delete {
            filter { eq("id", expenseGroupId) }
            select(Columns.list("id"))
        }.decodeList<DeletedIdDto>()
        if (deleted.isEmpty()) throw Exception("No se pudo eliminar el grupo de gastos")
    }

    override suspend fun finalizeExpenseGroup(expenseGroupId: String) {
        supabase.from("expense_groups").update({ set("state", "finalized") }) {
            filter { eq("id", expenseGroupId) }
        }
    }

    override suspend fun setExpenseGroupPinned(expenseGroupId: String, pinned: Boolean) {
        supabase.from("expense_groups").update({
            if (pinned) {
                set("pinned_at", Instant.now().atOffset(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))
            } else {
                set<String?>("pinned_at", null)
            }
        }) {
            filter { eq("id", expenseGroupId) }
        }
    }

    override suspend fun setExpenseGroupPayer(expenseGroupId: String, memberId: String) {
        supabase.from("expense_groups").update({ set("paid_by_member_id", memberId) }) {
            filter { eq("id", expenseGroupId) }
        }
    }

    override fun observeExpenseItems(expenseGroupId: String): Flow<List<ExpenseItem>> = channelFlow {
        send(fetchExpenseItems(expenseGroupId))

        val pgChannel = supabase.channel("expense-items-$expenseGroupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_items"
        }

        // Broadcast fallback: RLS blocks Postgres Changes for cross-user INSERTs
        val bcChannel = supabase.channel("expense-items-broadcast-$expenseGroupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "expense_item_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchExpenseItems(expenseGroupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override fun observeAssignments(expenseGroupId: String): Flow<List<ItemAssignment>> = channelFlow {
        send(fetchAssignments(expenseGroupId))

        // item_assignments has no expense_group_id column; re-fetch via expense items on every change.
        val pgChannel = supabase.channel("assignments-$expenseGroupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "item_assignments"
        }

        // Broadcast fallback: RLS blocks Postgres Changes for cross-user INSERTs
        val bcChannel = supabase.channel("assignments-broadcast-$expenseGroupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "assignment_changed")

        coroutineScope {
            launch { pgChannel.subscribe(blockUntilSubscribed = true) }
            launch { bcChannel.subscribe(blockUntilSubscribed = false) }
        }

        try {
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchAssignments(expenseGroupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun addExpenseItem(expenseGroupId: String, name: String, totalPriceCents: Long, quantity: Int): String {
        val travelGroupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        val dto = supabase.from("expense_items")
            .insert(InsertExpenseItemDto(groupId = travelGroupId, expenseGroupId = expenseGroupId, name = name, totalPriceCents = totalPriceCents, quantity = quantity)) {
                select()
            }
            .decodeSingle<ExpenseItemDto>()
        val bcChannel = supabase.channel("expense-items-broadcast-$expenseGroupId")
        runCatching {
            bcChannel.subscribe(blockUntilSubscribed = true)
            bcChannel.broadcast(event = "expense_item_changed", message = buildJsonObject {})
        }
        runCatching { supabase.realtime.removeChannel(bcChannel) }
        return dto.id
    }

    override suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome {
        val item = supabase.from("expense_items")
            .select { filter { eq("id", itemId) } }
            .decodeList<ExpenseItemDto>()
            .firstOrNull()?.toDomain()
            ?: throw IllegalStateException("Expense item $itemId not found")

        val currentAssignments = fetchAssignmentsForItem(itemId)
        val outcome = assignmentPolicy.validate(item, currentAssignments, memberId, quantity)
        if (outcome is AssignmentOutcome.Rejected) return outcome

        if (quantity == 0) {
            supabase.from("item_assignments").delete {
                filter {
                    eq("item_id", itemId)
                    eq("member_id", memberId)
                }
            }
        } else {
            supabase.from("item_assignments")
                .upsert(UpsertAssignmentDto(itemId = itemId, memberId = memberId, groupId = item.groupId, quantity = quantity)) {
                    onConflict = "item_id,member_id"
                }
        }
        val expenseGroupId = item.expenseGroupId
        if (expenseGroupId.isNotEmpty()) {
            val bcChannel = supabase.channel("assignments-broadcast-$expenseGroupId")
            runCatching {
                bcChannel.subscribe(blockUntilSubscribed = true)
                bcChannel.broadcast(event = "assignment_changed", message = buildJsonObject {})
            }
            runCatching { supabase.realtime.removeChannel(bcChannel) }
        }
        return AssignmentOutcome.Accepted
    }

    override suspend fun deleteExpenseItem(itemId: String) {
        supabase.from("expense_items").delete {
            filter { eq("id", itemId) }
        }
    }

    override suspend fun calculateSettlement(expenseGroupId: String): SettlementResult {
        val travelGroupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        val members = fetchMembers(travelGroupId)
        val items = fetchExpenseItems(expenseGroupId)
        val assignments = fetchAssignmentsByItemIds(items.map { it.id })
        return settlementCalculator.calculate(members = members, items = items, assignments = assignments)
    }

    // ─── Profile ─────────────────────────────────────────────────────────────

    @Serializable
    private data class MpAliasDto(@SerialName("mp_alias") val mpAlias: String? = null)

    override suspend fun getMpAlias(userId: String): String? =
        supabase.from("profiles")
            .select(Columns.list("mp_alias")) { filter { eq("id", userId) } }
            .decodeSingleOrNull<MpAliasDto>()
            ?.mpAlias

    override suspend fun updateMpAlias(alias: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.from("profiles").update({ set("mp_alias", alias.ifEmpty { null }) }) {
            filter { eq("id", userId) }
        }
    }

    // ─── Payment status ───────────────────────────────────────────────────────

    @Serializable
    private data class PaymentStatusDto(
        @SerialName("from_member_id") val fromMemberId: String,
        @SerialName("to_member_id") val toMemberId: String,
        @SerialName("expense_group_id") val expenseGroupId: String,
        @SerialName("debtor_confirmed") val debtorConfirmed: Boolean,
        @SerialName("creditor_confirmed") val creditorConfirmed: Boolean,
    ) {
        fun toDomain() = PaymentStatus(
            fromMemberId = fromMemberId,
            toMemberId = toMemberId,
            expenseGroupId = expenseGroupId,
            debtorConfirmed = debtorConfirmed,
            creditorConfirmed = creditorConfirmed,
        )
    }

    @Serializable
    private data class InsertPaymentStatusDto(
        @SerialName("from_member_id") val fromMemberId: String,
        @SerialName("to_member_id") val toMemberId: String,
        @SerialName("expense_group_id") val expenseGroupId: String,
        @SerialName("debtor_confirmed") val debtorConfirmed: Boolean = false,
        @SerialName("creditor_confirmed") val creditorConfirmed: Boolean = false,
    )

    override suspend fun getPaymentStatus(fromMemberId: String, toMemberId: String, expenseGroupId: String): PaymentStatus? =
        supabase.from("peer_to_peer_payment_status")
            .select {
                filter {
                    eq("from_member_id", fromMemberId)
                    eq("to_member_id", toMemberId)
                    eq("expense_group_id", expenseGroupId)
                }
            }
            .decodeSingleOrNull<PaymentStatusDto>()
            ?.toDomain()

    override suspend fun markDebtorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) {
        supabase.from("peer_to_peer_payment_status")
            .upsert(InsertPaymentStatusDto(fromMemberId, toMemberId, expenseGroupId)) {
                ignoreDuplicates = true
            }
        supabase.from("peer_to_peer_payment_status").update({ set("debtor_confirmed", true) }) {
            filter {
                eq("from_member_id", fromMemberId)
                eq("to_member_id", toMemberId)
                eq("expense_group_id", expenseGroupId)
            }
        }
    }

    override suspend fun markCreditorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) {
        supabase.from("peer_to_peer_payment_status")
            .upsert(InsertPaymentStatusDto(fromMemberId, toMemberId, expenseGroupId)) {
                ignoreDuplicates = true
            }
        supabase.from("peer_to_peer_payment_status").update({ set("creditor_confirmed", true) }) {
            filter {
                eq("from_member_id", fromMemberId)
                eq("to_member_id", toMemberId)
                eq("expense_group_id", expenseGroupId)
            }
        }
    }
}
