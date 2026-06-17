package com.hllous.plantravel.data.repository

import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.exists
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupabaseTravelRepositoryRealtimeContractTest {

    private fun repositorySource(): String {
        val candidates = listOf(
            Path.of("app", "src", "main", "java", "com", "hllous", "plantravel", "data", "repository", "SupabaseTravelRepositoryImpl.kt"),
            Path.of("src", "main", "java", "com", "hllous", "plantravel", "data", "repository", "SupabaseTravelRepositoryImpl.kt"),
        )
        val path = candidates.firstOrNull { it.exists() }
            ?: error("SupabaseTravelRepositoryImpl.kt not found from test working directory")
        return Files.readString(path)
    }

    @Test
    fun broadcastObserversNeverUseNonBlockingSubscription() {
        val source = repositorySource()

        assertFalse(
            "Broadcast observers must block until subscribed to avoid missing local mutation refresh events",
            source.contains("bcChannel.subscribe(blockUntilSubscribed = false)")
        )
    }

    @Test
    fun observeGroupsHasBroadcastFallback() {
        val source = repositorySource()
        // observeGroups() delegates to observeGroupsSharedFlow; implementation is in
        // createObserveGroupsChannelFlow(). The broadcastFlow must live in that implementation.
        val implStart = source.indexOf("fun createObserveGroupsChannelFlow()")
        require(implStart >= 0) { "createObserveGroupsChannelFlow not found in repository source" }
        val implEnd = source.indexOf("\n    override", implStart + 1)
            .takeIf { it > implStart } ?: source.length
        val implBody = source.substring(implStart, implEnd)

        assertTrue(
            "createObserveGroupsChannelFlow (backing observeGroups) must contain a broadcastFlow to handle cross-user RLS gaps",
            implBody.contains("broadcastFlow")
        )
    }

    @Test
    fun consumeInviteSendsBroadcastAfterInsert() {
        val source = repositorySource()
        assertTrue(
            "consumeInvite must call sendBroadcast after inserting into group_members",
            fnBody(source, "fun consumeInvite(").contains("sendBroadcast")
        )
    }

    @Test
    fun setTripDestinationSendsBroadcastAfterUpdate() {
        val source = repositorySource()
        assertTrue(
            "setTripDestination must call sendBroadcast after updating travel_groups so other members' observeGroups broadcast fallback fires",
            fnBody(source, "fun setTripDestination(").contains("sendBroadcast")
        )
    }

    private fun fnBody(source: String, fnSignatureFragment: String): String {
        val start = source.indexOf(fnSignatureFragment)
        require(start >= 0) { "Function containing '$fnSignatureFragment' not found in source" }
        val end = source.indexOf("\n    override", start + 1)
            .takeIf { it > start } ?: source.length
        return source.substring(start, end)
    }

    @Test
    fun createPollSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "createPoll must call sendBroadcast so other members' observeActivePoll fires",
            fnBody(source, "fun createPoll(").contains("sendBroadcast")
        )
    }

    @Test
    fun closePollSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "closePoll must call sendBroadcast so other members' observeActivePoll fires",
            fnBody(source, "fun closePoll(").contains("sendBroadcast")
        )
    }

    @Test
    fun deletePollSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "deletePoll must call sendBroadcast so other members' observeActivePoll fires",
            fnBody(source, "fun deletePoll(").contains("sendBroadcast")
        )
    }

    @Test
    fun addPollCandidateSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "addPollCandidate must call sendBroadcast so other members' observePollCandidates fires",
            fnBody(source, "fun addPollCandidate(").contains("sendBroadcast")
        )
    }

    @Test
    fun toggleVoteSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "toggleVote must call sendBroadcast so other members' observePollCandidates fires",
            fnBody(source, "fun toggleVote(").contains("sendBroadcast")
        )
    }

    @Test
    fun createItineraryEventSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "createItineraryEvent must call sendBroadcast so other members' observeItineraryEvents fires",
            fnBody(source, "fun createItineraryEvent(").contains("sendBroadcast")
        )
    }

    @Test
    fun updateItineraryEventSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "updateItineraryEvent must call sendBroadcast so other members' observeItineraryEvents fires",
            fnBody(source, "fun updateItineraryEvent(").contains("sendBroadcast")
        )
    }

    @Test
    fun deleteItineraryEventSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "deleteItineraryEvent must call sendBroadcast so other members' observeItineraryEvents fires",
            fnBody(source, "fun deleteItineraryEvent(").contains("sendBroadcast")
        )
    }

    @Test
    fun createExpenseGroupSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "createExpenseGroup must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun createExpenseGroup(").contains("sendBroadcast")
        )
    }

    @Test
    fun updateExpenseGroupNameSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "updateExpenseGroupName must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun updateExpenseGroupName(").contains("sendBroadcast")
        )
    }

    @Test
    fun deleteExpenseGroupSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "deleteExpenseGroup must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun deleteExpenseGroup(").contains("sendBroadcast")
        )
    }

    @Test
    fun finalizeExpenseGroupSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "finalizeExpenseGroup must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun finalizeExpenseGroup(").contains("sendBroadcast")
        )
    }

    @Test
    fun setExpenseGroupPinnedSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "setExpenseGroupPinned must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun setExpenseGroupPinned(").contains("sendBroadcast")
        )
    }

    @Test
    fun setExpenseGroupPayerSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "setExpenseGroupPayer must call sendBroadcast so other members' observeExpenseGroups fires",
            fnBody(source, "fun setExpenseGroupPayer(").contains("sendBroadcast")
        )
    }

    @Test
    fun addExpenseItemSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "addExpenseItem must call sendBroadcast so other members' observeExpenseItems fires",
            fnBody(source, "fun addExpenseItem(").contains("sendBroadcast")
        )
    }

    @Test
    fun deleteExpenseItemSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "deleteExpenseItem must call sendBroadcast so other members' observeExpenseItems fires",
            fnBody(source, "fun deleteExpenseItem(").contains("sendBroadcast")
        )
    }

    @Test
    fun assignItemToMemberSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "assignItemToMember must call sendBroadcast so other members' observeAssignments fires",
            fnBody(source, "fun assignItemToMember(").contains("sendBroadcast")
        )
    }

    @Test
    fun observeMembersDelegatesToSharedFlowCache() {
        val source = repositorySource()
        val start = source.indexOf("fun observeMembers(")
        val end = source.indexOf("\n    override", start + 1).takeIf { it > start } ?: source.length
        val body = source.substring(start, end)
        // Check for "channelFlow {" — the builder expression — not just the substring which also
        // appears in helper function names like createObserveMembersChannelFlow.
        assertFalse(
            "observeMembers must not create a new channelFlow per call — causes broadcast channel eviction",
            body.contains("channelFlow {")
        )
        assertTrue(
            "observeMembers must use a per-groupId SharedFlow cache (like pollBroadcastFlows)",
            body.contains("observeMembersSharedFlows")
        )
    }

    @Test
    fun observeGroupsDelegatesToSharedFlowProperty() {
        val source = repositorySource()
        val start = source.indexOf("fun observeGroups()")
        val end = source.indexOf("\n    override", start + 1).takeIf { it > start } ?: source.length
        val body = source.substring(start, end)
        // Check for "channelFlow {" — the builder expression — not just the substring which also
        // appears in helper function names like createObserveGroupsChannelFlow.
        assertFalse(
            "observeGroups must not create a new channelFlow per call — causes broadcast channel eviction across ViewModels",
            body.contains("channelFlow {")
        )
        assertTrue(
            "observeGroups must delegate to a cached SharedFlow property",
            body.contains("observeGroupsSharedFlow")
        )
    }

    @Test
    fun observeInvitesHasBroadcastFallback() {
        val source = repositorySource()
        val start = source.indexOf("fun observeInvites(")
        val end = source.indexOf("\n    override", start + 1).takeIf { it > start } ?: source.length
        val body = source.substring(start, end)
        assertTrue(
            "observeInvites must contain a broadcastFlow fallback (consistent with other observe* flows)",
            body.contains("broadcastFlow")
        )
    }

    @Test
    fun generateInviteSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "generateInvite must call sendBroadcast so other admins' invite list updates",
            fnBody(source, "fun generateInvite(").contains("sendBroadcast")
        )
    }

    @Test
    fun deleteInviteSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "deleteInvite must call sendBroadcast so other admins' invite list updates",
            fnBody(source, "fun deleteInvite(").contains("sendBroadcast")
        )
    }

    @Test
    fun consumeInviteInvalidatesGroupsFlowForFreshPerGroupChannels() {
        val source = repositorySource()
        assertTrue(
            "consumeInvite must increment _observeGroupsVersion to restart the groups observe flow " +
                "so newly joined group gets a per-group broadcast channel",
            fnBody(source, "fun consumeInvite(").contains("_observeGroupsVersion")
        )
    }

    @Test
    fun consumeInviteAlsoNotifiesGroupMembersViaGroupBroadcast() {
        val source = repositorySource()
        assertTrue(
            "consumeInvite must send group_list_changed so existing members' observeGroups broadcast fallback fires",
            fnBody(source, "fun consumeInvite(").contains("group_list_changed")
        )
    }

    @Test
    fun markDebtorConfirmedSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "markDebtorConfirmed must send a broadcast so the creditor's settlement view refreshes",
            fnBody(source, "fun markDebtorConfirmed(").contains("sendBroadcast")
        )
    }

    @Test
    fun markCreditorConfirmedSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "markCreditorConfirmed must send a broadcast so the debtor's settlement view refreshes",
            fnBody(source, "fun markCreditorConfirmed(").contains("sendBroadcast")
        )
    }

    @Test
    fun updateMpAliasSendsBroadcast() {
        val source = repositorySource()
        assertTrue(
            "updateMpAlias must send a broadcast so other members' observeMembers fires and settlement view refreshes with the new alias",
            fnBody(source, "fun updateMpAlias(").contains("sendBroadcast")
        )
    }

    @Test
    fun everyBroadcastObserverUsesBlockingSubscription() {
        val source = repositorySource()
        val broadcastObserverCount = "broadcastFlow<JsonObject>".toRegex().findAll(source).count()
        val blockingSubscribeCount = "bcChannel.subscribe\\(blockUntilSubscribed = true\\)".toRegex()
            .findAll(source)
            .count()

        assertTrue("Expected at least one broadcast observer in repository source", broadcastObserverCount > 0)
        assertTrue(
            "Each broadcast observer should have a blocking bcChannel subscription",
            blockingSubscribeCount >= broadcastObserverCount
        )
    }
}
