package com.hllous.plantravel.domain.usecase

import com.hllous.plantravel.FakeSessionProvider
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.ConsumeInviteFailure
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ConsumeInviteUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Cycle 1 — valid code + authenticated user
    @Test
    fun invoke_authenticatedUser_validCode_callsRepoWithUserIdAndReturnsSuccess() = runTest {
        val session = FakeSessionProvider(userId = "user-abc", displayName = "Alice")
        val repo = FakeTravelRepository()
        val useCase = ConsumeInviteUseCase(repo, session)

        val result = useCase("INVITE01")

        assertTrue(result.isSuccess)
        assertEquals("user-abc", repo.lastConsumeUserId)
    }

    // Cycle 2 — no authenticated session
    @Test
    fun invoke_noSession_returnsUnauthenticatedFailure() = runTest {
        val session = FakeSessionProvider(userId = null)
        val repo = FakeTravelRepository()
        val useCase = ConsumeInviteUseCase(repo, session)

        val result = useCase("INVITE01")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ConsumeInviteFailure.Unauthenticated)
    }

    // Cycle 3 — repo returns expired failure
    @Test
    fun invoke_expiredCode_propagatesExpiredFailure() = runTest {
        val session = FakeSessionProvider(userId = "user-abc", displayName = "Alice")
        val repo = FakeTravelRepository(
            consumeInviteResult = Result.failure(ConsumeInviteFailure.Expired)
        )
        val useCase = ConsumeInviteUseCase(repo, session)

        val result = useCase("EXPIRED")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ConsumeInviteFailure.Expired)
    }

    // Cycle 4 — repo returns already-member failure
    @Test
    fun invoke_alreadyMember_propagatesAlreadyMemberFailure() = runTest {
        val session = FakeSessionProvider(userId = "user-abc", displayName = "Alice")
        val repo = FakeTravelRepository(
            consumeInviteResult = Result.failure(ConsumeInviteFailure.AlreadyMember)
        )
        val useCase = ConsumeInviteUseCase(repo, session)

        val result = useCase("CODE01")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is ConsumeInviteFailure.AlreadyMember)
    }
}
