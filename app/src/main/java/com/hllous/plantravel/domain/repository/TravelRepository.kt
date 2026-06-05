package com.hllous.plantravel.domain.repository

import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.PaymentStatus
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollType
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

    suspend fun setTripDestination(groupId: String, placeId: String, name: String, lat: Double, lng: Double)

    fun observeItineraryEvents(groupId: String): Flow<List<ItineraryEvent>>
    suspend fun createItineraryEvent(groupId: String, name: String, date: String, timeOfDay: String?, description: String?, placeId: String?): String
    suspend fun updateItineraryEvent(eventId: String, name: String, date: String, timeOfDay: String?, description: String?)
    suspend fun deleteItineraryEvent(eventId: String)

    fun observeActivePoll(groupId: String): Flow<Poll?>
    suspend fun createPoll(groupId: String, type: PollType, expiresAt: String?): String
    suspend fun addPollCandidate(pollId: String, placeId: String, name: String, photoUrl: String): String
    suspend fun toggleVote(candidateId: String, memberId: String)
    suspend fun closePoll(pollId: String)
    suspend fun setPollWinner(pollId: String, placeId: String)
    fun observePollCandidates(pollId: String): Flow<List<PollCandidate>>

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
