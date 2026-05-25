package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.greetingForHour
import java.time.LocalTime

@Composable
fun HomeScreen(
    navController: NavHostController,
    displayName: String,
    groupViewModel: GroupViewModel,
) {
    val groups by groupViewModel.groups.collectAsState()
    val hour = LocalTime.now().hour
    val greeting = "${greetingForHour(hour)}, ${displayName.ifBlank { "viajero" }}"

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = greeting,
                style = MaterialTheme.typography.displayMedium,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Medium,
            )
        }

        if (groups.isEmpty()) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Todavía no tenés un grupo de viaje",
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FrauncesFamily,
                        )
                        Text(
                            text = "Creá un grupo o pedile a alguien que te invite para empezar a planear.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { navController.navigate("groups") },
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Ir a Grupos")
                        }
                    }
                }
            }
        } else {
            val activeGroup = groups.first()

            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = activeGroup.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontFamily = FrauncesFamily,
                        )
                        Text(
                            text = "${activeGroup.memberCount} miembro${if (activeGroup.memberCount != 1) "s" else ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilledTonalButton(onClick = { navController.navigate("gastos") }) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Agregar gasto")
                    }
                    FilledTonalButton(onClick = { navController.navigate("group_detail/${activeGroup.id}") }) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Invitar")
                    }
                    FilledTonalButton(onClick = { navController.navigate("destinations") }) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.padding(end = 4.dp)
                        )
                        Text("Destinos")
                    }
                }
            }

            item {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(contextualTiles(navController)) { tile ->
                        ElevatedCard(
                            onClick = tile.onClick,
                            modifier = Modifier.width(160.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(tile.emoji, style = MaterialTheme.typography.titleLarge)
                                Text(
                                    text = tile.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

private data class ContextualTile(val emoji: String, val title: String, val onClick: () -> Unit)

private fun contextualTiles(navController: NavHostController) = listOf(
    ContextualTile("💸", "Gastos sin dividir") { navController.navigate("gastos") },
    ContextualTile("🏔️", "Explorá destinos") { navController.navigate("destinations") },
    ContextualTile("👥", "Invitá personas") { navController.navigate("groups") },
)
