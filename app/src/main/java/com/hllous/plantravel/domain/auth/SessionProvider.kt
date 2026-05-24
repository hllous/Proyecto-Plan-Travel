package com.hllous.plantravel.domain.auth

interface SessionProvider {
    val userId: String?
    val displayName: String?
}
