package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun HomeScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bienvenido a Plan Travel", style = MaterialTheme.typography.headlineSmall)
                    Text("Organiza y coordina tus viajes en grupo de forma simple y eficiente.")
                }
            }
        }
        item {
            Text("Funcionalidades principales", style = MaterialTheme.typography.titleMedium)
        }
        item {
            HomeCard(
                icon = Icons.Default.People,
                title = "Grupos de viaje",
                subtitle = "Crea grupos con roles de admin y usuarios",
                onClick = { navController.navigate("groups") }
            )
        }
        item {
            HomeCard(
                icon = Icons.Default.QrCode,
                title = "Invitaciones",
                subtitle = "Comparte QR, codigo o link para invitar",
                onClick = { navController.navigate("groups") }
            )
        }
        item {
            HomeCard(
                icon = Icons.Default.LocationOn,
                title = "Destinos",
                subtitle = "Explora recomendaciones por region",
                onClick = { navController.navigate("destinations") }
            )
        }
        item {
            HomeCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Gastos",
                subtitle = "Reparte gastos por consumo real",
                onClick = { navController.navigate("gastos") }
            )
        }
        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun HomeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick) {
                Text("Ir")
            }
        }
    }
}
