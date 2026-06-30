package com.hllous.plantravel.data.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.PlaceReview
import com.hllous.plantravel.domain.places.PlacesApiClient
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

private const val BASE_URL = "https://places.googleapis.com/v1/places"
private const val FIELD_MASK = "places.id,places.displayName,places.rating,places.userRatingCount,places.photos,places.formattedAddress,places.location,places.primaryType,places.types,places.reviews"
private const val DETAIL_FIELD_MASK = "id,displayName,rating,userRatingCount,photos,formattedAddress,location,primaryType,types,reviews"
private const val PHOTO_WIDTH = 800

@Singleton
open class GooglePlacesApiClient @Inject constructor(
    private val httpClient: HttpClient,
    @Named("placesApiKey") private val apiKey: String,
) : PlacesApiClient {

    // ─── Response DTOs ────────────────────────────────────────────────────────

    @Serializable
    private data class TextSearchRequest(
        val textQuery: String,
        val languageCode: String = "es",
        val regionCode: String = "AR",
    )

    @Serializable
    private data class TextSearchWithBiasRequest(
        val textQuery: String,
        val locationBias: LocationBias,
        val languageCode: String = "es",
        val regionCode: String = "AR",
        val maxResultCount: Int = 20,
    )

    @Serializable
    private data class LocationBias(val circle: Circle)

    @Serializable
    private data class NearbySearchRequest(
        val locationRestriction: LocationRestriction,
        val includedTypes: List<String>,
        val languageCode: String = "es",
        val rankPreference: String = "POPULARITY",
        val maxResultCount: Int = 20,
    )

    @Serializable
    private data class LocationRestriction(val circle: Circle)

    @Serializable
    private data class Circle(val center: LatLng, val radius: Double)

    @Serializable
    private data class LatLng(val latitude: Double, val longitude: Double)

    @Serializable
    private data class PlacesResponse(val places: List<PlaceDto> = emptyList())

    @Serializable
    private data class PlaceDto(
        val id: String = "",
        @SerialName("displayName") val displayName: DisplayName = DisplayName(),
        val rating: Double = 0.0,
        @SerialName("userRatingCount") val userRatingCount: Int = 0,
        val photos: List<PhotoDto> = emptyList(),
        @SerialName("formattedAddress") val formattedAddress: String = "",
        val location: LatLng = LatLng(0.0, 0.0),
        @SerialName("primaryType") val primaryType: String? = null,
        val types: List<String> = emptyList(),
        val reviews: List<ReviewDto> = emptyList(),
    ) {
        fun toPlaceResult(photoUrl: String, photoReference: String) = PlaceResult(
            placeId = id,
            name = displayName.text,
            photoUrl = photoUrl,
            rating = rating,
            reviewCount = userRatingCount,
            address = formattedAddress,
            lat = location.latitude,
            lng = location.longitude,
            primaryType = primaryType,
            types = types,
            photoReference = photoReference,
            reviews = reviews.take(3).map { r ->
                PlaceReview(
                    authorName = r.authorAttribution?.displayName ?: "",
                    rating = r.rating,
                    text = if (r.originalText?.languageCode == "es") r.originalText.text
                           else r.text?.text ?: "",
                    relativeTime = r.relativeTime,
                )
            },
        )
    }

    @Serializable
    private data class DisplayName(val text: String = "")

    @Serializable
    private data class PhotoDto(val name: String = "")

    @Serializable
    private data class ReviewDto(
        val rating: Int = 0,
        @SerialName("relativePublishTimeDescription") val relativeTime: String = "",
        val text: ReviewText? = null,
        val originalText: ReviewText? = null,
        val authorAttribution: AuthorAttribution? = null,
    )

    @Serializable
    private data class ReviewText(val text: String = "", val languageCode: String = "")

    @Serializable
    private data class AuthorAttribution(val displayName: String = "")

    // ─── Helpers ──────────────────────────────────────────────────────────────

    override fun resolvePhotoUrl(resourceName: String): String =
        "https://places.googleapis.com/v1/$resourceName/media?maxWidthPx=$PHOTO_WIDTH&key=$apiKey"

    private fun PlaceDto.resolvedPhotoUrl(): String =
        photos.firstOrNull()?.name?.let { resolvePhotoUrl(it) } ?: ""

    private fun PlaceDto.photoReference(): String =
        photos.firstOrNull()?.name ?: ""

    // ─── PlacesApiClient ──────────────────────────────────────────────────────

    override open suspend fun searchDestinations(region: String): List<PlaceResult> {
        val response: PlacesResponse = httpClient.post("$BASE_URL:searchText") {
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", FIELD_MASK)
            contentType(ContentType.Application.Json)
            setBody(TextSearchRequest(textQuery = region))
        }.body()
        return response.places.map { it.toPlaceResult(it.resolvedPhotoUrl(), it.photoReference()) }
    }

    override open suspend fun searchPois(lat: Double, lng: Double, types: List<String>): List<PlaceResult> {
        return coroutineScope {
            types
                .map { singleType -> async { fetchNearby(lat, lng, listOf(singleType)) } }
                .awaitAll()
                .flatten()
                .distinctBy { it.placeId }
                .sortedByDescending { it.rating }
        }
    }

    override open suspend fun searchNearby(query: String, lat: Double, lng: Double): List<PlaceResult> {
        val response: PlacesResponse = httpClient.post("$BASE_URL:searchText") {
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", FIELD_MASK)
            contentType(ContentType.Application.Json)
            setBody(
                TextSearchWithBiasRequest(
                    textQuery = query,
                    locationBias = LocationBias(
                        circle = Circle(center = LatLng(lat, lng), radius = 5000.0)
                    ),
                )
            )
        }.body()
        return response.places.map { it.toPlaceResult(it.resolvedPhotoUrl(), it.photoReference()) }
    }

    override open suspend fun fetchPlaceDetails(placeId: String): PlaceResult {
        val response: PlaceDto = httpClient.get("$BASE_URL/$placeId") {
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", DETAIL_FIELD_MASK)
        }.body()
        return response.toPlaceResult(response.resolvedPhotoUrl(), response.photoReference())
    }

    override fun getCachedPlace(placeId: String): PlaceResult? = null

    override fun isCachedPlaceDetailed(placeId: String): Boolean = false

    override fun rememberPlace(place: PlaceResult, hasFullDetails: Boolean): PlaceResult = place

    private suspend fun fetchNearby(lat: Double, lng: Double, includedTypes: List<String>): List<PlaceResult> {
        val response: PlacesResponse = httpClient.post("$BASE_URL:searchNearby") {
            header("X-Goog-Api-Key", apiKey)
            header("X-Goog-FieldMask", FIELD_MASK)
            contentType(ContentType.Application.Json)
            setBody(
                NearbySearchRequest(
                    locationRestriction = LocationRestriction(
                        circle = Circle(center = LatLng(lat, lng), radius = 5000.0)
                    ),
                    includedTypes = includedTypes,
                )
            )
        }.body()
        return response.places.map { it.toPlaceResult(it.resolvedPhotoUrl(), it.photoReference()) }
    }

}
