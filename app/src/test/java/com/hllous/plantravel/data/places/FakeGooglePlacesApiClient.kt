package com.hllous.plantravel.data.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.places.PlacesApiClient
import kotlinx.coroutines.CompletableDeferred

class FakeGooglePlacesApiClient(
    private val searchDestinationsResult: List<PlaceResult> = emptyList(),
    private val searchPoisResult: List<PlaceResult> = emptyList(),
    private val searchNearbyResult: List<PlaceResult> = emptyList(),
    private val detailResult: PlaceResult = PlaceResult(
        placeId = "detail",
        name = "Detail",
        photoUrl = "",
        rating = 0.0,
        reviewCount = 0,
        address = "",
        lat = 0.0,
        lng = 0.0,
    ),
    private val detailThrows: Boolean = false,
    private val delayDetailUntilAwaited: Boolean = false,
) : PlacesApiClient {
    var fetchPlaceDetailsCallCount: Int = 0
        private set

    private val releaseDetail = CompletableDeferred<Unit>()

    override suspend fun searchDestinations(query: String): List<PlaceResult> = searchDestinationsResult

    override suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult> = searchPoisResult

    override suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult> = searchNearbyResult

    override suspend fun fetchPlaceDetails(placeId: String): PlaceResult {
        fetchPlaceDetailsCallCount++
        if (delayDetailUntilAwaited) releaseDetail.await()
        if (detailThrows) throw RuntimeException("network error")
        return detailResult
    }

    fun releaseDetailFetch() {
        releaseDetail.complete(Unit)
    }

    override fun resolvePhotoUrl(resourceName: String): String = resourceName

    override fun getCachedPlace(placeId: String): PlaceResult? = null

    override fun isCachedPlaceDetailed(placeId: String): Boolean = false

    override fun rememberPlace(place: PlaceResult, hasFullDetails: Boolean): PlaceResult = place
}
