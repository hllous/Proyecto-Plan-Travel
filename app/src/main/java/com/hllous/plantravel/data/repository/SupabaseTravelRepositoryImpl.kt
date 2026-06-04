package com.hllous.plantravel.data.repository

import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PaymentStatus
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.channel
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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Singleton
class SupabaseTravelRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TravelRepository {

    // ─── DTOs ───────────────────────────────────────────────────────────────

    @Serializable
    private data class TravelGroupDto(
        val id: String,
        val name: String,
        @SerialName("admin_user_id") val adminUserId: String? = null
    ) {
        fun toDomain() = TravelGroup(id = id, name = name, adminUserId = adminUserId)
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
        @SerialName("expense_items") val expenseItems: List<ExpenseItemSumDto> = emptyList()
    ) {
        fun toDomain() = ExpenseGroup(
            id = id,
            groupId = groupId,
            name = name,
            state = if (state == "finalized") ExpenseGroupState.Finalized else ExpenseGroupState.Open,
            totalPriceCents = expenseItems.sumOf { it.totalPriceCents }
        )
    }

    @Serializable
    private data class InsertExpenseGroupDto(
        val id: String,
        @SerialName("group_id") val groupId: String,
        val name: String
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

    // ─── Fetch helpers ───────────────────────────────────────────────────────

    private val settlementCalculator = ExpenseSettlementCalculator()
    private val assignmentPolicy = ExpenseAssignmentPolicy()

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

        val channel = supabase.channel("members-$groupId-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "group_members"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            changes.collect { send(fetchMembers(groupId)) }
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

    override suspend fun getRegions(): List<String> =
        throw NotImplementedError("getRegions not yet implemented — see #12")

    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> =
        throw NotImplementedError("getRecommendationsByRegion not yet implemented — see #12")

    private suspend fun fetchExpenseGroups(groupId: String): List<ExpenseGroup> =
        supabase.from("expense_groups")
            .select(Columns.raw("id, group_id, name, state, expense_items(total_price_cents)")) {
                filter { eq("group_id", groupId) }
            }
            .decodeList<ExpenseGroupDto>()
            .map { it.toDomain() }

    override fun observeExpenseGroups(groupId: String): Flow<List<ExpenseGroup>> = channelFlow {
        send(fetchExpenseGroups(groupId))

        val channel = supabase.channel("expense-groups-$groupId-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_groups"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            changes.collect { send(fetchExpenseGroups(groupId)) }
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    override suspend fun createExpenseGroup(groupId: String, name: String): String {
        val id = UUID.randomUUID().toString()
        supabase.from("expense_groups")
            .insert(InsertExpenseGroupDto(id = id, groupId = groupId, name = name))
        return id
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

    override fun observeExpenseItems(expenseGroupId: String): Flow<List<ExpenseItem>> = channelFlow {
        send(fetchExpenseItems(expenseGroupId))

        val channel = supabase.channel("expense-items-$expenseGroupId-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "expense_items"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            changes.collect { send(fetchExpenseItems(expenseGroupId)) }
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    override fun observeAssignments(expenseGroupId: String): Flow<List<ItemAssignment>> = channelFlow {
        send(fetchAssignments(expenseGroupId))

        // item_assignments has no expense_group_id column; re-fetch via expense items on every change.
        val channel = supabase.channel("assignments-$expenseGroupId-${UUID.randomUUID()}")
        val changes = channel.postgresChangeFlow<PostgresAction>(schema = "public") {
            table = "item_assignments"
        }
        channel.subscribe(blockUntilSubscribed = true)

        try {
            changes.collect { send(fetchAssignments(expenseGroupId)) }
        } finally {
            supabase.realtime.removeChannel(channel)
        }
    }

    override suspend fun addExpenseItem(expenseGroupId: String, name: String, totalPriceCents: Long, quantity: Int): String {
        val travelGroupId = fetchExpenseGroupTravelGroupId(expenseGroupId)
        val dto = supabase.from("expense_items")
            .insert(InsertExpenseItemDto(groupId = travelGroupId, expenseGroupId = expenseGroupId, name = name, totalPriceCents = totalPriceCents, quantity = quantity)) {
                select()
            }
            .decodeSingle<ExpenseItemDto>()
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
