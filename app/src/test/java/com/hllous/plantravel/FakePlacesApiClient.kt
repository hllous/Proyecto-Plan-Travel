package com.hllous.plantravel

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.places.PlacesApiClient

class FakePlacesApiClient(
    var destinationResults: List<PlaceResult> = emptyList(),
    var poiResults: List<PlaceResult> = emptyList(),
    var searchDestinationsThrows: Boolean = false,
    var searchPoisThrows: Boolean = false,
) : PlacesApiClient {

    var lastSearchedRegion: String? = null
    var lastSearchedLat: Double? = null
    var lastSearchedLng: Double? = null
    var lastSearchedType: String? = null

    override suspend fun searchDestinations(region: String): List<PlaceResult> {
        if (searchDestinationsThrows) throw RuntimeException("network error")
        lastSearchedRegion = region
        return destinationResults
    }

    override suspend fun searchPois(lat: Double, lng: Double, type: String): List<PlaceResult> {
        if (searchPoisThrows) throw RuntimeException("network error")
        lastSearchedLat = lat
        lastSearchedLng = lng
        lastSearchedType = type
        return poiResults
    }
}
