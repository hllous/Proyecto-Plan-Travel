package com.hllous.plantravel

import com.hllous.plantravel.data.destination.DestinationTextNormalizer
import com.hllous.plantravel.domain.model.DestinationDraft
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PaymentStatus
import com.hllous.plantravel.domain.model.SettlementResult
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.domain.model.StoredDestination
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
    var paymentStatusByKey: Map<Triple<String, String, String>, PaymentStatus> = emptyMap(),
    var updateMpAliasThrows: Boolean = false,
    var settlementResultsByExpenseGroupId: Map<String, SettlementResult> = emptyMap(),
    initialGroups: List<TravelGroup> = emptyList(),
    initialMembers: Map<String, List<GroupMember>> = emptyMap(),
    initialExpenseItems: Map<String, List<ExpenseItem>> = emptyMap(),
    initialExpenseGroups: Map<String, List<ExpenseGroup>> = emptyMap(),
    initialDestinations: List<StoredDestination> = emptyList(),
    val customObserveGroups: (() -> Flow<List<TravelGroup>>)? = null,
    val customObserveMembers: ((String) -> Flow<List<GroupMember>>)? = null,
    val availableGroupsForJoin: List<TravelGroup> = emptyList(),
    val customObserveInvites: ((String) -> Flow<List<InviteToken>>)? = null,
    val customObserveExpenseGroups: ((String) -> Flow<List<ExpenseGroup>>)? = null,
    val customObserveActivePoll: ((String) -> Flow<Poll?>)? = null,
    val customObserveAllPolls: ((String) -> Flow<List<Poll>>)? = null,
    val customObserveActiveActivityPolls: ((String) -> Flow<List<Poll>>)? = null,
    val customFetchActivePoll: (suspend (String) -> Poll?)? = null,
    val customObservePollCandidates: ((String) -> Flow<List<PollCandidate>>)? = null,
) : TravelRepository {

    private val _groups = MutableStateFlow(initialGroups)
    private val _membersByGroup = MutableStateFlow(initialMembers)
    private val _invitesByGroup = MutableStateFlow<Map<String, List<InviteToken>>>(emptyMap())
    private val _itemsByGroup = MutableStateFlow(initialExpenseItems)
    private val _expenseGroupsByGroup = MutableStateFlow(initialExpenseGroups)
    private val _assignmentsByGroup = MutableStateFlow<Map<String, List<ItemAssignment>>>(emptyMap())
    private val _destinations = MutableStateFlow(initialDestinations)

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
    var lastBrowsedDestinationRegion: String? = null
    var lastDestinationSearchQuery: String? = null
    var upsertDestinationCallCount = 0
    var lastUpsertedDestination: DestinationDraft? = null
    var lastUpdatedDestinationPhotoId: String? = null

    override fun observeGroups(): Flow<List<TravelGroup>> = customObserveGroups?.invoke() ?: _groups
    override fun observeMembers(groupId: String): Flow<List<GroupMember>> =
        customObserveMembers?.invoke(groupId) ?: _membersByGroup.map { it[groupId] ?: emptyList() }
    override fun observeInvites(groupId: String): Flow<List<InviteToken>> =
        customObserveInvites?.invoke(groupId) ?: _invitesByGroup.map { it[groupId] ?: emptyList() }
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
        _groups.value = _groups.value.filter { it.id != groupId }
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
            val joinedGroup = availableGroupsForJoin.firstOrNull { it.id == groupId }
                ?: TravelGroup(id = groupId, name = "")
            if (_groups.value.none { it.id == groupId }) {
                _groups.value = _groups.value + joinedGroup
            }
        }
        return consumeInviteResult
    }

    override suspend fun broadcastMemberJoined(groupId: String) = Unit
    override suspend fun broadcastDisplayNameChanged() = Unit

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

    override suspend fun setExpenseGroupPayer(expenseGroupId: String, memberId: String?) {
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

    override suspend fun getPaymentStatus(fromMemberId: String, toMemberId: String, expenseGroupId: String): PaymentStatus? =
        paymentStatusByKey[Triple(fromMemberId, toMemberId, expenseGroupId)]

    override suspend fun markDebtorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) = Unit

    override suspend fun markCreditorConfirmed(fromMemberId: String, toMemberId: String, expenseGroupId: String) = Unit

    override suspend fun browseDestinations(region: String): List<StoredDestination> =
        _destinations.value
            .also { lastBrowsedDestinationRegion = region }
            .filter { it.isActive && it.region == region }
            .sortedWith(compareByDescending<StoredDestination> { it.population }.thenBy { it.normalizedName })

    override suspend fun searchDestinations(query: String): List<StoredDestination> {
        lastDestinationSearchQuery = query
        val normalizedQuery = DestinationTextNormalizer.normalize(query)
        return _destinations.value
            .filter {
                it.isActive && (
                    it.normalizedName.contains(normalizedQuery) ||
                        DestinationTextNormalizer.normalize(it.province).contains(normalizedQuery)
                    )
            }
            .sortedWith(compareByDescending<StoredDestination> { it.population }.thenBy { it.normalizedName })
    }

    override suspend fun upsertDestination(destination: DestinationDraft): StoredDestination {
        upsertDestinationCallCount++
        lastUpsertedDestination = destination
        val existing = _destinations.value.firstOrNull {
            it.source == destination.source && it.sourceId == destination.sourceId
        }
        val stored = StoredDestination(
            id = existing?.id ?: "dest-${_destinations.value.size + 1}",
            source = destination.source,
            sourceId = destination.sourceId,
            name = destination.name,
            normalizedName = DestinationTextNormalizer.normalize(destination.name),
            province = destination.province,
            region = destination.region,
            countryCode = destination.countryCode,
            lat = destination.lat,
            lng = destination.lng,
            population = destination.population,
            googlePlaceId = destination.googlePlaceId,
            googlePhotoUrl = destination.googlePhotoUrl,
            wikipediaTitle = destination.wikipediaTitle,
            wikipediaPhotoUrl = destination.wikipediaPhotoUrl,
            displayPhotoUrl = destination.displayPhotoUrl,
            isActive = destination.isActive,
        )
        _destinations.value = if (existing == null) {
            _destinations.value + stored
        } else {
            _destinations.value.map { if (it.id == existing.id) stored else it }
        }
        return stored
    }

    override suspend fun updateDestinationPhoto(
        destinationId: String,
        googlePhotoUrl: String?,
        wikipediaTitle: String?,
        wikipediaPhotoUrl: String?,
        displayPhotoUrl: String?,
    ): StoredDestination {
        lastUpdatedDestinationPhotoId = destinationId
        val current = _destinations.value.first { it.id == destinationId }
        val updated = current.copy(
            googlePhotoUrl = googlePhotoUrl ?: current.googlePhotoUrl,
            wikipediaTitle = wikipediaTitle ?: current.wikipediaTitle,
            wikipediaPhotoUrl = wikipediaPhotoUrl ?: current.wikipediaPhotoUrl,
            displayPhotoUrl = displayPhotoUrl ?: current.displayPhotoUrl,
        )
        _destinations.value = _destinations.value.map { if (it.id == destinationId) updated else it }
        return updated
    }

    // ─── Trip planning stubs ──────────────────────────────────────────────────

    private val _itineraryEvents = MutableStateFlow<Map<String, List<ItineraryEvent>>>(emptyMap())
    private val _pollsByGroup = MutableStateFlow<Map<String, List<Poll>>>(emptyMap())
    private val _candidatesByPoll = MutableStateFlow<Map<String, List<PollCandidate>>>(emptyMap())

    var setTripDestinationCallCount = 0
    var lastTripDestinationPlaceId: String? = null
    var setTripDestinationThrows: Boolean = false
    var createPollThrows: Boolean = false
    var addPollCandidateThrows: Boolean = false
    var addPollCandidateCallCount = 0
    var lastAddedPollCandidatePollId: String? = null
    var lastAddedPollCandidatePlaceId: String? = null
    var renamePollCallCount = 0
    var lastRenamedPollId: String? = null
    var lastRenamedPollName: String? = null

    override suspend fun setTripDestination(groupId: String, placeId: String, name: String, lat: Double, lng: Double) {
        if (setTripDestinationThrows) throw RuntimeException("network error")
        setTripDestinationCallCount++
        lastTripDestinationPlaceId = placeId
        val current = _groups.value.map {
            if (it.id == groupId) it.copy(
                tripDestinationPlaceId = placeId,
                tripDestinationName = name,
                tripDestinationLat = lat,
                tripDestinationLng = lng,
            ) else it
        }
        _groups.value = current
    }

    override suspend fun endTrip(groupId: String) {
        _groups.value = _groups.value.map { if (it.id == groupId) it.copy(isActive = false) else it }
    }

    override suspend fun reactivateTrip(groupId: String) {
        _groups.value = _groups.value.map { if (it.id == groupId) it.copy(isActive = true) else it }
    }

    override fun observeItineraryEvents(groupId: String): Flow<List<ItineraryEvent>> =
        _itineraryEvents.map { it[groupId] ?: emptyList() }

    override suspend fun createItineraryEvent(groupId: String, name: String, date: String, timeOfDay: String?, description: String?, placeId: String?, endDate: String?): String {
        val event = ItineraryEvent(
            id = "fake-event-${System.currentTimeMillis()}",
            groupId = groupId,
            name = name,
            date = date,
            timeOfDay = timeOfDay,
            description = description,
            placeId = placeId,
            createdByMemberId = "fake-member-id",
            endDate = endDate,
        )
        val current = _itineraryEvents.value.toMutableMap()
        current[groupId] = (current[groupId] ?: emptyList()) + event
        _itineraryEvents.value = current
        return event.id
    }

    override suspend fun updateItineraryEvent(eventId: String, name: String, date: String, timeOfDay: String?, description: String?, endDate: String?) {
        _itineraryEvents.value = _itineraryEvents.value.mapValues { (_, events) ->
            events.map {
                if (it.id == eventId) it.copy(name = name, date = date, timeOfDay = timeOfDay, description = description, endDate = endDate)
                else it
            }
        }
    }

    override suspend fun deleteItineraryEvent(eventId: String) {
        _itineraryEvents.value = _itineraryEvents.value.mapValues { (_, events) ->
            events.filter { it.id != eventId }
        }
    }

    override fun observeActivePoll(groupId: String): Flow<Poll?> =
        customObserveActivePoll?.invoke(groupId)
            ?: _pollsByGroup.map { map ->
                map[groupId]?.lastOrNull { it.type == PollType.DESTINATION }
            }

    override fun observeAllPolls(groupId: String): Flow<List<Poll>> =
        customObserveAllPolls?.invoke(groupId)
            ?: _pollsByGroup.map { it[groupId] ?: emptyList() }

    override fun observeActiveActivityPolls(groupId: String): Flow<List<Poll>> =
        customObserveActiveActivityPolls?.invoke(groupId)
            ?: _pollsByGroup.map { map ->
                map[groupId]?.filter { it.state == com.hllous.plantravel.domain.model.PollState.OPEN && it.type == PollType.ACTIVITY }
                    ?: emptyList()
            }

    override suspend fun fetchActivePoll(groupId: String): Poll? =
        customFetchActivePoll?.invoke(groupId)
            ?: _pollsByGroup.value[groupId]?.lastOrNull { it.type == PollType.DESTINATION }

    override suspend fun createPoll(groupId: String, type: PollType, name: String, expiresAt: String?): String {
        if (createPollThrows) throw RuntimeException("network error")
        val poll = Poll(
            id = "fake-poll-${System.currentTimeMillis()}",
            groupId = groupId,
            type = type,
            state = com.hllous.plantravel.domain.model.PollState.OPEN,
            name = name,
            expiresAt = expiresAt,
        )
        val current = _pollsByGroup.value.toMutableMap()
        current[groupId] = (current[groupId] ?: emptyList()) + poll
        _pollsByGroup.value = current
        return poll.id
    }

    override suspend fun renamePoll(pollId: String, name: String) {
        renamePollCallCount++
        lastRenamedPollId = pollId
        lastRenamedPollName = name
        _pollsByGroup.value = _pollsByGroup.value.mapValues { (_, polls) ->
            polls.map { if (it.id == pollId) it.copy(name = name) else it }
        }
    }

    override suspend fun addPollCandidate(pollId: String, placeId: String, name: String, photoUrl: String, lat: Double, lng: Double): String {
        if (addPollCandidateThrows) throw RuntimeException("network error")
        addPollCandidateCallCount++
        lastAddedPollCandidatePollId = pollId
        lastAddedPollCandidatePlaceId = placeId
        val candidate = PollCandidate(
            id = "fake-candidate-${System.currentTimeMillis()}",
            pollId = pollId,
            placeId = placeId,
            name = name,
            photoUrl = photoUrl,
            addedByMemberId = "fake-member-id",
            lat = lat,
            lng = lng,
        )
        val current = _candidatesByPoll.value.toMutableMap()
        current[pollId] = (current[pollId] ?: emptyList()) + candidate
        _candidatesByPoll.value = current
        return candidate.id
    }

    override suspend fun toggleVote(candidateId: String, memberId: String, pollId: String) {
        _candidatesByPoll.value = _candidatesByPoll.value.mapValues { (_, candidates) ->
            candidates.map { c ->
                if (c.id != candidateId) c
                else if (c.votedByCurrentMember) c.copy(voteCount = c.voteCount - 1, votedByCurrentMember = false)
                else c.copy(voteCount = c.voteCount + 1, votedByCurrentMember = true)
            }
        }
    }

    override suspend fun closePoll(pollId: String) {
        _pollsByGroup.value = _pollsByGroup.value.mapValues { (_, polls) ->
            polls.map { if (it.id == pollId) it.copy(state = com.hllous.plantravel.domain.model.PollState.CLOSED) else it }
        }
    }

    override suspend fun setPollWinner(pollId: String, placeId: String, photoUrl: String?) {
        _pollsByGroup.value = _pollsByGroup.value.mapValues { (_, polls) ->
            polls.map { if (it.id == pollId) it.copy(winnerPlaceId = placeId, winnerPhotoUrl = photoUrl) else it }
        }
    }

    override suspend fun deletePoll(pollId: String) {
        _pollsByGroup.value = _pollsByGroup.value.mapValues { (_, polls) ->
            polls.filter { it.id != pollId }
        }
    }

    override fun observePollCandidates(pollId: String): Flow<List<PollCandidate>> =
        customObservePollCandidates?.invoke(pollId) ?: _candidatesByPoll.map { it[pollId] ?: emptyList() }

    fun simulateRemoteGroupsPush(groups: List<TravelGroup>) {
        _groups.value = groups
    }

    fun simulateRemoteInvitesPush(groupId: String, invites: List<InviteToken>) {
        _invitesByGroup.value = _invitesByGroup.value.toMutableMap().also { it[groupId] = invites }
    }

    fun simulateRemoteMemberJoin(groupId: String, member: GroupMember) {
        val current = _membersByGroup.value.toMutableMap()
        current[groupId] = (current[groupId] ?: emptyList()) + member
        _membersByGroup.value = current
    }

    fun simulateItineraryEventPush(groupId: String, events: List<ItineraryEvent>) {
        _itineraryEvents.value = _itineraryEvents.value.toMutableMap().also { it[groupId] = events }
    }

    fun getPollsForGroup(groupId: String): List<Poll> = _pollsByGroup.value[groupId] ?: emptyList()

    fun simulatePollUpdate(groupId: String, poll: Poll?) {
        val current = _pollsByGroup.value.toMutableMap()
        val existing = current[groupId] ?: emptyList()
        current[groupId] = if (poll == null) existing else {
            val replaced = existing.map { if (it.id == poll.id) poll else it }
            if (replaced.none { it.id == poll.id }) replaced + poll else replaced
        }
        _pollsByGroup.value = current
    }

    fun simulatePollsUpdate(groupId: String, polls: List<Poll>) {
        _pollsByGroup.value = _pollsByGroup.value.toMutableMap().also { it[groupId] = polls }
    }

    fun simulateCandidatesUpdate(pollId: String, candidates: List<PollCandidate>) {
        _candidatesByPoll.value = _candidatesByPoll.value.toMutableMap().also { it[pollId] = candidates }
    }

    fun simulateRemoteExpenseGroupPush(groupId: String, groups: List<ExpenseGroup>) {
        _expenseGroupsByGroup.value = _expenseGroupsByGroup.value.toMutableMap().also { it[groupId] = groups }
    }

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

    fun getStoredDestinationsSnapshot(): List<StoredDestination> = _destinations.value
}
