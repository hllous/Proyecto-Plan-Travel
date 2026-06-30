package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.RankedRecommendations
import com.hllous.plantravel.domain.model.StoredDestination
import kotlin.math.ln

private const val RATING_THRESHOLD = 4.2
private const val REVIEW_COUNT_THRESHOLD = 50

class PlaceRecommendationRanker @javax.inject.Inject constructor() {

    fun rank(places: List<PlaceResult>): RankedRecommendations {
        val (top, others) = places.partition { it.meetsThresholds() }
        return RankedRecommendations(
            top = top.sortedByScore(),
            others = others.sortedByScore(),
        )
    }

    private fun PlaceResult.meetsThresholds() =
        rating >= RATING_THRESHOLD && reviewCount >= REVIEW_COUNT_THRESHOLD

    private fun List<PlaceResult>.sortedByScore() =
        sortedByDescending { it.score() }

    private fun PlaceResult.score() =
        if (reviewCount > 0) rating * ln(reviewCount.toDouble()) else 0.0

    fun mergeWithCurated(
        curated: List<StoredDestination>,
        api: List<StoredDestination>,
    ): List<StoredDestination> {
        val curatedNames = curated.map { it.normalizedName }.toSet()
        val apiFiltered = api.filterNot { it.normalizedName in curatedNames }
        return curated + apiFiltered
    }
}
