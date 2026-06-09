package com.hllous.plantravel.data.auth

import com.hllous.plantravel.domain.auth.AuthRepository
import com.hllous.plantravel.domain.auth.ProfileDetails
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private inline fun <T> safeResult(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (e: CancellationException) {
    throw e
} catch (e: Throwable) {
    Result.failure(e)
}

class SupabaseAuthRepository(private val supabase: SupabaseClient) : AuthRepository {

    @Serializable
    private data class ProfileDto(
        val id: String,
        @SerialName("display_name") val displayName: String,
        val phone: String? = null,
        @SerialName("mp_alias") val mpAlias: String? = null,
    )

    @Serializable
    private data class DisplayNameDto(@SerialName("display_name") val displayName: String)

    override fun observeUserId(): Flow<String?> = supabase.auth.sessionStatus
        .filter { it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated }
        .map { status -> (status as? SessionStatus.Authenticated)?.session?.user?.id }

    override fun observeUserEmail(): Flow<String?> = supabase.auth.sessionStatus
        .filter { it is SessionStatus.Authenticated || it is SessionStatus.NotAuthenticated }
        .map { status -> (status as? SessionStatus.Authenticated)?.session?.user?.email }

    override suspend fun register(email: String, password: String, displayName: String): Result<Unit> = safeResult {
        supabase.auth.signUpWith(Email) {
            this.email = email
            this.password = password
        }
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("Registro exitoso pero sin sesión de usuario")
        supabase.from("profiles").upsert(
            ProfileDto(id = userId, displayName = displayName, phone = null)
        )
    }

    override suspend fun login(email: String, password: String): Result<Unit> = safeResult {
        supabase.auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
    }

    override suspend fun logout() {
        supabase.auth.signOut()
    }

    override suspend fun loginWithGoogle(): Result<Unit> = safeResult {
        supabase.auth.signInWith(Google)
    }

    override suspend fun getDisplayName(userId: String): String? = safeResult {
        supabase.from("profiles")
            .select(Columns.list("display_name")) {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<DisplayNameDto>()
            ?.displayName
    }.getOrNull()

    override suspend fun getProfileDetails(userId: String): ProfileDetails? = safeResult {
        supabase.from("profiles")
            .select(Columns.raw("id, display_name, phone, mp_alias")) {
                filter { eq("id", userId) }
                limit(1)
            }
            .decodeSingleOrNull<ProfileDto>()
            ?.let { profile ->
                ProfileDetails(
                    displayName = profile.displayName,
                    phone = profile.phone.orEmpty(),
                    mpAlias = profile.mpAlias.orEmpty(),
                )
            }
    }.getOrNull()

    override suspend fun createProfile(
        userId: String,
        displayName: String,
        phone: String
    ): Result<Unit> = safeResult {
        supabase.from("profiles").upsert(
            ProfileDto(
                id = userId,
                displayName = displayName,
                phone = phone.ifBlank { null },
                mpAlias = null,
            )
        )
    }

    override suspend fun updateDisplayName(newName: String): Result<Unit> = safeResult {
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("No hay sesión activa")
        supabase.from("profiles").update({ set("display_name", newName) }) {
            filter { eq("id", userId) }
        }
    }

    override suspend fun updateProfile(displayName: String, phone: String, mpAlias: String): Result<Unit> = safeResult {
        val userId = supabase.auth.currentUserOrNull()?.id ?: error("No hay sesión activa")
        supabase.from("profiles").update({
            set("display_name", displayName)
            set("phone", phone.ifBlank { null })
            set("mp_alias", mpAlias.ifBlank { null })
        }) {
            filter { eq("id", userId) }
        }
    }
}
