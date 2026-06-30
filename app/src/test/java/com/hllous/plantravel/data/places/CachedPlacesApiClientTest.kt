package com.hllous.plantravel.data.places

import com.hllous.plantravel.domain.model.PlaceResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CachedPlacesApiClientTest {

    private fun place(
        placeId: String,
        name: String = "Lugar",
        photoUrl: String = "",
        rating: Double = 0.0,
        reviewCount: Int = 0,
        address: String = "",
        lat: Double = 0.0,
        lng: Double = 0.0,
    ) = PlaceResult(
        placeId = placeId,
        name = name,
        photoUrl = photoUrl,
        rating = rating,
        reviewCount = reviewCount,
        address = address,
        lat = lat,
        lng = lng,
    )

    @Test
    fun searchResultsPopulateCacheForLaterPreviewReuse() = runTest {
        val delegate = FakeGooglePlacesApiClient(
            searchPoisResult = listOf(
                place(
                    placeId = "p1",
                    name = "Cerro Campanario",
                    photoUrl = "https://example.com/photo.jpg",
                    address = "Bariloche",
                ),
            ),
        )
        val client = CachedPlacesApiClient(delegate, PlaceSessionCache())

        client.searchPois(-41.1, -71.3, listOf("tourist_attraction"))

        val cached = client.getCachedPlace("p1")
        assertEquals("https://example.com/photo.jpg", cached?.photoUrl)
        assertEquals("Bariloche", cached?.address)
    }

    @Test
    fun fullDetailCacheAvoidsSecondFetch() = runTest {
        val detailed = place(
            placeId = "p1",
            name = "Cerro Campanario",
            photoUrl = "https://example.com/photo.jpg",
            rating = 4.8,
            reviewCount = 1200,
            address = "Bariloche",
        )
        val delegate = FakeGooglePlacesApiClient(
            detailResult = detailed,
        )
        val client = CachedPlacesApiClient(delegate, PlaceSessionCache())

        val first = client.fetchPlaceDetails("p1")
        val second = client.fetchPlaceDetails("p1")

        assertEquals(detailed, first)
        assertEquals(detailed, second)
        assertEquals(1, delegate.fetchPlaceDetailsCallCount)
        assertTrue(client.isCachedPlaceDetailed("p1"))
    }

    @Test
    fun fetchFailurePreservesBestCachedData() = runTest {
        val delegate = FakeGooglePlacesApiClient(
            detailThrows = true,
        )
        val client = CachedPlacesApiClient(delegate, PlaceSessionCache())
        client.rememberPlace(
            place(
                placeId = "p1",
                name = "Refugio",
                photoUrl = "https://example.com/refugio.jpg",
                address = "Calle 123",
            ),
        )

        val result = client.fetchPlaceDetails("p1")

        assertEquals("Refugio", result.name)
        assertEquals("https://example.com/refugio.jpg", result.photoUrl)
        assertEquals(1, delegate.fetchPlaceDetailsCallCount)
    }

    @Test
    fun richerDetailMergesIntoCachedSummary() = runTest {
        val delegate = FakeGooglePlacesApiClient(
            detailResult = place(
                placeId = "p1",
                name = "Museo de la Patagonia",
                photoUrl = "https://example.com/detail.jpg",
                rating = 4.7,
                reviewCount = 560,
                address = "Centro Cívico",
            ),
        )
        val client = CachedPlacesApiClient(delegate, PlaceSessionCache())
        client.rememberPlace(
            place(
                placeId = "p1",
                name = "Museo",
                photoUrl = "",
                rating = 0.0,
                reviewCount = 0,
                address = "",
            ),
        )

        val result = client.fetchPlaceDetails("p1")

        assertEquals("Museo de la Patagonia", result.name)
        assertEquals("https://example.com/detail.jpg", result.photoUrl)
        assertEquals(560, result.reviewCount)
    }

    @Test
    fun leastRecentlyUsedEntryIsEvictedWhenCapacityExceeded() = runTest {
        val client = CachedPlacesApiClient(
            delegate = FakeGooglePlacesApiClient(),
            cache = PlaceSessionCache(maxEntries = 2),
        )

        client.rememberPlace(place(placeId = "p1"))
        client.rememberPlace(place(placeId = "p2"))
        client.getCachedPlace("p1")
        client.rememberPlace(place(placeId = "p3"))

        assertEquals(null, client.getCachedPlace("p2"))
        assertEquals("p1", client.getCachedPlace("p1")?.placeId)
        assertEquals("p3", client.getCachedPlace("p3")?.placeId)
    }

    @Test
    fun concurrentDetailRequestsShareOneInFlightFetch() = runTest {
        val delegate = FakeGooglePlacesApiClient(
            detailResult = place(placeId = "p1", name = "Cascada"),
            delayDetailUntilAwaited = true,
        )
        val client = CachedPlacesApiClient(delegate, PlaceSessionCache())

        val first = async { client.fetchPlaceDetails("p1") }
        val second = async { client.fetchPlaceDetails("p1") }
        delegate.releaseDetailFetch()
        val results = awaitAll(first, second)

        assertEquals(listOf("Cascada", "Cascada"), results.map { it.name })
        assertEquals(1, delegate.fetchPlaceDetailsCallCount)
    }
}
