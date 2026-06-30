package com.hllous.plantravel.data.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.places.PlacesApiClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope

@Singleton
class CachedPlacesApiClient @Inject constructor(
    @Named("remotePlacesApiClient") private val delegate: PlacesApiClient,
    private val cache: PlaceSessionCache,
) : PlacesApiClient {

    private val inFlightDetailRequests = ConcurrentHashMap<String, Deferred<PlaceResult>>()

    override suspend fun searchDestinations(query: String): List<PlaceResult> =
        delegate.searchDestinations(query).map { rememberPlace(it) }

    override suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult> =
        delegate.searchPois(lat, lng, types).map { rememberPlace(it) }

    override suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult> =
        delegate.searchNearby(query, lat, lng).map { rememberPlace(it) }

    override suspend fun fetchPlaceDetails(placeId: String): PlaceResult {
        getCachedPlace(placeId)?.takeIf { isCachedPlaceDetailed(placeId) }?.let { return it }

        return coroutineScope {
            val deferred = async(start = CoroutineStart.LAZY) {
                try {
                    val detailed = delegate.fetchPlaceDetails(placeId)
                    rememberPlace(detailed, hasFullDetails = true)
                } catch (error: Throwable) {
                    getCachedPlace(placeId) ?: throw error
                } finally {
                    inFlightDetailRequests.remove(placeId)
                }
            }
            val active = inFlightDetailRequests.putIfAbsent(placeId, deferred)?.also {
                deferred.cancel()
            } ?: deferred.also { it.start() }
            active.await()
        }
    }

    override fun resolvePhotoUrl(resourceName: String): String = delegate.resolvePhotoUrl(resourceName)

    override fun getCachedPlace(placeId: String): PlaceResult? = cache.get(placeId)?.place

    override fun isCachedPlaceDetailed(placeId: String): Boolean = cache.get(placeId)?.hasFullDetails == true

    override fun rememberPlace(place: PlaceResult, hasFullDetails: Boolean): PlaceResult =
        cache.remember(place, hasFullDetails).place
}
