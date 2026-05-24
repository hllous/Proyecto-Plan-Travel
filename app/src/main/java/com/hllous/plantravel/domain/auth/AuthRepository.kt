package com.hllous.plantravel.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeUserId(): Flow<String?>
    suspend fun register(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun loginWithGoogle(): Result<Unit>
    suspend fun getDisplayName(userId: String): String?
    suspend fun createProfile(userId: String, displayName: String, phone: String): Result<Unit>
}
