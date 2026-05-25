package com.hllous.plantravel.domain.repository

import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.domain.settlement.AssignmentOutcome
import kotlinx.coroutines.flow.Flow

interface TravelRepository {
    fun observeGroups(): Flow<List<TravelGroup>>
    fun observeMembers(groupId: String): Flow<List<GroupMember>>
    fun observeInvites(groupId: String): Flow<List<InviteToken>>

    suspend fun createGroup(groupName: String): String
    suspend fun updateGroupName(groupId: String, name: String)
    suspend fun deleteMember(memberId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun generateInvite(groupId: String): InviteToken
    suspend fun deleteInvite(code: String)
    suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String>

    suspend fun getRegions(): List<String>
    suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation>

    fun observeExpenseItems(groupId: String): Flow<List<ExpenseItem>>
    fun observeAssignments(groupId: String): Flow<List<ItemAssignment>>
    suspend fun addExpenseItem(groupId: String, itemName: String, totalPriceCents: Long, quantity: Int): String
    suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome
    suspend fun deleteExpenseItem(itemId: String)
    suspend fun calculateSettlement(groupId: String): SettlementResult
}
