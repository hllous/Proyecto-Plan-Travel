package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.ui.components.SectionCard

@Suppress("UNUSED_PARAMETER")
@Composable
fun DestinationScreen(viewModel: MainViewModel, navController: NavHostController) {
    val regions by viewModel.regions.collectAsState(initial = emptyList())
    val selectedRegion by viewModel.selectedRegion.collectAsState(initial = null)
    val recommendations by viewModel.recommendations.collectAsState(initial = emptyList())
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionCard(title = "Regiones de Argentina") {
                if (regions.isEmpty()) {
                    Text("Cargando...")
                } else {
                    regions.forEach { region ->
                        FilterChip(
                            selected = region == selectedRegion,
                            onClick = { viewModel.selectRegion(region) },
                            label = { Text(region) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }
        if (recommendations.isEmpty() && selectedRegion != null) {
            item {
                SectionCard(title = "Sin destinos disponibles") {
                    Text("No hay destinos para esta region")
                }
            }
        } else if (recommendations.isNotEmpty()) {
            items(recommendations) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.width(20.dp))
                            Text(rec.destination, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(rec.recommendation)
                    }
                }
            }
        }
    }
}
