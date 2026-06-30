package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.StoredDestination
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceRecommendationRankerTest {

    private val ranker = PlaceRecommendationRanker()

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun place(
        id: String,
        rating: Double,
        reviewCount: Int,
    ) = PlaceResult(
        placeId = id,
        name = "Place $id",
        photoUrl = "",
        rating = rating,
        reviewCount = reviewCount,
        address = "",
        lat = 0.0,
        lng = 0.0,
    )

    // ─── Tests ────────────────────────────────────────────────────────────────

    @Test
    fun singleBelowThresholdPlaceAppearsOnlyInOthers() {
        val belowRating = place("br", rating = 4.1, reviewCount = 200)
        val belowReviews = place("brc", rating = 4.5, reviewCount = 49)

        val result = ranker.rank(listOf(belowRating, belowReviews))

        assertTrue(result.top.isEmpty())
        assertEquals(setOf("br", "brc"), result.others.map { it.placeId }.toSet())
    }

    @Test
    fun exactlyAtThresholdLandsInTopSection() {
        val exact = place("exact", rating = 4.2, reviewCount = 50)

        val result = ranker.rank(listOf(exact))

        assertEquals(listOf(exact), result.top)
        assertTrue(result.others.isEmpty())
    }

    @Test
    fun emptyInputReturnsEmptySections() {
        val result = ranker.rank(emptyList())
        assertTrue(result.top.isEmpty())
        assertTrue(result.others.isEmpty())
    }

    @Test
    fun bothSectionsSortedByScoreDescending() {
        // score = rating × ln(reviewCount)
        // high: 4.8 × ln(200) = ~25.4; low: 4.3 × ln(60) = ~17.6
        val highScore = place("high", rating = 4.8, reviewCount = 200)
        val lowScore = place("low", rating = 4.3, reviewCount = 60)
        // others: below rating threshold — sorted by score among themselves
        val othersHigh = place("oh", rating = 4.0, reviewCount = 200)
        val othersLow = place("ol", rating = 3.5, reviewCount = 100)

        val result = ranker.rank(listOf(lowScore, highScore, othersLow, othersHigh))

        assertEquals(listOf("high", "low"), result.top.map { it.placeId })
        assertEquals(listOf("oh", "ol"), result.others.map { it.placeId })
    }

    @Test
    fun topSectionContainsOnlyPlacesMeetingBothThresholds() {
        val qualifies = place("a", rating = 4.5, reviewCount = 100)
        val lowRating = place("b", rating = 4.0, reviewCount = 100)
        val lowReviews = place("c", rating = 4.5, reviewCount = 10)

        val result = ranker.rank(listOf(qualifies, lowRating, lowReviews))

        assertEquals(listOf(qualifies), result.top)
        assertEquals(setOf("b", "c"), result.others.map { it.placeId }.toSet())
    }

    // ── mergeWithCurated ──────────────────────────────────────────────────────

    private fun dest(name: String) = StoredDestination(
        id = name, source = "geonames", sourceId = name,
        name = name, normalizedName = name.lowercase(),
        province = "Río Negro", region = "Patagonia", countryCode = "AR",
        lat = 0.0, lng = 0.0, population = 1000,
    )

    @Test
    fun mergeWithCurated_curatedNamesAlwaysFirst() {
        val curated = listOf(dest("Bariloche"), dest("El Bolsón"))
        val api = listOf(dest("Ushuaia"), dest("El Calafate"), dest("Bariloche"))

        val result = ranker.mergeWithCurated(curated, api)

        assertEquals("Bariloche", result[0].name)
        assertEquals("El Bolsón", result[1].name)
    }

    @Test
    fun mergeWithCurated_deduplicatesByNormalizedName() {
        val curated = listOf(dest("Bariloche"))
        val api = listOf(dest("Bariloche"), dest("Ushuaia"))

        val result = ranker.mergeWithCurated(curated, api)

        assertEquals(2, result.size)
        assertEquals(listOf("Bariloche", "Ushuaia"), result.map { it.name })
    }
}