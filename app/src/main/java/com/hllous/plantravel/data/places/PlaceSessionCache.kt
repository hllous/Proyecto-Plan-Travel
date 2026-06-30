package com.hllous.plantravel.data.places

import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.places.mergePlaceResults

class PlaceSessionCache(
    private val maxEntries: Int = 100,
) {
    data class Entry(
        val place: PlaceResult,
        val hasFullDetails: Boolean,
    )

    private val entries = LinkedHashMap<String, Entry>(maxEntries, 0.75f, true)
    private val lock = Any()

    fun get(placeId: String): Entry? = synchronized(lock) { entries[placeId] }

    fun remember(place: PlaceResult, hasFullDetails: Boolean): Entry = synchronized(lock) {
        val current = entries[place.placeId]
        val merged = mergePlaceResults(current?.place, place) ?: place
        val updated = Entry(
            place = merged,
            hasFullDetails = current?.hasFullDetails == true || hasFullDetails,
        )
        entries[place.placeId] = updated
        trimToSize()
        updated
    }

    private fun trimToSize() {
        while (entries.size > maxEntries) {
            val eldestKey = entries.entries.firstOrNull()?.key ?: return
            entries.remove(eldestKey)
        }
    }
}
