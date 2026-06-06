package com.hllous.plantravel.data.destination

import com.hllous.plantravel.domain.model.StoredDestination

object DestinationFallbackImage {
    private const val Prefix = "fallback://region/"

    private val regionSlugByName = linkedMapOf(
        "Patagonia" to "patagonia",
        "Cuyo" to "cuyo",
        "Noroeste" to "noroeste",
        "Litoral" to "litoral",
        "Buenos Aires" to "buenos_aires",
        "Córdoba" to "cordoba",
    )

    fun tokenFor(destination: StoredDestination): String = tokenForRegion(destination.region)

    fun tokenForRegion(region: String): String =
        Prefix + (regionSlugByName[region] ?: "argentina")

    fun regionSlugFromToken(url: String): String? =
        url.removePrefix(Prefix).takeIf { url.startsWith(Prefix) && it.isNotBlank() }
}
