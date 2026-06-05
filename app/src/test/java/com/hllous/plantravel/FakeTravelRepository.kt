package com.hllous.plantravel

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
    var leaveGroupThrows: Boolean = false,
    var addExpenseItemThrows: Boolean = false,
    var deleteExpenseItemThrows: Boolean = false,
    var calculateSettlementThrows: Boolean = false,
    var assignItemThrows: Boolean = false,
    var createExpenseGroupThrows: Boolean = false,
    var updateExpenseGroupNameThrows: Boolean = false,
    var deleteExpenseGroupThrows: Boolean = false,
    var finalizeExpenseGroupThrows: Boolean = false,
    var setExpenseGroupPinnedThrows: Boolean = false,
    var setExpenseGroupPayerThrows: Boolean = false,
    var mpAliasByUserId: Map<String, String?> = emptyMap(),
    var updateMpAliasThrows: Boolean = false,
    var settlementResultsByExpenseGroupId: Map<String, SettlementResult> = emptyMap(),
    initialGroups: List<TravelGroup> = emptyList(),
    initialMembers: Map<String, List<GroupMember>> = emptyMap(),
    initialExpenseItems: Map<String, List<ExpenseItem>> = emptyMap(),
    initialExpenseGroups: Map<String, List<ExpenseGroup>> = emptyMap(),
    val customObserveGroups: (() -> Flow<List<TravelGroup>>)? = null,
    val customObserveMembers: ((String) -> Flow<List<GroupMember>>)? = null,
    val customObserveInvites: ((String) -> Flow<List<InviteToken>>)? = null,
    val customObserveExpenseGroups: ((String) -> Flow<List<ExpenseGroup>>)? = null,
) : TravelRepository {

    private val _groups = MutableStateFlow(initialGroups)
    private val _membersByGroup = MutableStateFlow(initialMembers)
    private val _itemsByGroup = MutableStateFlow(initialExpenseItems)
    private val _expenseGroupsByGroup = MutableStateFlow(initialExpenseGroups)
    private val _assignmentsByGroup = MutableStateFlow<Map<String, List<ItemAssignment>>>(emptyMap())

    var lastConsumeUserId: String? = null
    var calculateSettlementCallCount = 0
    var addExpenseItemCallCount = 0
    var deleteExpenseItemCallCount = 0
    var assignItemCallCount = 0
    var createExpenseGroupCallCount = 0
    var updateExpenseGroupNameCallCount = 0
    var finalizeExpenseGroupCallCount = 0
    var setExpenseGroupPinnedCallCount = 0
    var setExpenseGroupPayerCallCount = 0
    var lastCreatedExpenseGroupCategory: String? = null

    override fun observeGroups(): Flow<List<TravelGroup>> = customObserveGroups?.invoke() ?: _groups
    override fun observeMembers(groupId: String): Flow<List<GroupMember>> =
        customObserveMembers?.invoke(groupId) ?: _membersByGroup.map { it[groupId] ?: emptyList() }
    override fun observeInvites(groupId: String): Flow<List<InviteToken>> =
        customObserveInvites?.invoke(groupId) ?: flowOf(emptyList())
    override fun observeExpenseItems(expenseGroupId: String): Flow<List<ExpenseItem>> =
        _itemsByGroup.map { it[expenseGroupId] ?: emptyList() }
    override fun observeAssignments(expenseGroupId: String): Flow<List<ItemAssignment>> =
        _assignmentsByGroup.map { it[expenseGroupId] ?: emptyList() }

    override suspend fun createGroup(groupName: String): String {
        if (createGroupThrows) throw RuntimeException("network error")
        val newGroup = TravelGroup(id = "fake-group-id", name = groupName)
        _groups.value = _groups.value + newGroup
        return newGroup.id
    }

    override suspend fun updateGroupName(groupId: String, name: String) = Unit
    override suspend fun deleteMember(memberId: String) = Unit
    override suspend fun leaveGroup(groupId: String) {
        if (leaveGroupThrows) throw RuntimeException("network error")
    }
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

    override suspend fun broadcastMemberJoined(groupId: String) = Unit

    override fun observeExpenseGroups(groupId: String): Flow<List<ExpenseGroup>> =
        customObserveExpenseGroups?.invoke(groupId) ?: _expenseGroupsByGroup.map { it[groupId] ?: emptyList() }

    override suspend fun createExpenseGroup(groupId: String, name: String, category: String?): String {
        if (createExpenseGroupThrows) throw RuntimeException("network error")
        createExpenseGroupCallCount++
        lastCreatedExpenseGroupCategory = category
        val group = ExpenseGroup(
            id = "fake-expense-group-id",
            groupId = groupId,
            name = name,
            state = ExpenseGroupState.Open,
            totalPriceCents = 0,
            category = category,
        )
        val current = _expenseGroupsByGroup.value.toMutableMap()
        current[groupId] = (current[groupId] ?: emptyList()) + group
        _expenseGroupsByGroup.value = current
        return group.id
    }

    override suspend fun updateExpenseGroupName(expenseGroupId: String, name: String) {
        if (updateExpenseGroupNameThrows) throw RuntimeException("network error")
        updateExpenseGroupNameCallCount++
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value.mapValues { (_, groups) ->
            groups.map { if (it.id == expenseGroupId) it.copy(name = name) else it }
        }
    }

    override suspend fun deleteExpenseGroup(expenseGroupId: String) {
        if (deleteExpenseGroupThrows) throw RuntimeException("network error")
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value
            .mapValues { (_, groups) -> groups.filter { it.id != expenseGroupId } }
    }

    override suspend fun finalizeExpenseGroup(expenseGroupId: String) {
        if (finalizeExpenseGroupThrows) throw RuntimeException("network error")
        finalizeExpenseGroupCallCount++
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value.mapValues { (_, groups) ->
            groups.map { if (it.id == expenseGroupId) it.copy(state = ExpenseGroupState.Finalized) else it }
        }
    }

    override suspend fun setExpenseGroupPinned(expenseGroupId: String, pinned: Boolean) {
        if (setExpenseGroupPinnedThrows) throw RuntimeException("network error")
        setExpenseGroupPinnedCallCount++
        val pinnedAtMillis = if (pinned) System.currentTimeMillis() else null
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value.mapValues { (_, groups) ->
            groups.map {
                if (it.id == expenseGroupId) it.copy(pinnedAtMillis = pinnedAtMillis) else it
            }
        }
    }

    override suspend fun setExpenseGroupPayer(expenseGroupId: String, memberId: String) {
        if (setExpenseGroupPayerThrows) throw RuntimeException("network error")
        setExpenseGroupPayerCallCount++
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value.mapValues { (_, groups) ->
            groups.map {
                if (it.id == expenseGroupId) it.copy(paidByMemberId = memberId) else it
            }
        }
    }

    override suspend fun getMpAlias(userId: String): String? = mpAliasByUserId[userId]

    override suspend fun updateMpAlias(alias: String) {
        if (updateMpAliasThrows) throw RuntimeException("network error")
    }

    override suspend fun getPaymentStatus(fromMemberId: String, toMemberId: String, expenseGroupId: String): PaymentStatus? = null

    override suspend fun markDebtorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) = Unit

    override suspend fun markCreditorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) = Unit

    override suspend fun getRegions(): List<String> = emptyList()
    override suspend fun getRecommendationsByRegion(region: String): List<DestinationRecommendation> = emptyList()

    fun simulateRemoteExpenseItemPush(expenseGroupId: String, items: List<ExpenseItem>) {
        _itemsByGroup.value = _itemsByGroup.value.toMutableMap().also { it[expenseGroupId] = items }
    }

    fun simulateRemoteAssignmentPush(expenseGroupId: String, assignments: List<ItemAssignment>) {
        _assignmentsByGroup.value = _assignmentsByGroup.value.toMutableMap().also { it[expenseGroupId] = assignments }
    }

    override suspend fun addExpenseItem(expenseGroupId: String, name: String, totalPriceCents: Long, quantity: Int): String {
        if (addExpenseItemThrows) throw RuntimeException("network error")
        addExpenseItemCallCount++
        val item = ExpenseItem(id = "fake-item-id", groupId = expenseGroupId, expenseGroupId = expenseGroupId, name = name, totalPriceCents = totalPriceCents, quantity = quantity)
        val current = _itemsByGroup.value.toMutableMap()
        current[expenseGroupId] = (current[expenseGroupId] ?: emptyList()) + item
        _itemsByGroup.value = current
        return item.id
    }

    override suspend fun assignItemToMember(itemId: String, memberId: String, quantity: Int): AssignmentOutcome {
        if (assignItemThrows) throw RuntimeException("network error")
        assignItemCallCount++
        return assignOutcome
    }

    override suspend fun deleteExpenseItem(itemId: String) {
        if (deleteExpenseItemThrows) throw RuntimeException("network error")
        deleteExpenseItemCallCount++
        val current = _itemsByGroup.value.toMutableMap()
        _itemsByGroup.value = current.mapValues { (_, items) -> items.filter { it.id != itemId } }
    }

    override suspend fun calculateSettlement(expenseGroupId: String): SettlementResult {
        if (calculateSettlementThrows) throw RuntimeException("network error")
        calculateSettlementCallCount++
        return settlementResultsByExpenseGroupId[expenseGroupId] ?: settlementResult
    }

    fun getExpenseGroupsSnapshot(groupId: String): List<ExpenseGroup> =
        _expenseGroupsByGroup.value[groupId] ?: emptyList()
}
