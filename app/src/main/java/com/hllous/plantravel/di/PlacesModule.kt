package com.hllous.plantravel.di

import com.hllous.plantravel.BuildConfig
import com.hllous.plantravel.data.places.GooglePlacesApiClient
import com.hllous.plantravel.domain.places.PlacesApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.serialization.json.Json

@Module
@InstallIn(SingletonComponent::class)
abstract class PlacesModule {

    @Binds
    @Singleton
    abstract fun bindPlacesApiClient(impl: GooglePlacesApiClient): PlacesApiClient

    companion object {
        @Provides
        @Singleton
        fun providePlacesHttpClient(): HttpClient = HttpClient(Android) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
        }

        @Provides
        @Named("placesApiKey")
        fun providePlacesApiKey(): String = BuildConfig.GOOGLE_PLACES_API_KEY
    }
}
