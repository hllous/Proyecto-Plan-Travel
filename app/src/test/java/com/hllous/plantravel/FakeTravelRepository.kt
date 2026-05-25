package com.hllous.plantravel

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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class FakeTravelRepository(
    var assignOutcome: AssignmentOutcome = AssignmentOutcome.Accepted,
    var consumeInviteResult: Result<String> = Result.success("fake-member-id"),
    var settlementResult: SettlementResult = SettlementResult(emptyList(), emptyList()),
    var generateInviteThrows: Boolean = false,
    var deleteInviteThrows: Boolean = false,
    var createGroupThrows: Boolean = false,
    var addExpenseItemThrows: Boolean = false,
    var deleteExpenseItemThrows: Boolean = false,
    var calculateSettlementThrows: Boolean = false,
    var assignItemThrows: Boolean = false,
    initialGroups: List<TravelGroup> = emptyList(),
    initialMembers: Map<String, List<GroupMember>> = emptyMap(),
    initialExpenseItems: Map<String, List<ExpenseItem>> = emptyMap(),
) : TravelRepository {

    private val _groups = MutableStateFlow(initialGroups)
    private val _membersByGroup = MutableStateFlow(initialMembers)
    private val _itemsByGroup = MutableStateFlow(initialExpenseItems)

    var lastConsumeUserId: String? = null
    var calculateSettlementCallCount = 0

    override fun observeGroups(): Flow<List<TravelGroup>> = _groups
    override fun observeMembers(groupId: String): Flow<List<GroupMember>> =
        _membersByGroup.map { it[groupId] ?: emptyList() }
    override fun observeInvites(groupId: String): Flow<List<InviteToken>> = flowOf(emptyList())
    override fun observeExpenseItems(groupId: String): Flow<List<ExpenseItem>> =
        _itemsByGroup.map { it[groupId] ?: emptyList() }
    override fun observeAssignments(groupId: String): Flow<List<ItemAssignment>> = flowOf(emptyList())

    override suspend fun createGroup(groupName: String): String {
        if (createGroupThrows) throw RuntimeException("network error")
        val newGroup = TravelGroup(id = "fake-group-id", name = groupName)
        _groups.value = _groups.value + newGroup
        return newGroup.id
    }

    override suspend fun updateGroupName(groupId: String, name: String) = Unit
    override suspend fun deleteMember(memberId: String) = Unit
    override suspend fun deleteGroup(groupId: String) {
        _groups.value = _groups.value.filter { it.id != groupId }
    }
    override suspend fun generateInvite(groupId: String): InviteToken {
        if (generateInviteThrows) throw RuntimeException("network error")
        return InviteToken(code = "FAKECODE", groupId = groupId, link = "plantravel://invite/FAKECODE", expiresAtMillis = Long.MAX_VALUE)
    }
    override suspend fun deleteInvite(code: String) {
        if (deleteInviteThrows) throw RuntimeException("network error")
    }
    override suspend fun consumeInvite(code: String, userId: String, displayName: String): Result<String> {
        lastConsumeUserId = userId
        if (consumeInviteResult.isSuccess) {
            val groupId = consumeInviteResult.getOrThrow()
            val newMember = GroupMember(id = "new-member-id", groupId = groupId, name = displayName, userId = userId, role = MemberRole.USER)
            val current = _membersByGroup.value.toMutableMap()
            current[groupId] = (current[groupId] ?: emptyList()) + newMember
            _membersByGroup.value = current
        }
        return consumeInviteResult
    }

    override suspend fun getRegions(): List<String> = emptyList()
    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> = emptyList()

    override suspend fun addExpenseItem(groupId: String, itemName: String, totalPriceCents: Long, quantity: Int): String {
        if (addExpenseItemThrows) throw RuntimeException("network error")
        val item = ExpenseItem(id = "fake-item-id", groupId = groupId, name = itemName, totalPriceCents = totalPriceCents, quantity = quantity)
        val current = _itemsByGroup.value.toMutableMap()
        current[groupId] = (current[groupId] ?: emptyList()) + item
        _itemsByGroup.value = current
        return item.id
    }

    override suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome {
        if (assignItemThrows) throw RuntimeException("network error")
        return assignOutcome
    }

    override suspend fun deleteExpenseItem(itemId: String) {
        if (deleteExpenseItemThrows) throw RuntimeException("network error")
        val current = _itemsByGroup.value.toMutableMap()
        _itemsByGroup.value = current.mapValues { (_, items) -> items.filter { it.id != itemId } }
    }

    override suspend fun calculateSettlement(groupId: String): SettlementResult {
        if (calculateSettlementThrows) throw RuntimeException("network error")
        calculateSettlementCallCount++
        return settlementResult
    }
}
