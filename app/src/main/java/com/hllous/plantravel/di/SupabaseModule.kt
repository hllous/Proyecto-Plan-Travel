package com.hllous.plantravel.di

import com.hllous.plantravel.BuildConfig
import com.hllous.plantravel.data.auth.SupabaseAuthRepository
import com.hllous.plantravel.data.auth.SupabaseSessionProvider
import com.hllous.plantravel.domain.auth.AuthRepository
import com.hllous.plantravel.domain.auth.SessionProvider
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SupabaseModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_PUBLISHABLE_KEY
    ) {
        install(Auth) {
            scheme = "plantravel"
            host = "auth"
        }
        install(Postgrest)
        install(Realtime)
    }

    @Provides
    @Singleton
    fun provideAuthRepository(supabase: SupabaseClient): AuthRepository =
        SupabaseAuthRepository(supabase)

    @Provides
    @Singleton
    fun provideSessionProvider(supabase: SupabaseClient): SessionProvider =
        SupabaseSessionProvider(supabase)
}
