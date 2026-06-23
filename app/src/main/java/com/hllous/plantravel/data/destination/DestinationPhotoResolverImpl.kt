package com.hllous.plantravel.data.destination

import android.util.Log
import com.hllous.plantravel.domain.destination.DestinationPhotoResolver
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.places.PlacesApiClient
import com.hllous.plantravel.domain.repository.TravelRepository
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DestinationPhotoResolverImpl @Inject constructor(
    private val placesApiClient: PlacesApiClient,
    private val repository: TravelRepository,
) : DestinationPhotoResolver {

    internal var googleFetcher: suspend (StoredDestination) -> String? = ::fetchGooglePhotoUrl
    internal var wikipediaFetcher: suspend (StoredDestination) -> String? = ::fetchWikipediaPhotoUrl

    override suspend fun resolve(destination: StoredDestination): String? {
        val googleRef = googleFetcher(destination)?.takeIf { it.isNotBlank() }
        if (googleRef != null) {
            val displayUrl = if (googleRef.startsWith("places/")) {
                placesApiClient.resolvePhotoUrl(googleRef)
            } else {
                googleRef
            }
            if (destination.id.isNotBlank()) {
                repository.updateDestinationPhoto(
                    destinationId = destination.id,
                    googlePhotoUrl = googleRef,
                    displayPhotoUrl = googleRef,
                )
            }
            return displayUrl
        }

        val wikipediaUrl = wikipediaFetcher(destination)?.takeIf { it.isNotBlank() }
        if (wikipediaUrl != null) {
            if (destination.id.isNotBlank()) {
                repository.updateDestinationPhoto(
                    destinationId = destination.id,
                    wikipediaTitle = destination.name,
                    wikipediaPhotoUrl = wikipediaUrl,
                    displayPhotoUrl = wikipediaUrl,
                )
            }
            return wikipediaUrl
        }

        return DestinationFallbackImage.tokenFor(destination)
    }

    private suspend fun fetchGooglePhotoUrl(destination: StoredDestination): String? {
        val query = "${destination.name}, ${destination.province}, Argentina"
        return runCatching {
            placesApiClient.searchDestinations(query)
                .asSequence()
                .filter(::isLocalityLike)
                .filter { it.photoUrl.isNotBlank() }
                .sortedByDescending { scoreDestinationMatch(it, destination) }
                .firstOrNull()
                ?.let { it.photoReference.ifBlank { it.photoUrl } }
        }.onFailure { e ->
            Log.w(TAG, "Google Places fetch failed for '${destination.name}': ${e.message}")
        }.getOrNull()
    }

    private fun isLocalityLike(place: PlaceResult): Boolean {
        val types = buildSet {
            place.primaryType?.let(::add)
            addAll(place.types)
        }
        return types.any {
            it == "locality" ||
                it == "postal_town" ||
                it == "administrative_area_level_3" ||
                it == "sublocality"
        }
    }

    private fun scoreDestinationMatch(place: PlaceResult, destination: StoredDestination): Int {
        val name = DestinationTextNormalizer.normalize(place.name)
        val address = DestinationTextNormalizer.normalize(place.address)
        val destinationName = destination.normalizedName
        val province = DestinationTextNormalizer.normalize(destination.province)

        var score = 0
        if (name.contains(destinationName)) score += 3
        if (address.contains(destinationName)) score += 2
        if (address.contains(province)) score += 1
        return score
    }

    private suspend fun fetchWikipediaPhotoUrl(destination: StoredDestination): String? = withContext(Dispatchers.IO) {
        runCatching {
            // Step 1: direct name lookup on Spanish Wikipedia.
            val direct = wikipediaThumbnail(destination.name, destination)
            if (direct != null) return@runCatching direct

            // Step 2: parenthetical disambiguation form → "Cervantes (Río Negro)".
            val parenthetical = wikipediaThumbnail("${destination.name} (${destination.province})", destination)
            if (parenthetical != null) return@runCatching parenthetical

            // Step 3: coordinate geosearch.
            wikipediaThumbnailByGeosearch(destination)
        }.getOrNull()
    }

    private fun wikipediaThumbnail(title: String, destination: StoredDestination? = null): String? {
        val encoded = URLEncoder.encode(title, "UTF-8").replace("+", "_")
        val conn = URL("https://es.wikipedia.org/api/rest_v1/page/summary/$encoded")
            .openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "PlanTravelApp/1.0 (Android)")
        conn.connect()
        if (conn.responseCode != 200) return null
        val json = JSONObject(conn.inputStream.bufferedReader().readText())
        if (json.optString("type") == "disambiguation") return null

        if (destination != null && !isWikipediaArticleRelevant(json, destination)) return null

        val thumbnailUrl = json.optJSONObject("thumbnail")?.optString("source")
            ?.takeIf { it.isNotBlank() } ?: return null

        // SVG thumbnails are location maps, flags, or diagrams — not destination photos.
        if (thumbnailUrl.contains(".svg", ignoreCase = true)) return null

        return thumbnailUrl
    }

    private fun isWikipediaArticleRelevant(json: JSONObject, destination: StoredDestination): Boolean {
        val coords = json.optJSONObject("coordinates")
        if (coords != null) {
            val lat = coords.optDouble("lat", Double.NaN)
            val lon = coords.optDouble("lon", Double.NaN)
            if (!lat.isNaN() && !lon.isNaN()) {
                return haversineKm(destination.lat, destination.lng, lat, lon) <= 500.0
            }
        }
        // No coordinates: accept only if the article text mentions Argentina or the province.
        val text = (json.optString("description", "") + " " + json.optString("extract", ""))
            .lowercase()
        val province = DestinationTextNormalizer.normalize(destination.province).lowercase()
        return text.contains("argentina") || text.contains(province)
    }

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = Math.sin(dLat / 2).let { it * it } +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2).let { it * it }
        return r * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    }

    private fun wikipediaThumbnailByGeosearch(destination: StoredDestination): String? {
        val conn = URL(
            "https://es.wikipedia.org/w/api.php?action=query&list=geosearch" +
                "&gscoord=${destination.lat}|${destination.lng}&gsradius=10000&gslimit=5&format=json"
        ).openConnection() as HttpURLConnection
        conn.setRequestProperty("User-Agent", "PlanTravelApp/1.0 (Android)")
        conn.connect()
        if (conn.responseCode != 200) return null

        val results = JSONObject(conn.inputStream.bufferedReader().readText())
            .optJSONObject("query")?.optJSONArray("geosearch") ?: return null
        val destNorm = DestinationTextNormalizer.normalize(destination.name)

        // Prefer titles that begin with the destination name over ones that merely contain it.
        var startsWithMatch: String? = null
        var containsMatch: String? = null
        var firstTitle: String? = null
        for (i in 0 until results.length()) {
            val title = results.optJSONObject(i)?.optString("title") ?: continue
            val norm = DestinationTextNormalizer.normalize(title)
            if (firstTitle == null) firstTitle = title
            if (startsWithMatch == null && norm.startsWith(destNorm)) startsWithMatch = title
            if (containsMatch == null && norm.contains(destNorm)) containsMatch = title
        }
        val best = startsWithMatch ?: return null
        return wikipediaThumbnail(best, destination)
    }

    private companion object {
        private const val TAG = "DestinationPhotoResolver"
    }
}
