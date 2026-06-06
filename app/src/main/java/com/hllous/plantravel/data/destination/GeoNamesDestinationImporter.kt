package com.hllous.plantravel.data.destination

data class ImportedDestination(
    val source: String = "geonames",
    val sourceId: String,
    val name: String,
    val province: String,
    val region: String,
    val countryCode: String,
    val lat: Double,
    val lng: Double,
    val population: Int,
)

class ProvinceRegionMapper(
    private val mapping: Map<String, String>,
) {
    fun regionFor(province: String): String? = mapping[province]

    companion object {
        fun argentinaDefaults(): ProvinceRegionMapper = ProvinceRegionMapper(
            mapOf(
                "Buenos Aires" to "Buenos Aires",
                "CABA" to "Buenos Aires",
                "Catamarca" to "Noroeste",
                "Chaco" to "Litoral",
                "Chubut" to "Patagonia",
                "Córdoba" to "Córdoba",
                "Corrientes" to "Litoral",
                "Entre Ríos" to "Litoral",
                "Formosa" to "Litoral",
                "Jujuy" to "Noroeste",
                "La Pampa" to "Patagonia",
                "La Rioja" to "Noroeste",
                "Mendoza" to "Cuyo",
                "Misiones" to "Litoral",
                "Neuquén" to "Patagonia",
                "Río Negro" to "Patagonia",
                "Salta" to "Noroeste",
                "San Juan" to "Cuyo",
                "San Luis" to "Cuyo",
                "Santa Cruz" to "Patagonia",
                "Santa Fe" to "Litoral",
                "Santiago del Estero" to "Noroeste",
                "Tierra del Fuego" to "Patagonia",
                "Tucumán" to "Noroeste",
            ),
        )

        fun fromCsvResource(resourcePath: String): ProvinceRegionMapper {
            val stream = ProvinceRegionMapper::class.java.classLoader
                ?.getResourceAsStream(resourcePath)
                ?: error("Missing province-region resource: $resourcePath")
            val mapping = stream.bufferedReader().useLines { lines ->
                lines
                    .filter { it.isNotBlank() }
                    .drop(1)
                    .associate { line ->
                        val columns = line.split(',')
                        columns[0] to columns[1]
                    }
            }
            return ProvinceRegionMapper(mapping)
        }
    }
}

class GeoNamesDestinationImporter(
    private val provinceRegionMapper: ProvinceRegionMapper,
    private val minimumPopulation: Int = 1_000,
) {
    fun importRows(rows: List<String>): List<ImportedDestination> =
        rows
            .mapNotNull(::parseRow)
            .distinctBy { it.source to it.sourceId }

    private fun parseRow(row: String): ImportedDestination? {
        val columns = row.split('\t')
        if (columns.size < 15) return null

        val featureClass = columns[6]
        val countryCode = columns[8]
        val province = columns[10]
        val population = columns[14].toIntOrNull() ?: return null

        if (featureClass != "P") return null
        if (countryCode != "AR") return null
        if (population < minimumPopulation) return null

        val region = provinceRegionMapper.regionFor(province) ?: return null

        return ImportedDestination(
            sourceId = columns[0],
            name = columns[1],
            province = province,
            region = region,
            countryCode = countryCode,
            lat = columns[4].toDouble(),
            lng = columns[5].toDouble(),
            population = population,
        )
    }
}
