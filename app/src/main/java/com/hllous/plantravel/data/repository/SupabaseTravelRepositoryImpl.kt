package com.hllous.plantravel.data.repository

import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.model.DestinationDraft
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
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
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
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.shareIn
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
        val status: String = "active",
    ) {
        fun toDomain() = TravelGroup(
            id = id,
            name = name,
            adminUserId = adminUserId,
            tripDestinationPlaceId = tripDestinationPlaceId,
            tripDestinationName = tripDestinationName,
            tripDestinationLat = tripDestinationLat,
            tripDestinationLng = tripDestinationLng,
            isActive = status == "active",
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
    private data class ExpenseItemGroupRefDto(
        @SerialName("expense_group_id") val expenseGroupId: String? = null
    )

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
        @SerialName("end_date") val endDate: String? = null,
    ) {
        fun toDomain() = ItineraryEvent(id, groupId, name, date, timeOfDay, description, placeId, createdByMemberId, endDate)
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
        @SerialName("end_date") val endDate: String? = null,
    )

    @Serializable
    private data class PollDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val type: String,
        val state: String,
        val name: String = "",
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("winner_place_id") val winnerPlaceId: String? = null,
        @SerialName("winner_photo_url") val winnerPhotoUrl: String? = null,
        @SerialName("thumbnail_photo_url") val thumbnailPhotoUrl: String? = null,
        @SerialName("created_at") val createdAt: String? = null,
    ) {
        fun toDomain() = Poll(
            id = id, groupId = groupId,
            type = if (type.equals("destination", ignoreCase = true)) PollType.DESTINATION else PollType.ACTIVITY,
            state = if (state.equals("closed", ignoreCase = true)) PollState.CLOSED else PollState.OPEN,
            name = name,
            expiresAt = expiresAt,
            winnerPlaceId = winnerPlaceId,
            winnerPhotoUrl = winnerPhotoUrl,
            thumbnailPhotoUrl = thumbnailPhotoUrl,
        )
    }

    @Serializable
    private data class InsertPollDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val type: String,
        val name: String = "",
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
        val lat: Double = 0.0,
        val lng: Double = 0.0,
    ) {
        fun toDomain() = PollCandidate(id = id, pollId = pollId, placeId = placeId,
            name = name, photoUrl = photoUrl, addedByMemberId = addedByMemberId,
            lat = lat, lng = lng)
    }

    @Serializable
    private data class InsertPollCandidateDto(
        val id: String,
        @SerialName("poll_id") val pollId: String,
        @SerialName("place_id") val placeId: String,
        val name: String,
        @SerialName("photo_url") val photoUrl: String,
        @SerialName("added_by_member_id") val addedByMemberId: String,
        val lat: Double = 0.0,
        val lng: Double = 0.0,
    )

    @Serializable
    private data class PollVoteDto(
        @SerialName("candidate_id") val candidateId: String,
        @SerialName("member_id") val memberId: String,
    )

    @Serializable
    private data class MemberIdDto(val id: String)

    @Serializable
    private data class StoredDestinationDto(
        val id: String,
        val source: String,
        @SerialName("source_id") val sourceId: String,
        val name: String,
        @SerialName("normalized_name") val normalizedName: String,
        val province: String,
        val region: String,
        @SerialName("country_code") val countryCode: String,
        val lat: Double,
        val lng: Double,
        val population: Int,
        @SerialName("google_place_id") val googlePlaceId: String? = null,
        @SerialName("google_photo_url") val googlePhotoUrl: String? = null,
        @SerialName("wikipedia_title") val wikipediaTitle: String? = null,
        @SerialName("wikipedia_photo_url") val wikipediaPhotoUrl: String? = null,
        @SerialName("display_photo_url") val displayPhotoUrl: String? = null,
        @SerialName("is_active") val isActive: Boolean = true,
    ) {
        fun toDomain() = StoredDestination(
            id = id,
            source = source,
            sourceId = sourceId,
            name = name,
            normalizedName = normalizedName,
            province = province,
            region = region,
            countryCode = countryCode,
            lat = lat,
            lng = lng,
            population = population,
            googlePlaceId = googlePlaceId,
            googlePhotoUrl = googlePhotoUrl,
            wikipediaTitle = wikipediaTitle,
            wikipediaPhotoUrl = wikipediaPhotoUrl,
            displayPhotoUrl = displayPhotoUrl,
            isActive = isActive,
        )
    }

    @Serializable
    private data class UpsertStoredDestinationDto(
        val source: String,
        @SerialName("source_id") val sourceId: String,
        val name: String,
        @SerialName("normalized_name") val normalizedName: String,
        val province: String,
        val region: String,
        @SerialName("country_code") val countryCode: String,
        val lat: Double,
        val lng: Double,
        val population: Int,
        @SerialName("google_place_id") val googlePlaceId: String? = null,
        @SerialName("google_photo_url") val googlePhotoUrl: String? = null,
        @SerialName("wikipedia_title") val wikipediaTitle: String? = null,
        @SerialName("wikipedia_photo_url") val wikipediaPhotoUrl: String? = null,
        @SerialName("display_photo_url") val displayPhotoUrl: String? = null,
        @SerialName("is_active") val isActive: Boolean = true,
    )

    // ─── Fetch helpers ───────────────────────────────────────────────────────

    private val repositoryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    // Incrementing this restarts observeGroupsSharedFlow via flatMapLatest, rebuilding
    // per-group broadcast channels for any groups joined mid-session.
    private val _observeGroupsVersion = MutableStateFlow(0)

    private val pollBroadcastFlows = ConcurrentHashMap<String, SharedFlow<Unit>>()

    private fun pollBroadcastFlow(groupId: String): SharedFlow<Unit> =
        pollBroadcastFlows.getOrPut(groupId) {
            channelFlow {
                val bcChannel = supabase.channel("group-polls-broadcast-$groupId")
                val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "poll_changed")
                runCatching { bcChannel.subscribe(blockUntilSubscribed = true) }
                try {
                    broadcasts.collect {
                        send(Unit)
                    }
                } finally {
                    supabase.realtime.removeChannel(bcChannel)
                }
            }.shareIn(repositoryScope, SharingStarted.Eagerly, replay = 0)
        }

    private val settlementCalculator = ExpenseSettlementCalculator()
    private val assignmentPolicy = ExpenseAssignmentPolicy()

    private suspend fun fetchItineraryEvents(groupId: String): List<ItineraryEvent> =
        supabase.from("itinerary_events")
            .select { filter { eq("group_id", groupId) } }
            .decodeList<ItineraryEventDto>()
            .map { it.toDomain() }

    private suspend fun fetchAllPolls(groupId: String): List<Poll> =
        supabase.from("group_polls")
            .select {
                filter { eq("group_id", groupId) }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PollDto>()
            .map { it.toDomain() }

    override suspend fun fetchActivePoll(groupId: String): Poll? =
        supabase.from("group_polls")
            .select {
                filter {
                    eq("group_id", groupId)
                    eq("type", "destination")
                }
                order("created_at", Order.DESCENDING)
                limit(1)
            }
            .decodeList<PollDto>()
            .firstOrNull()
            ?.toDomain()

    private suspend fun fetchActiveActivityPolls(groupId: String): List<Poll> =
        supabase.from("group_polls")
            .select {
                filter {
                    eq("group_id", groupId)
                    eq("state", "open")
                    eq("type", "activity")
                }
                order("created_at", Order.DESCENDING)
            }
            .decodeList<PollDto>()
            .map { it.toDomain() }

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

    private fun createObserveMembersChannelFlow(groupId: String): Flow<List<GroupMember>> = channelFlow {
        send(fetchMembers(groupId))

        // Postgres Changes: detects local inserts/updates/deletes (same-user actions work reliably)
        val pgChannel = supabase.channel("members-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }

        // Broadcast: fallback for cross-user inserts that Postgres Changes RLS may block
        val bcChannel = supabase.channel("members-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "member_joined")

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                // The broadcast observer is the fallback for local mutations when Postgres Changes
                // do not echo back to the sender. Wait until it is attached before relying on it.
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(pgChanges.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchMembers(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    private fun createObserveGroupsChannelFlow(): Flow<List<TravelGroup>> = channelFlow {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@channelFlow

        val initialGroups = fetchGroupsForUser(userId)
        send(initialGroups)

        val pgChannel = supabase.channel("groups-for-$userId-${UUID.randomUUID()}")
        val memberChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }
        val groupChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "travel_groups"
        }

        // Per-user broadcast: catches the joiner's own group list update after consumeInvite.
        val bcChannel = supabase.channel("groups-broadcast-$userId")
        val userBroadcasts = bcChannel.broadcastFlow<JsonObject>(event = "groups_changed")

        // Per-group broadcasts: catches rename/delete/kick from other members for each group
        // the user is already in. New groups after joining are covered by the per-user channel.
        val bcGroupEntries = initialGroups.map { group ->
            val bcChannel = supabase.channel("groups-broadcast-${group.id}")
            bcChannel to bcChannel.broadcastFlow<JsonObject>(event = "group_list_changed")
        }

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
                bcGroupEntries.forEach { (bcChannel, _) ->
                    launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
                }
            }
            val allFlows: List<Flow<Unit>> = buildList {
                add(memberChanges.map { Unit })
                add(groupChanges.map { Unit })
                add(userBroadcasts.map { Unit })
                bcGroupEntries.forEach { (_, flow) -> add(flow.map { Unit }) }
            }
            var prevMemberCounts = initialGroups.associate { it.id to it.memberCount }
            merge(*allFlows.toTypedArray()).collect {
                val newGroups = fetchGroupsForUser(userId)
                send(newGroups)
                newGroups.forEach { group ->
                    val prev = prevMemberCounts[group.id]
                    if (prev != null && prev != group.memberCount) notifyMembersChanged(group.id)
                }
                prevMemberCounts = newGroups.associate { it.id to it.memberCount }
            }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
            bcGroupEntries.forEach { (bcChannel, _) -> supabase.realtime.removeChannel(bcChannel) }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val observeGroupsSharedFlow: SharedFlow<List<TravelGroup>> =
        _observeGroupsVersion
            .flatMapLatest { createObserveGroupsChannelFlow() }
            .shareIn(repositoryScope, SharingStarted.WhileSubscribed(5000), replay = 1)

    override fun observeGroups(): Flow<List<TravelGroup>> = observeGroupsSharedFlow

    private val observeMembersSharedFlows = ConcurrentHashMap<String, SharedFlow<List<GroupMember>>>()
    private val observeMembersVersions = ConcurrentHashMap<String, MutableStateFlow<Int>>()

    private fun membersVersion(groupId: String): MutableStateFlow<Int> =
        observeMembersVersions.getOrPut(groupId) { MutableStateFlow(0) }

    // Restart the observeMembers channelFlow for the given group, forcing an immediate DB
    // re-fetch. Called after any mutation that changes group membership (join, leave, kick)
    // so the local device never relies solely on broadcast delivery.
    private fun notifyMembersChanged(groupId: String) {
        membersVersion(groupId).value++
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun observeMembers(groupId: String): Flow<List<GroupMember>> =
        observeMembersSharedFlows.getOrPut(groupId) {
            membersVersion(groupId)
                .flatMapLatest { createObserveMembersChannelFlow(groupId) }
                .shareIn(repositoryScope, SharingStarted.WhileSubscribed(5000), replay = 1)
        }

    override suspend fun broadcastMemberJoined(groupId: String) {
        sendBroadcast("members-broadcast-$groupId", "member_joined")
    }

    override suspend fun broadcastDisplayNameChanged() {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        val groupIds = runCatching {
            supabase.from("group_members")
                .select(Columns.list("group_id")) { filter { eq("user_id", userId) } }
                .decodeList<GroupMembershipDto>()
                .map { it.groupId }
        }.getOrDefault(emptyList())
        groupIds.forEach { broadcastMemberJoined(it) }
    }

    private suspend fun sendBroadcast(channelName: String, event: String) {
        // supabase.channel() in v3 always creates a NEW RealtimeChannelImpl — there is NO
        // deduplication at the factory level. Calling subscribe() on the new object writes it
        // into _subscriptions[topic], silently evicting the live observer channel that has
        // broadcastFlow listeners attached. Subsequent server-side broadcasts are then routed to
        // the new (listener-free) channel and the observer never fires.
        //
        // The fix: never call subscribe(). RealtimeChannelImpl.broadcast() already has a built-in
        // fallback: when status != SUBSCRIBED it sends via HTTP REST (POST to /realtime/v1/api/broadcast).
        // The REST path delivers to ALL WebSocket subscribers on the server side, including the
        // sender's own observer (bypassing the "no self-broadcast" restriction), and leaves
        // _subscriptions untouched.
        val channel = supabase.channel(channelName)
        runCatching { channel.broadcast(event = event, message = buildJsonObject {}) }
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
        // Postgres Changes for INSERT is unreliable here: the group_members row doesn't
        // exist yet when travel_groups fires, so fetchGroupsForUser misses it; and the
        // group_members INSERT RLS check is self-referential and may not deliver.
        // Restart the flow immediately so the new group is fetched from DB.
        _observeGroupsVersion.value++
        return groupId
    }

    override suspend fun updateGroupName(groupId: String, name: String) {
        supabase.from("travel_groups").update({ set("name", name) }) {
            filter { eq("id", groupId) }
        }
        _observeGroupsVersion.value++
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
    }

    override suspend fun deleteMember(memberId: String) {
        val member = runCatching {
            supabase.from("group_members")
                .select(Columns.list("group_id")) { filter { eq("id", memberId) } }
                .decodeList<MembershipCheckDto>()
                .firstOrNull()
        }.getOrNull()
        supabase.from("group_members").delete {
            filter { eq("id", memberId) }
        }
        member?.groupId?.let {
            notifyMembersChanged(it)
            sendBroadcast("groups-broadcast-$it", "group_list_changed")
            sendBroadcast("members-broadcast-$it", "member_joined")
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
        // Postgres Changes for this DELETE is blocked by RLS (the user's own membership row
        // is gone by the time the realtime event is evaluated). Force an immediate re-fetch
        // on this device via _observeGroupsVersion, and broadcast for other devices.
        _observeGroupsVersion.value++
        notifyMembersChanged(groupId)
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
        sendBroadcast("members-broadcast-$groupId", "member_joined")
    }

    override suspend fun deleteGroup(groupId: String) {
        // FK cascade on group_id → travel_groups.id handles members, invites, expense_items, assignments
        supabase.from("travel_groups").delete {
            filter { eq("id", groupId) }
        }
        // Postgres Changes for travel_groups DELETE is RLS-blocked: the admin's group_members
        // row is cascade-deleted in the same transaction, so realtime can't verify access.
        // Increment _observeGroupsVersion to immediately restart the groups flow on this device
        // (same pattern as consumeInvite). Broadcast notifies other members' devices.
        _observeGroupsVersion.value++
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
    }

    // ─── Stubs (implemented in subsequent slices) ─────────────────────────────

    override fun observeInvites(groupId: String): Flow<List<InviteToken>> = channelFlow {
        send(fetchInvites(groupId))

        val pgChannel = supabase.channel("invites-$groupId-${UUID.randomUUID()}")
        val changes = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "invite_tokens"
        }

        // Broadcast fallback: RLS may block Postgres Changes for cross-user invite operations
        val bcChannel = supabase.channel("invites-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "invite_changed")

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(changes.map { Unit }, broadcasts.map { Unit })
                .collect { send(fetchInvites(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
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
        sendBroadcast("invites-broadcast-$groupId", "invite_changed")
        return dto.toDomain()
    }

    override suspend fun deleteInvite(code: String) {
        // Fetch the groupId before deleting so we can broadcast to the correct channel.
        val groupId = runCatching {
            supabase.from("invite_tokens")
                .select(Columns.list("group_id")) { filter { eq("code", code) } }
                .decodeList<GroupMembershipDto>()
                .firstOrNull()?.groupId
        }.getOrNull()
        supabase.from("invite_tokens").delete {
            filter { eq("code", code) }
        }
        groupId?.let { sendBroadcast("invites-broadcast-$it", "invite_changed") }
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

        // Trigger the joiner's own observeGroups broadcast fallback so their group list updates
        // without relying on the Postgres Change self-echo (which RLS may block).
        sendBroadcast("groups-broadcast-$userId", "groups_changed")
        sendBroadcast("groups-broadcast-${token.groupId}", "group_list_changed")
        sendBroadcast("members-broadcast-${token.groupId}", "member_joined")
        notifyMembersChanged(token.groupId)
        _observeGroupsVersion.value++

        return Result.success(token.groupId)
    }

    override suspend fun setTripDestination(groupId: String, placeId: String, name: String, lat: Double, lng: Double) {
        supabase.from("travel_groups").update({
            set("trip_destination_place_id", placeId)
            set("trip_destination_name", name)
            set("trip_destination_lat", lat)
            set("trip_destination_lng", lng)
        }) { filter { eq("id", groupId) } }
        _observeGroupsVersion.value++
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
    }

    override suspend fun endTrip(groupId: String) {
        supabase.from("travel_groups").update({ set("status", "closed") }) { filter { eq("id", groupId) } }
        _observeGroupsVersion.value++
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
    }

    override suspend fun reactivateTrip(groupId: String) {
        supabase.from("travel_groups").update({ set("status", "active") }) { filter { eq("id", groupId) } }
        _observeGroupsVersion.value++
        sendBroadcast("groups-broadcast-$groupId", "group_list_changed")
    }

    override fun observeItineraryEvents(groupId: String): Flow<List<ItineraryEvent>> = channelFlow {
        send(fetchItineraryEvents(groupId))

        val pgChannel = supabase.channel("itinerary-events-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "itinerary_events"
        }

        val bcChannel = supabase.channel("itinerary-events-broadcast-$groupId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "itinerary_event_changed")

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(
                pgChanges.map { Unit },
                broadcasts.map { Unit },
            ).collect { send(fetchItineraryEvents(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
            supabase.realtime.removeChannel(bcChannel)
        }
    }

    override suspend fun createItineraryEvent(
        groupId: String, name: String, date: String, timeOfDay: String?, description: String?, placeId: String?, endDate: String?
    ): String {
        val memberId = currentMemberIdForGroup(groupId) ?: error("Member not found for group $groupId")
        val id = UUID.randomUUID().toString()
        supabase.from("itinerary_events")
            .insert(InsertItineraryEventDto(
                id = id, groupId = groupId, name = name, date = date,
                timeOfDay = timeOfDay, description = description, placeId = placeId,
                createdByMemberId = memberId, endDate = endDate,
            ))
        sendBroadcast("itinerary-events-broadcast-$groupId", "itinerary_event_changed")
        return id
    }

    override suspend fun updateItineraryEvent(
        eventId: String, name: String, date: String, timeOfDay: String?, description: String?, endDate: String?
    ) {
        val groupId = supabase.from("itinerary_events").update({
            set("name", name)
            set("date", date)
            set<String?>("time_of_day", timeOfDay)
            set<String?>("description", description)
            set<String?>("end_date", endDate)
        }) {
            filter { eq("id", eventId) }
            select(Columns.list("group_id"))
        }.decodeList<ExpenseGroupRefDto>().firstOrNull()?.groupId ?: return
        sendBroadcast("itinerary-events-broadcast-$groupId", "itinerary_event_changed")
    }

    override suspend fun deleteItineraryEvent(eventId: String) {
        val groupId = supabase.from("itinerary_events").delete {
            filter { eq("id", eventId) }
            select(Columns.list("group_id"))
        }.decodeList<ExpenseGroupRefDto>().firstOrNull()?.groupId ?: return
        sendBroadcast("itinerary-events-broadcast-$groupId", "itinerary_event_changed")
    }

    override fun observeActivePoll(groupId: String): Flow<Poll?> = channelFlow {
        send(fetchActivePoll(groupId))

        val pgChannel = supabase.channel("group-polls-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_polls"
        }

        try {
            // runCatching prevents a subscription failure from propagating through
            // coroutineScope → channelFlow → PollViewModel's .catch { emit(null) },
            // which would null-out the poll that was already emitted by fetchActivePoll above.
            runCatching { pgChannel.subscribe(blockUntilSubscribed = true) }
            merge(
                pgChanges.map { Unit },
                pollBroadcastFlow(groupId),
            ).collect { send(fetchActivePoll(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
        }
    }

    override fun observeAllPolls(groupId: String): Flow<List<Poll>> = channelFlow {
        send(fetchAllPolls(groupId))

        val pgChannel = supabase.channel("group-polls-all-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_polls"
        }

        try {
            runCatching { pgChannel.subscribe(blockUntilSubscribed = true) }
            merge(
                pgChanges.map { Unit },
                pollBroadcastFlow(groupId),
            ).collect { send(fetchAllPolls(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
        }
    }

    override fun observeActiveActivityPolls(groupId: String): Flow<List<Poll>> = channelFlow {
        send(fetchActiveActivityPolls(groupId))

        val pgChannel = supabase.channel("group-polls-activity-$groupId-${UUID.randomUUID()}")
        val pgChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_polls"
        }

        try {
            runCatching { pgChannel.subscribe(blockUntilSubscribed = true) }
            merge(
                pgChanges.map { Unit },
                pollBroadcastFlow(groupId),
            ).collect { send(fetchActiveActivityPolls(groupId)) }
        } finally {
            supabase.realtime.removeChannel(pgChannel)
        }
    }

    override suspend fun createPoll(groupId: String, type: PollType, name: String, expiresAt: String?): String {
        val id = UUID.randomUUID().toString()
        supabase.from("group_polls")
            .insert(InsertPollDto(id = id, groupId = groupId, type = type.name.lowercase(), name = name, expiresAt = expiresAt))
        sendBroadcast("group-polls-broadcast-$groupId", "poll_changed")
        return id
    }

    override suspend fun renamePoll(pollId: String, name: String) {
        val groupId = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.groupId ?: return
        supabase.from("group_polls").update({ set("name", name) }) {
            filter { eq("id", pollId) }
        }
        sendBroadcast("group-polls-broadcast-$groupId", "poll_changed")
    }

    // Resolves the group_members.id for the current auth user in a given group.
    // Several tables (poll_candidates, poll_votes, itinerary_events) reference group_members(id),
    // NOT auth.users.id. Always call this helper before inserting into those tables.
    private suspend fun currentMemberIdForGroup(groupId: String): String? {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return null
        return supabase.from("group_members")
            .select(Columns.list("id")) { filter { eq("group_id", groupId); eq("user_id", userId) } }
            .decodeList<MemberIdDto>().firstOrNull()?.id
    }

    private suspend fun currentMemberIdForPoll(pollId: String): String? {
        val groupId = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.groupId ?: return null
        return currentMemberIdForGroup(groupId)
    }

    override suspend fun addPollCandidate(pollId: String, placeId: String, name: String, photoUrl: String, lat: Double, lng: Double): String {
        val memberId = currentMemberIdForPoll(pollId) ?: error("Member not found for poll $pollId")
        val id = UUID.randomUUID().toString()
        supabase.from("poll_candidates")
            .insert(InsertPollCandidateDto(id = id, pollId = pollId, placeId = placeId,
                name = name, photoUrl = photoUrl, addedByMemberId = memberId, lat = lat, lng = lng))
        // Store thumbnail on the poll from the first candidate added (for Level 1 card preview)
        val hasThumbnail = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.thumbnailPhotoUrl != null
        if (!hasThumbnail) {
            supabase.from("group_polls").update({ set("thumbnail_photo_url", photoUrl) }) {
                filter { eq("id", pollId) }
            }
        }
        sendBroadcast("poll-candidates-broadcast-$pollId", "poll_candidate_changed")
        return id
    }

    override suspend fun toggleVote(candidateId: String, memberId: String, pollId: String) {
        val inserted = runCatching {
            supabase.from("poll_votes")
                .insert(PollVoteDto(candidateId = candidateId, memberId = memberId)) { select() }
                .decodeList<PollVoteDto>()
        }
        if (inserted.isFailure || inserted.getOrNull()?.isEmpty() == true) {
            supabase.from("poll_votes").delete {
                filter { eq("candidate_id", candidateId); eq("member_id", memberId) }
            }
        }
        sendBroadcast("poll-candidates-broadcast-$pollId", "poll_candidate_changed")
    }

    override suspend fun closePoll(pollId: String) {
        val groupId = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.groupId ?: return
        supabase.from("group_polls").update({ set("state", "closed") }) {
            filter { eq("id", pollId) }
        }
        sendBroadcast("group-polls-broadcast-$groupId", "poll_changed")
    }

    override suspend fun setPollWinner(pollId: String, placeId: String, photoUrl: String?) {
        val groupId = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.groupId ?: return
        supabase.from("group_polls").update({
            set("winner_place_id", placeId)
            set("winner_photo_url", photoUrl)
        }) { filter { eq("id", pollId) } }
        sendBroadcast("group-polls-broadcast-$groupId", "poll_changed")
    }

    override suspend fun deletePoll(pollId: String) {
        val groupId = supabase.from("group_polls")
            .select { filter { eq("id", pollId) } }
            .decodeList<PollDto>().firstOrNull()?.groupId ?: return
        supabase.from("group_polls").delete {
            filter { eq("id", pollId) }
        }
        sendBroadcast("group-polls-broadcast-$groupId", "poll_changed")
    }

    override fun observePollCandidates(pollId: String): Flow<List<PollCandidate>> = channelFlow {
        // poll_votes.member_id references group_members(id), not auth.users.id.
        // Resolved lazily: if null at startup (poll not yet committed), retry on each event.
        var currentMemberId = currentMemberIdForPoll(pollId)
        send(fetchPollCandidatesWithVotes(pollId, currentMemberId))

        val pgChannel = supabase.channel("poll-candidates-$pollId-${UUID.randomUUID()}")
        val candidateChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "poll_candidates"
        }
        val voteChanges = pgChannel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "poll_votes"
        }

        val bcChannel = supabase.channel("poll-candidates-broadcast-$pollId")
        val broadcasts = bcChannel.broadcastFlow<JsonObject>(event = "poll_candidate_changed")

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(
                candidateChanges.map { Unit },
                voteChanges.map { Unit },
                broadcasts.map { Unit },
            ).collect {
                    if (currentMemberId == null) currentMemberId = currentMemberIdForPoll(pollId)
                    send(fetchPollCandidatesWithVotes(pollId, currentMemberId))
                }
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

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(
                pgChanges.map { Unit },
                broadcasts.map { Unit },
            ).collect { send(fetchExpenseGroups(groupId)) }
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
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
        return id
    }

    override suspend fun updateExpenseGroupName(expenseGroupId: String, name: String) {
        val groupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        supabase.from("expense_groups").update({ set("name", name) }) {
            filter { eq("id", expenseGroupId) }
        }
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
    }

    override suspend fun deleteExpenseGroup(expenseGroupId: String) {
        val groupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        val deleted = supabase.from("expense_groups").delete {
            filter { eq("id", expenseGroupId) }
            select(Columns.list("id"))
        }.decodeList<DeletedIdDto>()
        if (deleted.isEmpty()) throw Exception("No se pudo eliminar el grupo de gastos")
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
    }

    override suspend fun finalizeExpenseGroup(expenseGroupId: String) {
        supabase.from("expense_groups").update({ set("state", "finalized") }) {
            filter { eq("id", expenseGroupId) }
        }
        val groupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
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
        val groupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
    }

    override suspend fun setExpenseGroupPayer(expenseGroupId: String, memberId: String?) {
        supabase.from("expense_groups").update({
            set("paid_by_member_id", memberId)
        }) {
            filter { eq("id", expenseGroupId) }
        }
        val groupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        sendBroadcast("expense-groups-broadcast-$groupId", "expense_group_changed")
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

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(
                pgChanges.map { Unit },
                broadcasts.map { Unit },
            ).collect { send(fetchExpenseItems(expenseGroupId)) }
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

        try {
            coroutineScope {
                launch { runCatching { pgChannel.subscribe(blockUntilSubscribed = true) } }
                launch { runCatching { bcChannel.subscribe(blockUntilSubscribed = true) } }
            }
            merge(
                pgChanges.map { Unit },
                broadcasts.map { Unit },
            ).collect { send(fetchAssignments(expenseGroupId)) }
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
        sendBroadcast("expense-items-broadcast-$expenseGroupId", "expense_item_changed")
        sendBroadcast("expense-groups-broadcast-$travelGroupId", "expense_group_changed")
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
            sendBroadcast("assignments-broadcast-$expenseGroupId", "assignment_changed")
            sendBroadcast("expense-groups-broadcast-${item.groupId}", "expense_group_changed")
        }
        return AssignmentOutcome.Accepted
    }

    override suspend fun deleteExpenseItem(itemId: String) {
        val expenseGroupId = supabase.from("expense_items").delete {
            filter { eq("id", itemId) }
            select(Columns.list("expense_group_id"))
        }.decodeList<ExpenseItemGroupRefDto>().firstOrNull()?.expenseGroupId ?: return
        sendBroadcast("expense-items-broadcast-$expenseGroupId", "expense_item_changed")
        val travelGroupId = runCatching { fetchExpenseGroupTravelGroupId(expenseGroupId) }.getOrNull() ?: return
        sendBroadcast("expense-groups-broadcast-$travelGroupId", "expense_group_changed")
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

    private suspend fun fetchGroupIdsForCurrentUser(): List<String> {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return emptyList()
        return supabase.from("group_members")
            .select(Columns.list("group_id")) { filter { eq("user_id", userId) } }
            .decodeList<GroupMembershipDto>()
            .map { it.groupId }
    }

    override suspend fun updateMpAlias(alias: String) {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return
        supabase.from("profiles").update({ set("mp_alias", alias.ifEmpty { null }) }) {
            filter { eq("id", userId) }
        }
        // Notify group members so their observeMembers fires and expense settlement view
        // refreshes with the updated alias.
        fetchGroupIdsForCurrentUser().forEach { groupId ->
            sendBroadcast("members-broadcast-$groupId", "member_joined")
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
        val travelGroupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        sendBroadcast("expense-groups-broadcast-$travelGroupId", "expense_group_changed")
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
        val travelGroupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        sendBroadcast("expense-groups-broadcast-$travelGroupId", "expense_group_changed")
    }

    override suspend fun browseDestinations(region: String): List<StoredDestination> =
        supabase.from("destinations")
            .select {
                filter {
                    eq("region", region)
                    eq("is_active", true)
                }
            }
            .decodeList<StoredDestinationDto>()
            .map { it.toDomain() }
            .sortedWith(compareByDescending<StoredDestination> { it.population }.thenBy { it.normalizedName })

    override suspend fun searchDestinations(query: String): List<StoredDestination> {
        val normalizedQuery = DestinationTextNormalizer.normalize(query)
        return supabase.from("destinations")
            .select {
                filter { eq("is_active", true) }
            }
            .decodeList<StoredDestinationDto>()
            .map { it.toDomain() }
            .filter {
                it.normalizedName.contains(normalizedQuery) ||
                    DestinationTextNormalizer.normalize(it.province).contains(normalizedQuery)
            }
            .sortedWith(compareByDescending<StoredDestination> { it.population }.thenBy { it.normalizedName })
    }

    override suspend fun upsertDestination(destination: DestinationDraft): StoredDestination =
        supabase.from("destinations")
            .upsert(
                UpsertStoredDestinationDto(
                    source = destination.source,
                    sourceId = destination.sourceId,
                    name = destination.name,
                    normalizedName = DestinationTextNormalizer.normalize(destination.name),
                    province = destination.province,
                    region = destination.region,
                    countryCode = destination.countryCode,
                    lat = destination.lat,
                    lng = destination.lng,
                    population = destination.population,
                    googlePlaceId = destination.googlePlaceId,
                    googlePhotoUrl = destination.googlePhotoUrl,
                    wikipediaTitle = destination.wikipediaTitle,
                    wikipediaPhotoUrl = destination.wikipediaPhotoUrl,
                    displayPhotoUrl = destination.displayPhotoUrl,
                    isActive = destination.isActive,
                ),
            ) {
                onConflict = "source,source_id"
                select()
            }
            .decodeSingle<StoredDestinationDto>()
            .toDomain()

    override suspend fun updateDestinationPhoto(
        destinationId: String,
        googlePhotoUrl: String?,
        wikipediaTitle: String?,
        wikipediaPhotoUrl: String?,
        displayPhotoUrl: String?,
    ): StoredDestination =
        supabase.from("destinations")
            .update({
                if (googlePhotoUrl != null) set("google_photo_url", googlePhotoUrl)
                if (wikipediaTitle != null) set("wikipedia_title", wikipediaTitle)
                if (wikipediaPhotoUrl != null) set("wikipedia_photo_url", wikipediaPhotoUrl)
                if (displayPhotoUrl != null) set("display_photo_url", displayPhotoUrl)
            }) {
                filter { eq("id", destinationId) }
                select()
            }
            .decodeSingle<StoredDestinationDto>()
            .toDomain()
}
