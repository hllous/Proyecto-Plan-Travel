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
    var consumeInviteResult: Result<String> = Result.success("fake-member-id")
) : TravelRepository {

    var lastConsumeUserId: String? = null

    override fun observeGroups(): Flow<List<TravelGroup>> = flowOf(emptyList())
    override fun observeMembers(groupId: String): Flow<List<GroupMember>> = flowOf(emptyList())
    override fun observeInvites(groupId: String): Flow<List<InviteToken>> = flowOf(emptyList())
    override fun observeExpenseItems(groupId: String): Flow<List<ExpenseItem>> = flowOf(emptyList())
    override fun observeAssignments(groupId: String): Flow<List<ItemAssignment>> = flowOf(emptyList())

    override suspend fun createGroup(groupName: String, adminName: String): String = "fake-group-id"
    override suspend fun updateGroupName(groupId: String, name: String) = Unit
    override suspend fun deleteMember(memberId: String) = Unit
    override suspend fun deleteGroup(groupId: String) = Unit
    override suspend fun generateInvite(groupId: String): InviteToken = error("not needed in tests")
    override suspend fun deleteInvite(code: String) = Unit
    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String> {
        lastConsumeUserId = userId
        return consumeInviteResult
    }

    override suspend fun getRegions(): List<String> = emptyList()
    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> = emptyList()

    override suspend fun addExpenseItem(groupId: String, itemName: String, totalPriceCents: Long, quantity: Int): String = "fake-item-id"
    override suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome = assignOutcome
    override suspend fun deleteExpenseItem(itemId: String) = Unit
    override suspend fun calculateSettlement(groupId: String): SettlementResult = SettlementResult(emptyList(), emptyList())
}
