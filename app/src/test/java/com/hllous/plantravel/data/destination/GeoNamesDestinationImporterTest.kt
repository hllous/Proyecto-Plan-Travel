package com.hllous.plantravel.data.destination

import org.junit.Assert.assertEquals
import org.junit.Test

class GeoNamesDestinationImporterTest {

    @Test
    fun loadsProvinceToRegionMappingsFromCsvResource() {
        val mapper = ProvinceRegionMapper.fromCsvResource("destinations/argentina_province_regions.csv")

        assertEquals("Patagonia", mapper.regionFor("Neuquén"))
        assertEquals("Cuyo", mapper.regionFor("Mendoza"))
        assertEquals("Córdoba", mapper.regionFor("Córdoba"))
        assertEquals("Buenos Aires", mapper.regionFor("CABA"))
    }

    @Test
    fun importsOnlyArgentinePopulatedPlacesWithPopulationAtLeastOneThousand() {
        val importer = GeoNamesDestinationImporter(
            provinceRegionMapper = ProvinceRegionMapper(mapOf("Río Negro" to "Patagonia")),
        )

        val imported = importer.importRows(
            listOf(
                geoNamesRow(
                    geonameId = "1",
                    name = "San Carlos de Bariloche",
                    asciiname = "San Carlos de Bariloche",
                    latitude = "-41.1335",
                    longitude = "-71.3103",
                    featureClass = "P",
                    featureCode = "PPL",
                    countryCode = "AR",
                    adminName1 = "Río Negro",
                    population = "135000",
                ),
                geoNamesRow(
                    geonameId = "2",
                    name = "Tiny Hamlet",
                    asciiname = "Tiny Hamlet",
                    latitude = "-41.0",
                    longitude = "-71.0",
                    featureClass = "P",
                    featureCode = "PPL",
                    countryCode = "AR",
                    adminName1 = "Río Negro",
                    population = "999",
                ),
                geoNamesRow(
                    geonameId = "3",
                    name = "Santiago",
                    asciiname = "Santiago",
                    latitude = "-33.4",
                    longitude = "-70.6",
                    featureClass = "P",
                    featureCode = "PPL",
                    countryCode = "CL",
                    adminName1 = "Región Metropolitana",
                    population = "5000000",
                ),
                geoNamesRow(
                    geonameId = "4",
                    name = "Nahuel Huapi National Park",
                    asciiname = "Nahuel Huapi National Park",
                    latitude = "-41.0",
                    longitude = "-71.5",
                    featureClass = "L",
                    featureCode = "PRK",
                    countryCode = "AR",
                    adminName1 = "Río Negro",
                    population = "0",
                ),
            ),
        )

        assertEquals(
            listOf(
                ImportedDestination(
                    sourceId = "1",
                    name = "San Carlos de Bariloche",
                    province = "Río Negro",
                    region = "Patagonia",
                    countryCode = "AR",
                    lat = -41.1335,
                    lng = -71.3103,
                    population = 135000,
                ),
            ),
            imported,
        )
    }

    @Test
    fun keepsOnlyOneDestinationPerGeoNamesSourceId() {
        val importer = GeoNamesDestinationImporter(
            provinceRegionMapper = ProvinceRegionMapper(mapOf("Río Negro" to "Patagonia")),
        )

        val imported = importer.importRows(
            listOf(
                geoNamesRow(
                    geonameId = "1",
                    name = "Bariloche",
                    asciiname = "Bariloche",
                    latitude = "-41.1335",
                    longitude = "-71.3103",
                    featureClass = "P",
                    featureCode = "PPL",
                    countryCode = "AR",
                    adminName1 = "Río Negro",
                    population = "135000",
                ),
                geoNamesRow(
                    geonameId = "1",
                    name = "Bariloche Duplicate",
                    asciiname = "Bariloche Duplicate",
                    latitude = "-41.1335",
                    longitude = "-71.3103",
                    featureClass = "P",
                    featureCode = "PPL",
                    countryCode = "AR",
                    adminName1 = "Río Negro",
                    population = "135000",
                ),
            ),
        )

        assertEquals(1, imported.size)
        assertEquals("Bariloche", imported.single().name)
    }

    private fun geoNamesRow(
        geonameId: String,
        name: String,
        asciiname: String,
        latitude: String,
        longitude: String,
        featureClass: String,
        featureCode: String,
        countryCode: String,
        adminName1: String,
        population: String,
    ): String = listOf(
        geonameId,
        name,
        asciiname,
        "",
        latitude,
        longitude,
        featureClass,
        featureCode,
        countryCode,
        "",
        adminName1,
        "",
        "",
        "",
        population,
        "",
        "",
        "",
        "",
    ).joinToString("\t")
}
