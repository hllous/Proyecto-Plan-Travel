package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hllous.plantravel.presentation.auth.AuthViewModel

@Composable
fun ProfileScreen(authViewModel: AuthViewModel, onLogout: () -> Unit) {
    // Placeholder — implemented in #31
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
