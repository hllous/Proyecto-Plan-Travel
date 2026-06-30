package com.hllous.plantravel.domain.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.PlaceReview

fun mergePlaceResults(existing: PlaceResult?, incoming: PlaceResult?): PlaceResult? {
    if (existing == null) return incoming
    if (incoming == null) return existing

    return PlaceResult(
        placeId = incoming.placeId.ifBlank { existing.placeId },
        name = preferText(existing.name, incoming.name),
        photoUrl = preferText(existing.photoUrl, incoming.photoUrl),
        rating = maxOf(existing.rating, incoming.rating),
        reviewCount = maxOf(existing.reviewCount, incoming.reviewCount),
        address = preferText(existing.address, incoming.address),
        lat = preferCoordinate(existing.lat, incoming.lat),
        lng = preferCoordinate(existing.lng, incoming.lng),
        primaryType = preferOptionalText(existing.primaryType, incoming.primaryType),
        types = (incoming.types + existing.types).distinct(),
        photoReference = preferText(existing.photoReference, incoming.photoReference),
        reviews = preferReviews(existing.reviews, incoming.reviews),
    )
}

private fun preferText(existing: String, incoming: String): String {
    if (incoming.isBlank()) return existing
    if (existing.isBlank()) return incoming
    return if (incoming.length >= existing.length) incoming else existing
}

private fun preferOptionalText(existing: String?, incoming: String?): String? =
    when {
        !incoming.isNullOrBlank() -> incoming
        !existing.isNullOrBlank() -> existing
        else -> null
    }

private fun preferCoordinate(existing: Double, incoming: Double): Double =
    when {
        incoming != 0.0 -> incoming
        else -> existing
    }

private fun preferReviews(existing: List<PlaceReview>, incoming: List<PlaceReview>): List<PlaceReview> {
    if (incoming.isEmpty()) return existing
    if (existing.isEmpty()) return incoming

    val existingScore = existing.sumOf { it.text.length } + (existing.size * 100)
    val incomingScore = incoming.sumOf { it.text.length } + (incoming.size * 100)
    return if (incomingScore >= existingScore) incoming else existing
}
