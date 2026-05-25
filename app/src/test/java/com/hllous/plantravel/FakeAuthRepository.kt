package com.hllous.plantravel

import com.hllous.plantravel.domain.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository(
    val userIdFlow: MutableSharedFlow<String?> = MutableSharedFlow(replay = 1),
    var loginResult: Result<Unit> = Result.success(Unit),
    var registerResult: Result<Unit> = Result.success(Unit),
    var createProfileResult: Result<Unit> = Result.success(Unit),
    var loginWithGoogleResult: Result<Unit> = Result.success(Unit),
    var displayName: String? = null,
    var email: String? = null,
) : AuthRepository {

    var logoutCalled = false
    val userEmailFlow: MutableSharedFlow<String?> = MutableSharedFlow(replay = 1)

    override fun observeUserId(): Flow<String?> = userIdFlow
    override fun observeUserEmail(): Flow<String?> = userEmailFlow

    override suspend fun login(email: String, password: String): Result<Unit> = loginResult

    override suspend fun register(email: String, password: String, displayName: String): Result<Unit> {
        if (registerResult.isSuccess) this.displayName = displayName
        return registerResult
    }

    override suspend fun logout() {
        logoutCalled = true
    }

    override suspend fun loginWithGoogle(): Result<Unit> = loginWithGoogleResult

    override suspend fun getDisplayName(userId: String): String? = displayName

    override suspend fun createProfile(userId: String, displayName: String, phone: String): Result<Unit> {
        if (createProfileResult.isSuccess) this.displayName = displayName
        return createProfileResult
    }

    suspend fun emitUserId(userId: String?) = userIdFlow.emit(userId)
}
