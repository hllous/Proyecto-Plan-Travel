package com.hllous.plantravel.data.repository

import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
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
        val profiles: ProfileDto
    ) {
        fun toDomain() = GroupMember(
            id = id,
            groupId = groupId,
            name = profiles.displayName,
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
        val name: String,
        @SerialName("admin_user_id") val adminUserId: String
    )

    @Serializable
    private data class InsertMemberDto(
        @SerialName("group_id") val groupId: String,
        @SerialName("user_id") val userId: String,
        val role: String
    )

    // ─── Fetch helpers ───────────────────────────────────────────────────────

    private suspend fun fetchGroupsForUser(userId: String): List<TravelGroup> {
        val memberships = supabase.from("group_members")
            .select(Columns.list("group_id")) {
                filter { eq("user_id", userId) }
            }
            .decodeList<GroupMembershipDto>()
        val groupIds = memberships.map { it.groupId }
        if (groupIds.isEmpty()) return emptyList()
        return supabase.from("travel_groups")
            .select {
                filter { isIn("id", groupIds) }
            }
            .decodeList<TravelGroupDto>()
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

        val channel = supabase.channel("groups-for-$userId")
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

        val channel = supabase.channel("members-$groupId")
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

    override suspend fun createGroup(groupName: String, adminName: String): String {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("User must be authenticated to create a group")
        val group = supabase.from("travel_groups")
            .insert(InsertGroupDto(name = groupName, adminUserId = userId)) {
                select()
            }
            .decodeSingle<TravelGroupDto>()
        supabase.from("group_members")
            .insert(InsertMemberDto(groupId = group.id, userId = userId, role = MemberRole.ADMIN.name))
        return group.id
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

    override suspend fun deleteGroup(groupId: String) {
        // FK cascade on group_id → travel_groups.id handles members, invites, expense_items, assignments
        supabase.from("travel_groups").delete {
            filter { eq("id", groupId) }
        }
    }

    // ─── Stubs (implemented in subsequent slices) ─────────────────────────────

    override fun observeInvites(groupId: String): Flow<List<InviteToken>> =
        flow { throw NotImplementedError("observeInvites not yet implemented — see #22") }

    override suspend fun generateInvite(groupId: String): InviteToken =
        throw NotImplementedError("generateInvite not yet implemented — see #22")

    override suspend fun deleteInvite(code: String) =
        throw NotImplementedError("deleteInvite not yet implemented — see #22")

    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String> =
        throw NotImplementedError("consumeInvite not yet implemented — see #22")

    override suspend fun getRegions(): List<String> =
        throw NotImplementedError("getRegions not yet implemented — see #12")

    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> =
        throw NotImplementedError("getRecommendationsByRegion not yet implemented — see #12")

    override fun observeExpenseItems(groupId: String): Flow<List<ExpenseItem>> =
        flow { throw NotImplementedError("observeExpenseItems not yet implemented — see #23") }

    override fun observeAssignments(groupId: String): Flow<List<ItemAssignment>> =
        flow { throw NotImplementedError("observeAssignments not yet implemented — see #23") }

    override suspend fun addExpenseItem(groupId: String, itemName: String, totalPriceCents: Long, quantity: Int): String =
        throw NotImplementedError("addExpenseItem not yet implemented — see #23")

    override suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome =
        throw NotImplementedError("assignItemToMember not yet implemented — see #23")

    override suspend fun deleteExpenseItem(itemId: String) =
        throw NotImplementedError("deleteExpenseItem not yet implemented — see #23")

    override suspend fun calculateSettlement(groupId: String): SettlementResult =
        throw NotImplementedError("calculateSettlement not yet implemented — see #23")
}
