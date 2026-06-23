package com.hllous.plantravel

import com.hllous.plantravel.domain.destination.DestinationPhotoResolver
import com.hllous.plantravel.domain.model.StoredDestination

class FakeDestinationPhotoResolver(
    private val onResolve: suspend (StoredDestination) -> String? = { "https://fake.photo/destination.jpg" },
) : DestinationPhotoResolver {
    var resolveCallCount = 0

    override suspend fun resolve(destination: StoredDestination): String? {
        resolveCallCount++
        return onResolve(destination)
    }
}
