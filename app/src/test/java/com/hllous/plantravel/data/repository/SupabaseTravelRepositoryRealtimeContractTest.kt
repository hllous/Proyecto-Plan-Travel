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
        val observeGroupsStart = source.indexOf("fun observeGroups()")
        val nextFunStart = source.indexOf("\n    override", observeGroupsStart + 1)
            .takeIf { it > observeGroupsStart } ?: source.length
        val observeGroupsBody = source.substring(observeGroupsStart, nextFunStart)

        assertTrue(
            "observeGroups must contain a broadcastFlow to handle cross-user RLS gaps",
            observeGroupsBody.contains("broadcastFlow")
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
