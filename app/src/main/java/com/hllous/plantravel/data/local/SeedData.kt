package com.hllous.plantravel.data.local

import com.hllous.plantravel.data.local.entity.DestinationRecommendationEntity

object SeedData {
    val argentinaRecommendations = listOf(
        DestinationRecommendationEntity(region = "NOA", destination = "Salta", recommendation = "Cafayate, Tren a las Nubes y empanadas saltenas."),
        DestinationRecommendationEntity(region = "NOA", destination = "Jujuy", recommendation = "Purmamarca y Cerro de los Siete Colores."),
        DestinationRecommendationEntity(region = "NEA", destination = "Iguazu", recommendation = "Cataratas y circuito superior al amanecer."),
        DestinationRecommendationEntity(region = "Cuyo", destination = "Mendoza", recommendation = "Bodegas, alta montana y termas."),
        DestinationRecommendationEntity(region = "Patagonia", destination = "Bariloche", recommendation = "Circuito Chico y senderismo en cerros."),
        DestinationRecommendationEntity(region = "Patagonia", destination = "El Calafate", recommendation = "Glaciar Perito Moreno y navegacion."),
        DestinationRecommendationEntity(region = "Centro", destination = "Cordoba", recommendation = "Sierras, rios y gastronomia local."),
        DestinationRecommendationEntity(region = "Litoral", destination = "Rosario", recommendation = "Costanera, monumentos y vida nocturna."),
        DestinationRecommendationEntity(region = "Buenos Aires", destination = "Mar del Plata", recommendation = "Playas, puerto y actividades familiares.")
    )
}

