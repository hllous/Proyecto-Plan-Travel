package com.hllous.plantravel.domain.destination

import com.hllous.plantravel.domain.model.StoredDestination

interface DestinationPhotoResolver {
    suspend fun resolve(destination: StoredDestination): String?
}
