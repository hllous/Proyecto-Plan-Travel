package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult

interface PlacesApiClient {
    suspend fun searchDestinations(region: String): List<PlaceResult>
    suspend fun searchPois(lat: Double, lng: Double, type: String): List<PlaceResult>
}
