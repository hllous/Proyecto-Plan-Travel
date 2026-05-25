package com.hllous.plantravel.data.repository

import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import io.github.jan.supabase.SupabaseClient
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class SupabaseTravelRepositoryImpl @Inject constructor(
    private val supabase: SupabaseClient
) : TravelRepository {

    override fun observeGroups(): Flow<List<TravelGroup>> =
        throw NotImplementedError("observeGroups not yet implemented — see #21")

    override fun observeMembers(groupId: Long): Flow<List<GroupMember>> =
        throw NotImplementedError("observeMembers not yet implemented — see #21")

    override fun observeInvites(groupId: Long): Flow<List<InviteToken>> =
        throw NotImplementedError("observeInvites not yet implemented — see #22")

    override suspend fun createGroup(groupName: String, adminName: String): Long =
        throw NotImplementedError("createGroup not yet implemented — see #21")

    override suspend fun updateGroupName(groupId: Long, name: String) =
        throw NotImplementedError("updateGroupName not yet implemented — see #21")

    override suspend fun deleteMember(memberId: Long) =
        throw NotImplementedError("deleteMember not yet implemented — see #21")

    override suspend fun deleteGroup(groupId: Long) =
        throw NotImplementedError("deleteGroup not yet implemented — see #21")

    override suspend fun generateInvite(groupId: Long): InviteToken =
        throw NotImplementedError("generateInvite not yet implemented — see #22")

    override suspend fun deleteInvite(code: String) =
        throw NotImplementedError("deleteInvite not yet implemented — see #22")

    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<Long> =
        throw NotImplementedError("consumeInvite not yet implemented — see #22")

    override suspend fun getRegions(): List<String> =
        throw NotImplementedError("getRegions not yet implemented — see #12")

    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> =
        throw NotImplementedError("getRecommendationsByRegion not yet implemented — see #12")

    override fun observeExpenseItems(groupId: Long): Flow<List<ExpenseItem>> =
        throw NotImplementedError("observeExpenseItems not yet implemented — see #23")

    override fun observeAssignments(groupId: Long): Flow<List<ItemAssignment>> =
        throw NotImplementedError("observeAssignments not yet implemented — see #23")

    override suspend fun addExpenseItem(groupId: Long, itemName: String, totalPriceCents: Long, quantity: Int): Long =
        throw NotImplementedError("addExpenseItem not yet implemented — see #23")

    override suspend fun assignItemToMember(itemId: Long, memberId: Long, quantity: Int): AssignmentOutcome =
        throw NotImplementedError("assignItemToMember not yet implemented — see #23")

    override suspend fun deleteExpenseItem(itemId: Long) =
        throw NotImplementedError("deleteExpenseItem not yet implemented — see #23")

    override suspend fun calculateSettlement(groupId: Long): SettlementResult =
        throw NotImplementedError("calculateSettlement not yet implemented — see #23")
}
