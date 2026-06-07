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
