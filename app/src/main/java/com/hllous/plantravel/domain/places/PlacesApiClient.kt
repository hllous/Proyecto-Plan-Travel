package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult

interface PlacesApiClient {
    suspend fun searchDestinations(query: String): List<PlaceResult>
    suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult>
    suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult>
    suspend fun fetchPlaceDetails(placeId: String): PlaceResult
    fun resolvePhotoUrl(resourceName: String): String
    fun getCachedPlace(placeId: String): PlaceResult?
    fun isCachedPlaceDetailed(placeId: String): Boolean
    fun rememberPlace(place: PlaceResult, hasFullDetails: Boolean = false): PlaceResult
}
