package com.hllous.plantravel

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.places.PlacesApiClient

class FakePlacesApiClient(
    var destinationResults: List<PlaceResult> = emptyList(),
    var poiResults: List<PlaceResult> = emptyList(),
    var nearbyResults: List<PlaceResult> = emptyList(),
    var searchDestinationsThrows: Boolean = false,
    var searchPoisThrows: Boolean = false,
    var beforeSearchPois: suspend (List<String>) -> Unit = {},
    var afterSearchPois: suspend (List<String>) -> Unit = {},
) : PlacesApiClient {

    var lastSearchedRegion: String? = null
    var lastSearchedLat: Double? = null
    var lastSearchedLng: Double? = null
    var lastSearchedTypes: List<String>? = null
    var lastNearbyQuery: String? = null
    var lastNearbyLat: Double? = null
    var lastNearbyLng: Double? = null

    override suspend fun searchDestinations(region: String): List<PlaceResult> {
        if (searchDestinationsThrows) throw RuntimeException("network error")
        lastSearchedRegion = region
        return destinationResults
    }

    override suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult> {
        if (searchPoisThrows) throw RuntimeException("network error")
        beforeSearchPois(types)
        lastSearchedLat = lat
        lastSearchedLng = lng
        lastSearchedTypes = types
        val result = poiResults
        afterSearchPois(types)
        return result
    }

    override suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult> {
        lastNearbyQuery = query
        lastNearbyLat = lat
        lastNearbyLng = lng
        return nearbyResults
    }

    override fun resolvePhotoUrl(resourceName: String): String =
        "https://places.googleapis.com/v1/$resourceName/media?maxWidthPx=800&key=test"
}
