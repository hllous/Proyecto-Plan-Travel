package com.hllous.plantravel

import com.hllous.plantravel.domain.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository(
    val userIdFlow: MutableSharedFlow<String?> = MutableSharedFlow(replay = 1),
    var loginResult: Result<Unit> = Result.success(Unit),
    var registerResult: Result<Unit> = Result.success(Unit),
    var createProfileResult: Result<Unit> = Result.success(Unit),
    var displayName: String? = null,
) : AuthRepository {

    var logoutCalled = false

    override fun observeUserId(): Flow<String?> = userIdFlow

    override suspend fun login(email: String, password: String): Result<Unit> = loginResult

    override suspend fun register(email: String, password: String): Result<Unit> = registerResult

    override suspend fun logout() {
        logoutCalled = true
    }

    override suspend fun getDisplayName(userId: String): String? = displayName

    override suspend fun createProfile(userId: String, displayName: String, phone: String): Result<Unit> =
        createProfileResult

    suspend fun emitUserId(userId: String?) = userIdFlow.emit(userId)
}
