package com.hllous.plantravel.data.destination

import com.hllous.plantravel.FakeTravelRepository
import com.hllous.plantravel.domain.model.DestinationDraft
import com.hllous.plantravel.domain.model.StoredDestination
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelRepositoryDestinationCatalogTest {

    @Test
    fun browseDestinationsReturnsStoredDestinationsInStableOrder() {
        val repository = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(id = "1", name = "Villa La Angostura", region = "Patagonia", population = 11_063),
                storedDestination(id = "2", name = "Bariloche", region = "Patagonia", population = 135_000),
                storedDestination(id = "3", name = "Ushuaia", region = "Patagonia", population = 82_615),
                storedDestination(id = "4", name = "Mendoza", region = "Cuyo", province = "Mendoza", population = 115_041),
            ),
        )

        val destinations = runSuspend { repository.browseDestinations("Patagonia") }

        assertEquals(listOf("Bariloche", "Ushuaia", "Villa La Angostura"), destinations.map { it.name })
    }

    @Test
    fun searchDestinationsMatchesAccentsAndCaseInsensitiveQueries() {
        val repository = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(id = "1", name = "Malargüe", region = "Cuyo", province = "Mendoza"),
                storedDestination(id = "2", name = "Villa Pehuenia", region = "Patagonia", province = "Neuquén"),
            ),
        )

        val results = runSuspend { repository.searchDestinations("malargue") }

        assertEquals(listOf("Malargüe"), results.map { it.name })
    }

    @Test
    fun upsertDestinationAddsOrUpdatesStoredDestinationRow() {
        val repository = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(
                    id = "dest-1",
                    source = "google",
                    sourceId = "place-123",
                    name = "Villa Pehuenia",
                    region = "Patagonia",
                    province = "Neuquén",
                    googlePlaceId = "place-123",
                ),
            ),
        )

        val updated = runSuspend {
            repository.upsertDestination(
                DestinationDraft(
                    source = "google",
                    sourceId = "place-123",
                    name = "Villa Pehuenia",
                    province = "Neuquén",
                    region = "Patagonia",
                    lat = -38.88,
                    lng = -71.21,
                    population = 2_000,
                    googlePlaceId = "place-123",
                    displayPhotoUrl = "https://example.com/villa-pehuenia.jpg",
                ),
            )
        }

        assertEquals("dest-1", updated.id)
        assertEquals("https://example.com/villa-pehuenia.jpg", updated.displayPhotoUrl)
        assertEquals(1, repository.getStoredDestinationsSnapshot().size)
    }

    @Test
    fun updateDestinationPhotoPersistsDisplayPhotoUrl() {
        val repository = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(id = "dest-1", name = "El Chaltén", region = "Patagonia", province = "Santa Cruz"),
            ),
        )

        val updated = runSuspend {
            repository.updateDestinationPhoto(
                destinationId = "dest-1",
                wikipediaPhotoUrl = "https://example.com/chalten-wiki.jpg",
                displayPhotoUrl = "https://example.com/chalten-wiki.jpg",
            )
        }

        assertEquals("https://example.com/chalten-wiki.jpg", updated.displayPhotoUrl)
        assertEquals(
            "https://example.com/chalten-wiki.jpg",
            repository.getStoredDestinationsSnapshot().single().displayPhotoUrl,
        )
    }

    @Test
    fun updateDestinationPhotoAcceptsFallbackTokenDisplayUrl() {
        val repository = FakeTravelRepository(
            initialDestinations = listOf(
                storedDestination(id = "dest-1", name = "Gualeguay", region = "Litoral", province = "Entre Ríos"),
            ),
        )

        val updated = runSuspend {
            repository.updateDestinationPhoto(
                destinationId = "dest-1",
                displayPhotoUrl = DestinationFallbackImage.tokenForRegion("Litoral"),
            )
        }

        assertEquals(DestinationFallbackImage.tokenForRegion("Litoral"), updated.displayPhotoUrl)
    }

    private fun storedDestination(
        id: String,
        name: String,
        region: String,
        province: String = "Río Negro",
        population: Int = 1_000,
        source: String = "geonames",
        sourceId: String = id,
        googlePlaceId: String? = null,
    ) = StoredDestination(
        id = id,
        source = source,
        sourceId = sourceId,
        name = name,
        normalizedName = DestinationTextNormalizer.normalize(name),
        province = province,
        region = region,
        countryCode = "AR",
        lat = -41.0,
        lng = -71.0,
        population = population,
        googlePlaceId = googlePlaceId,
    )

    private fun <T> runSuspend(block: suspend () -> T): T = kotlinx.coroutines.runBlocking { block() }
}
