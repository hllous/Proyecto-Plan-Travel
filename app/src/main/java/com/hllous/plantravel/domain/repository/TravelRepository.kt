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
    fun observeMembers(groupId: Long): Flow<List<GroupMember>>
    fun observeInvites(groupId: Long): Flow<List<InviteToken>>

    suspend fun createGroup(groupName: String, adminName: String): Long
    suspend fun updateGroupName(groupId: Long, name: String)
    suspend fun deleteMember(memberId: Long)
    suspend fun deleteGroup(groupId: Long)
    suspend fun generateInvite(groupId: Long): InviteToken
    suspend fun deleteInvite(code: String)
    suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<Long>

    suspend fun getRegions(): List<String>
    suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation>

    fun observeExpenseItems(groupId: Long): Flow<List<ExpenseItem>>
    fun observeAssignments(groupId: Long): Flow<List<ItemAssignment>>
    suspend fun addExpenseItem(groupId: Long, itemName: String, totalPriceCents: Long, quantity: Int): Long
    suspend fun assignItemToMember(itemId: Long, memberId: Long, quantity: Int): AssignmentOutcome
    suspend fun deleteExpenseItem(itemId: Long)
    suspend fun calculateSettlement(groupId: Long): SettlementResult
}

