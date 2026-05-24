package com.hllous.plantravel

import com.hllous.plantravel.domain.auth.SessionProvider

class FakeSessionProvider(
    override val userId: String? = null,
    override val displayName: String? = null
) : SessionProvider
