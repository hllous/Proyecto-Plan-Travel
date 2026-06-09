package com.hllous.plantravel.data.auth

import com.hllous.plantravel.domain.auth.SessionProvider
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth

class SupabaseSessionProvider(private val supabase: SupabaseClient) : SessionProvider {
    override val userId: String?
        get() = supabase.auth.currentUserOrNull()?.id

    override val displayName: String?
        get() {
            val metadata = supabase.auth.currentUserOrNull()?.userMetadata ?: return null
            return metadata["display_name"]?.toString()?.trim('"')
                ?: metadata["full_name"]?.toString()?.trim('"')
                ?: metadata["name"]?.toString()?.trim('"')
        }
}
