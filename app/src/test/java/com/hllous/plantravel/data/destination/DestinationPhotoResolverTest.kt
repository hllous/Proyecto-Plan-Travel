package com.hllous.plantravel.data.destination

import com.hllous.plantravel.FakePlacesApiClient
import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.MainDispatcherRule
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.StoredDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DestinationPhotoResolverTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun storedDestination(
        id: String,
        name: String,
        region: String,
        province: String,
        lat: Double = -41.0,
        lng: Double = -71.0,
    ) = StoredDestination(
        id = id,
        source = "geonames",
        sourceId = id,
        name = name,
        normalizedName = DestinationTextNormalizer.normalize(name),
        province = province,
        region = region,
        countryCode = "AR",
        lat = lat,
        lng = lng,
        population = 1000,
        googlePlaceId = null,
        displayPhotoUrl = null,
    )

    private fun resolver(
        places: FakePlacesApiClient = FakePlacesApiClient(),
        repo: FakeTravelRepository = FakeTravelRepository(),
    ) = DestinationPhotoResolverImpl(places, repo)

    // ── Test 1: Google photo via real fetchGooglePhotoUrl ──────────────────────

    @Test
    fun resolveReturnsGooglePhotoUrlWhenPlacesApiFindsLocalityMatch() = runTest {
        val dest = storedDestination("dest-bari", "Bariloche", "Patagonia", "Río Negro")
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-bari",
                    name = "Bariloche",
                    photoUrl = "https://example.com/bariloche.jpg",
                    rating = 4.9, reviewCount = 1000,
                    address = "Bariloche, Río Negro, Argentina",
                    lat = -41.13, lng = -71.31,
                    primaryType = "locality",
                    types = listOf("locality"),
                ),
            ),
        )
        val repo = FakeTravelRepository(initialDestinations = listOf(dest))
        val r = resolver(places = places, repo = repo).apply { wikipediaFetcher = { null } }

        val url = r.resolve(dest)

        assertEquals("https://example.com/bariloche.jpg", url)
    }

    // ── Test 2: Non-locality results ignored, falls back to Wikipedia ──────────

    @Test
    fun resolveIgnoresNonLocalityResultsAndFallsBackToWikipedia() = runTest {
        val dest = storedDestination("dest-bari", "Bariloche", "Patagonia", "Río Negro")
        val places = FakePlacesApiClient(
            destinationResults = listOf(
                PlaceResult(
                    placeId = "place-museum",
                    name = "Museo Patagónico",
                    photoUrl = "https://example.com/museum.jpg",
                    rating = 4.5, reviewCount = 50,
                    address = "Bariloche, Río Negro, Argentina",
                    lat = -41.13, lng = -71.31,
                    primaryType = "museum",
                    types = listOf("museum"),
                ),
            ),
        )
        val repo = FakeTravelRepository(initialDestinations = listOf(dest))
        val r = resolver(places = places, repo = repo).apply {
            wikipediaFetcher = { "https://wikipedia.example/Bariloche.jpg" }
        }

        val url = r.resolve(dest)

        assertEquals("https://wikipedia.example/Bariloche.jpg", url)
    }

    // ── Test 3: PlacesApi throws → falls back to Wikipedia ────────────────────

    @Test
    fun resolveFallsBackToWikipediaWhenGooglePlacesThrows() = runTest {
        val dest = storedDestination("dest-1", "Bariloche", "Patagonia", "Río Negro")
        val places = FakePlacesApiClient(searchDestinationsThrows = true)
        val repo = FakeTravelRepository(initialDestinations = listOf(dest))
        val r = resolver(places = places, repo = repo).apply {
            wikipediaFetcher = { "https://wikipedia.example/${it.name}.jpg" }
        }

        val url = r.resolve(dest)

        assertEquals("https://wikipedia.example/Bariloche.jpg", url)
    }

    // ── Test 4: Both APIs null → fallback token ────────────────────────────────

    @Test
    fun resolveFallsBackToFallbackImageTokenWhenBothApisReturnNull() = runTest {
        val dest = storedDestination("dest-1", "Lago Puelo", "Patagonia", "Chubut")
        val r = resolver().apply {
            googleFetcher = { null }
            wikipediaFetcher = { null }
        }

        val url = r.resolve(dest)

        assertEquals(DestinationFallbackImage.tokenFor(dest), url)
    }

    // ── Test 5: updateDestinationPhoto called on Google success ───────────────

    @Test
    fun resolveCallsUpdateDestinationPhotoWhenGoogleSucceeds() = runTest {
        val dest = storedDestination("dest-1", "El Chaltén", "Patagonia", "Santa Cruz")
        val repo = FakeTravelRepository(initialDestinations = listOf(dest))
        val r = resolver(repo = repo).apply {
            googleFetcher = { "https://google.example/chalten.jpg" }
            wikipediaFetcher = { null }
        }

        r.resolve(dest)

        assertEquals("dest-1", repo.lastUpdatedDestinationPhotoId)
    }

    // ── Test 6: updateDestinationPhoto called on Wikipedia success ────────────

    @Test
    fun resolveCallsUpdateDestinationPhotoWhenWikipediaSucceeds() = runTest {
        val dest = storedDestination("dest-1", "El Chaltén", "Patagonia", "Santa Cruz")
        val repo = FakeTravelRepository(initialDestinations = listOf(dest))
        val r = resolver(repo = repo).apply {
            googleFetcher = { null }
            wikipediaFetcher = { "https://wikipedia.example/chalten.jpg" }
        }

        r.resolve(dest)

        assertEquals("dest-1", repo.lastUpdatedDestinationPhotoId)
    }

    // ── Test 7: No updateDestinationPhoto for fallback token ──────────────────

    @Test
    fun resolveDoesNotCallUpdateDestinationPhotoForFallbackToken() = runTest {
        val repo = FakeTravelRepository()
        val dest = storedDestination("dest-1", "Lago Puelo", "Patagonia", "Chubut")
        val r = resolver(repo = repo).apply {
            googleFetcher = { null }
            wikipediaFetcher = { null }
        }

        r.resolve(dest)

        assertNull(repo.lastUpdatedDestinationPhotoId)
    }
}
