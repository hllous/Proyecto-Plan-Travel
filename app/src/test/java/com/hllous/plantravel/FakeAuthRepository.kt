package com.hllous.plantravel

import com.hllous.plantravel.domain.auth.AuthRepository
import com.hllous.plantravel.domain.auth.ProfileDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeAuthRepository(
    val userIdFlow: MutableSharedFlow<String?> = MutableSharedFlow(replay = 1),
    var loginResult: Result<Unit> = Result.success(Unit),
    var registerResult: Result<Unit> = Result.success(Unit),
    var createProfileResult: Result<Unit> = Result.success(Unit),
    var loginWithGoogleResult: Result<Unit> = Result.success(Unit),
    var updateDisplayNameResult: Result<Unit> = Result.success(Unit),
    var updateProfileResult: Result<Unit> = Result.success(Unit),
    var displayName: String? = null,
    var phone: String = "",
    var mpAlias: String = "",
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

    override suspend fun getProfileDetails(userId: String): ProfileDetails? =
        displayName?.let {
            ProfileDetails(
                displayName = it,
                phone = phone,
                mpAlias = mpAlias,
            )
        }

    override suspend fun createProfile(userId: String, displayName: String, phone: String): Result<Unit> {
        if (createProfileResult.isSuccess) {
            this.displayName = displayName
            this.phone = phone
        }
        return createProfileResult
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> {
        if (updateDisplayNameResult.isSuccess) this.displayName = newName
        return updateDisplayNameResult
    }

    override suspend fun updateProfile(displayName: String, phone: String, mpAlias: String): Result<Unit> {
        if (updateProfileResult.isSuccess) {
            this.displayName = displayName
            this.phone = phone
            this.mpAlias = mpAlias
        }
        return updateProfileResult
    }

    suspend fun emitUserId(userId: String?) = userIdFlow.emit(userId)
}
