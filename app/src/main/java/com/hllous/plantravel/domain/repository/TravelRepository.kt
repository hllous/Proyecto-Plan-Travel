package com.hllous.plantravel.domain.repository

import com.hllous.plantravel.domain.model.DestinationRecommendation
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.PaymentStatus
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
    suspend fun leaveGroup(groupId: String)
    suspend fun deleteGroup(groupId: String)
    suspend fun generateInvite(groupId: String): InviteToken
    suspend fun deleteInvite(code: String)
    suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String>
    suspend fun broadcastMemberJoined(groupId: String)

    suspend fun getMpAlias(userId: String): String?
    suspend fun updateMpAlias(alias: String)

    suspend fun getPaymentStatus(fromMemberId: String, toMemberId: String, expenseGroupId: String): PaymentStatus?
    suspend fun markDebtorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String)
    suspend fun markCreditorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String)

    suspend fun getRegions(): List<String>
    suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation>

    fun observeExpenseGroups(groupId: String): Flow<List<ExpenseGroup>>
    suspend fun createExpenseGroup(groupId: String, name: String, category: String? = null): String
    suspend fun updateExpenseGroupName(expenseGroupId: String, name: String)
    suspend fun deleteExpenseGroup(expenseGroupId: String)
    suspend fun finalizeExpenseGroup(expenseGroupId: String)
    suspend fun setExpenseGroupPinned(expenseGroupId: String, pinned: Boolean)
    suspend fun setExpenseGroupPayer(expenseGroupId: String, memberId: String)

    fun observeExpenseItems(expenseGroupId: String): Flow<List<ExpenseItem>>
    fun observeAssignments(expenseGroupId: String): Flow<List<ItemAssignment>>
    suspend fun addExpenseItem(expenseGroupId: String, name: String, totalPriceCents: Long, quantity: Int): String
    suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome
    suspend fun deleteExpenseItem(itemId: String)
    suspend fun calculateSettlement(expenseGroupId: String): SettlementResult
}
