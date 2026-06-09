package com.hllous.plantravel.domain.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    fun observeUserId(): Flow<String?>
    fun observeUserEmail(): Flow<String?>
    suspend fun register(email: String, password: String, displayName: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun logout()
    suspend fun loginWithGoogle(): Result<Unit>
    suspend fun getDisplayName(userId: String): String?
    suspend fun getProfileDetails(userId: String): ProfileDetails?
    suspend fun createProfile(userId: String, displayName: String, phone: String): Result<Unit>
    suspend fun updateDisplayName(newName: String): Result<Unit>
    suspend fun updateProfile(displayName: String, phone: String, mpAlias: String): Result<Unit>
}
