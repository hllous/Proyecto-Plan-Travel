package com.hllous.plantravel.data.destination

object CuratedDestinations {
    val byRegion: Map<String, List<String>> = mapOf(
        "Patagonia" to listOf(
            "Bariloche", "El Bolsón", "San Martín de los Andes",
            "Puerto Madryn", "Esquel", "Neuquén", "Comodoro Rivadavia",
        ),
        "Cuyo" to listOf("Mendoza", "San Rafael", "Malargüe", "San Juan"),
        "Noroeste" to listOf("Salta", "Jujuy", "Tucumán", "Cafayate"),
        "Litoral" to listOf("Iguazú", "Corrientes", "Posadas", "Colón"),
        "Buenos Aires" to listOf("Mar del Plata", "Villa Gesell", "Pinamar", "Tandil", "La Plata"),
        "Córdoba" to listOf("Villa Carlos Paz", "La Cumbrecita", "Mina Clavero", "Alta Gracia"),
    )
}
