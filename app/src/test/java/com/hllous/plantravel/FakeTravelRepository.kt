package com.hllous.plantravel

import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.repository.TravelRepository
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

class FakeTravelRepository(
    var assignOutcome: AssignmentOutcome = AssignmentOutcome.Accepted,
    var consumeInviteResult: Result<Long> = Result.success(0L)
) : TravelRepository {

    var lastConsumeUserId: String? = null

    override fun observeGroups(): Flow<List<TravelGroup>> = flowOf(emptyList())
    override fun observeMembers(groupId: Long): Flow<List<GroupMember>> = flowOf(emptyList())
    override fun observeInvites(groupId: Long): Flow<List<InviteToken>> = flowOf(emptyList())
    override fun observeExpenseItems(groupId: Long): Flow<List<ExpenseItem>> = flowOf(emptyList())
    override fun observeAssignments(groupId: Long): Flow<List<ItemAssignment>> = flowOf(emptyList())

    override suspend fun createGroup(groupName: String, adminName: String): Long = 0L
    override suspend fun updateGroupName(groupId: Long, name: String) = Unit
    override suspend fun deleteMember(memberId: Long) = Unit
    override suspend fun deleteGroup(groupId: Long) = Unit
    override suspend fun generateInvite(groupId: Long): InviteToken = error("not needed in tests")
    override suspend fun deleteInvite(code: String) = Unit
    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<Long> {
        lastConsumeUserId = userId
        return consumeInviteResult
    }

    override suspend fun getRegions(): List<String> = emptyList()
    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> = emptyList()

    override suspend fun addExpenseItem(groupId: Long, itemName: String, totalPriceCents: Long, quantity: Int): Long = 0L
    override suspend fun assignItemToMember(itemId: Long, memberId: Long, quantity: Int): AssignmentOutcome = assignOutcome
    override suspend fun deleteExpenseItem(itemId: Long) = Unit
    override suspend fun calculateSettlement(groupId: Long): SettlementResult = SettlementResult(emptyList(), emptyList())
}
