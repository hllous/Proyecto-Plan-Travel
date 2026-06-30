package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult

interface PlacesApiClient {
    suspend fun searchDestinations(query: String): List<PlaceResult>
    suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult>
    suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult>
    fun resolvePhotoUrl(resourceName: String): String
}
