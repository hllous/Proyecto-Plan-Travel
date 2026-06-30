package com.hllous.plantravel.di

import com.hllous.plantravel.BuildConfig
import com.hllous.plantravel.data.destination.DestinationPhotoResolverImpl
import com.hllous.plantravel.data.places.CachedPlacesApiClient
import com.hllous.plantravel.data.places.GooglePlacesApiClient
import com.hllous.plantravel.data.places.PlaceSessionCache
import com.hllous.plantravel.domain.destination.DestinationPhotoResolver
import com.hllous.plantravel.domain.places.PlacesApiClient
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
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
    abstract fun bindPlacesApiClient(impl: CachedPlacesApiClient): PlacesApiClient

    @Binds
    @Singleton
    @Named("remotePlacesApiClient")
    abstract fun bindRemotePlacesApiClient(impl: GooglePlacesApiClient): PlacesApiClient

    @Binds
    @Singleton
    abstract fun bindDestinationPhotoResolver(impl: DestinationPhotoResolverImpl): DestinationPhotoResolver

    companion object {
        @Provides
        @Singleton
        fun providePlacesHttpClient(): HttpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true; encodeDefaults = true })
            }
        }

        @Provides
        @Named("placesApiKey")
        fun providePlacesApiKey(): String = BuildConfig.GOOGLE_PLACES_API_KEY

        @Provides
        @Singleton
        fun providePlaceSessionCache(): PlaceSessionCache = PlaceSessionCache(maxEntries = 100)
    }
}
