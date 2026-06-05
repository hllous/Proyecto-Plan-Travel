package com.hllous.plantravel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.ui.theme.FrauncesFamily

private val REGIONS = listOf("Patagonia", "Cuyo", "Noroeste", "Litoral", "Buenos Aires", "Córdoba")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationScreen(
    viewModel: DestinationViewModel,
    navController: NavHostController,
) {
    val tripDestination by viewModel.tripDestination.collectAsState()

    if (tripDestination is TripDestinationState.Set) {
        Level2Placeholder()
        return
    }

    Level1BrowseContent(viewModel = viewModel)
}

@Composable
private fun Level2Placeholder() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Level 2 — próximamente en #52",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Level1BrowseContent(viewModel: DestinationViewModel) {
    val regionResults by viewModel.regionResults.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val activePoll by viewModel.activePoll.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var selectedRegion by rememberSaveable { mutableStateOf<String?>(null) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var pollBannerDismissed by rememberSaveable { mutableStateOf(false) }
    var selectedPlace by remember { mutableStateOf<PlaceResult?>(null) }

    val showPollBanner = activePoll != null && !pollBannerDismissed

    val displayedResults: UiState<List<PlaceResult>> = when {
        isSearchActive -> searchResults
        selectedRegion != null -> regionResults
        else -> UiState.Success(emptyList())
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Destinos",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Active poll banner ────────────────────────────────────────────
            AnimatedVisibility(
                visible = showPollBanner,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                PollBanner(onDismiss = { pollBannerDismissed = true })
            }

            // ── Search bar ────────────────────────────────────────────────────
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank()) {
                        isSearchActive = true
                        selectedRegion = null
                        viewModel.search(q)
                    } else {
                        isSearchActive = false
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Buscar destino…") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = {
                            searchQuery = ""
                            isSearchActive = false
                        }) {
                            Icon(Icons.Default.Close, contentDescription = "Limpiar búsqueda")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
            )

            // ── Region chip row ───────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Recovery chip when banner is dismissed
                if (activePoll != null && pollBannerDismissed) {
                    item {
                        FilterChip(
                            selected = false,
                            onClick = { pollBannerDismissed = false },
                            label = { Text("Encuesta activa") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.HowToVote,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                iconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            ),
                        )
                    }
                }

                items(REGIONS) { region ->
                    FilterChip(
                        selected = selectedRegion == region,
                        onClick = {
                            if (selectedRegion == region) {
                                selectedRegion = null
                            } else {
                                selectedRegion = region
                                isSearchActive = false
                                searchQuery = ""
                                viewModel.selectRegion(region)
                            }
                        },
                        label = { Text(region) },
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))

            // ── Results ───────────────────────────────────────────────────────
            when (val state = displayedResults) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                is UiState.Success -> {
                    if (state.data.isEmpty() && (selectedRegion != null || isSearchActive)) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Sin resultados",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else if (state.data.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Seleccioná una región para explorar destinos",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            contentPadding = PaddingValues(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            items(state.data) { place ->
                                DestinationCard(
                                    place = place,
                                    onClick = { selectedPlace = place },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Destination bottom sheet ──────────────────────────────────────────────
    selectedPlace?.let { place ->
        DestinationBottomSheet(
            place = place,
            isAdmin = currentMember?.role == MemberRole.ADMIN,
            viewModel = viewModel,
            onDismiss = { selectedPlace = null },
        )
    }
}

@Composable
private fun PollBanner(onDismiss: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Default.HowToVote,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Hay una encuesta activa — ¡participá!",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.weight(1f),
            )
            IconButton(onClick = onDismiss) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun DestinationCard(
    place: PlaceResult,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            AsyncImage(
                model = place.photoUrl,
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
            )
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = "%.1f".format(place.rating),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DestinationBottomSheet(
    place: PlaceResult,
    isAdmin: Boolean,
    viewModel: DestinationViewModel,
    onDismiss: () -> Unit,
) {
    val tripDestination by viewModel.tripDestination.collectAsState()
    val sheetState = rememberModalBottomSheetState()

    var showPollPromptDialog by remember { mutableStateOf(false) }
    var showReplaceDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            AsyncImage(
                model = place.photoUrl,
                contentDescription = place.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "%.1f".format(place.rating),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "(${place.reviewCount} reseñas)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (place.address.isNotBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = place.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                if (isAdmin) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = {
                            when (tripDestination) {
                                is TripDestinationState.None -> showPollPromptDialog = true
                                is TripDestinationState.Set -> showReplaceDialog = true
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Establecer como destino")
                    }
                }
            }
        }
    }

    // ── "Create poll first?" dialog ───────────────────────────────────────────
    if (showPollPromptDialog) {
        AlertDialog(
            onDismissRequest = { showPollPromptDialog = false },
            icon = { Icon(Icons.Default.HowToVote, contentDescription = null) },
            title = { Text("¿Crear una encuesta primero?") },
            text = {
                Text(
                    "Podés crear una encuesta para que el grupo vote antes de elegir el destino, " +
                        "o establecerlo directamente.",
                )
            },
            confirmButton = {
                // TODO(#54): navigate to poll creation screen
                TextButton(onClick = { showPollPromptDialog = false }) {
                    Text("Crear encuesta")
                }
            },
            dismissButton = {
                Button(onClick = {
                    showPollPromptDialog = false
                    viewModel.setTripDestination(place)
                    onDismiss()
                }) {
                    Text("Saltear y establecer")
                }
            },
        )
    }

    // ── "Replace existing destination?" dialog ────────────────────────────────
    if (showReplaceDialog) {
        val currentName = (tripDestination as? TripDestinationState.Set)?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("¿Reemplazar destino?") },
            text = {
                Text(
                    "El destino actual es \"$currentName\". ¿Querés reemplazarlo por \"${place.name}\"?",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showReplaceDialog = false
                    viewModel.setTripDestination(place)
                    onDismiss()
                }) {
                    Text("Reemplazar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }
}
