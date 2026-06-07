package com.hllous.plantravel.ui.screens

import android.content.Intent
import android.net.Uri
import com.hllous.plantravel.presentation.itinerary.ItineraryEventDraft
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import com.hllous.plantravel.R
import com.hllous.plantravel.data.destination.DestinationFallbackImage
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.RankedRecommendations
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.ui.theme.FrauncesFamily

private val REGIONS = listOf("Patagonia", "Cuyo", "Noroeste", "Litoral", "Buenos Aires", "Córdoba")
private data class PoiCategory(val label: String, val icon: ImageVector)
private val POI_CATEGORIES = listOf(
    PoiCategory("Alojamiento", Icons.Default.Hotel),
    PoiCategory("Gastronomía", Icons.Default.Restaurant),
    PoiCategory("Actividades", Icons.Default.LocalActivity),
    PoiCategory("Naturaleza", Icons.Default.Park),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DestinationScreen(
    viewModel: DestinationViewModel,
    navController: NavHostController,
) {
    val tripDestination by viewModel.tripDestination.collectAsState()
    var overrideToLevel1 by rememberSaveable { mutableStateOf(false) }

    val destination = tripDestination as? TripDestinationState.Set
    if (destination != null && !overrideToLevel1) {
        Level2Content(
            viewModel = viewModel,
            destination = destination,
            navController = navController,
            onChangeDestination = { overrideToLevel1 = true },
        )
    } else {
        Level1BrowseContent(viewModel = viewModel, navController = navController)
    }
}

// ─── Level 2 ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Level2Content(
    viewModel: DestinationViewModel,
    destination: TripDestinationState.Set,
    navController: NavHostController,
    onChangeDestination: () -> Unit,
) {
    val poisByCategory by viewModel.poisByCategory.collectAsState()
    val activePoll by viewModel.activePoll.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()

    var selectedCategory by rememberSaveable { mutableStateOf(POI_CATEGORIES.first().label) }
    var selectedPoi by remember { mutableStateOf<PlaceResult?>(null) }

    LaunchedEffect(Unit) {
        viewModel.selectPoiCategory(selectedCategory)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = destination.name,
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                actions = {
                    TextButton(onClick = { navController.navigate("itinerary") }) {
                        Text("Ver itinerario")
                    }
                    TextButton(onClick = onChangeDestination) {
                        Text("Cambiar")
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("poll_detail") },
                icon = { Icon(Icons.Default.HowToVote, contentDescription = null) },
                text = { Text(if (activePoll != null) "Ver encuesta" else "Crear encuesta") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ── Category chip row ─────────────────────────────────────────────
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(POI_CATEGORIES) { cat ->
                    FilterChip(
                        selected = selectedCategory == cat.label,
                        onClick = {
                            selectedCategory = cat.label
                            viewModel.selectPoiCategory(cat.label)
                        },
                        label = { Text(cat.label) },
                        leadingIcon = {
                            Icon(
                                cat.icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }
            }

            HorizontalDivider()

            // ── Results ───────────────────────────────────────────────────────
            when (val state = poisByCategory) {
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
                    PoiGrid(
                        ranked = state.data,
                        onPoiClick = { selectedPoi = it },
                    )
                }
            }
        }
    }

    // ── POI bottom sheet ──────────────────────────────────────────────────────
    selectedPoi?.let { poi ->
        PoiBottomSheet(
            place = poi,
            hasActivePoll = activePoll != null,
            onDismiss = { selectedPoi = null },
            onAddToItinerary = {
                val draft = ItineraryEventDraft(
                    name = poi.name,
                    description = poi.address,
                    placeId = poi.placeId,
                )
                val draftJson = Uri.encode(Json.encodeToString(draft))
                navController.navigate("itinerary?draft=$draftJson")
                selectedPoi = null
            },
            onAddToPoll = {
                viewModel.addPoiToPoll(poi) {
                    navController.navigate("poll_detail")
                    selectedPoi = null
                }
            },
            onCreatePoll = {
                viewModel.createPollWithPoi(poi) {
                    navController.navigate("poll_detail")
                    selectedPoi = null
                }
            },
        )
    }
}

private data class PoiItem(val place: PlaceResult, val isTop: Boolean)

@Composable
private fun PoiGrid(
    ranked: RankedRecommendations,
    onPoiClick: (PlaceResult) -> Unit,
) {
    if (ranked.top.isEmpty() && ranked.others.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Sin resultados en esta categoría",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        return
    }

    val items = ranked.top.map { PoiItem(it, isTop = true) } +
        ranked.others.map { PoiItem(it, isTop = false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(items, key = { it.place.placeId }) { item ->
            PoiGridCard(
                place = item.place,
                isTop = item.isTop,
                onClick = { onPoiClick(item.place) },
            )
        }
    }
}

@Composable
private fun PoiGridCard(
    place: PlaceResult,
    isTop: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp),
        ) {
            DestinationImage(
                imageUrl = place.photoUrl,
                contentDescription = place.name,
                title = place.name,
                subtitle = place.address,
                icon = Icons.Default.Map,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x80000000)),
                            startY = 60f,
                        ),
                    ),
            )
            Text(
                text = place.name,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(8.dp),
            )
            if (isTop) {
                SuggestionChip(
                    onClick = {},
                    label = { Text("Top", style = MaterialTheme.typography.labelSmall) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PoiBottomSheet(
    place: PlaceResult,
    hasActivePoll: Boolean,
    onDismiss: () -> Unit,
    onAddToItinerary: () -> Unit,
    onAddToPoll: () -> Unit,
    onCreatePoll: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            DestinationImage(
                imageUrl = place.photoUrl,
                contentDescription = place.name,
                title = place.name,
                icon = Icons.Default.Map,
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

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onAddToItinerary,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Añadir al itinerario")
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (hasActivePoll) {
                    OutlinedButton(
                        onClick = onAddToPoll,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Añadir a encuesta")
                    }
                } else {
                    OutlinedButton(
                        onClick = onCreatePoll,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Crear encuesta primero")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = {
                        val uri = Uri.parse("geo:${place.lat},${place.lng}?q=${Uri.encode(place.name)}")
                        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        Icons.Default.Map,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ver en Maps")
                }
            }
        }
    }
}

// ─── Level 1 ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Level1BrowseContent(viewModel: DestinationViewModel, navController: NavHostController) {
    val regionDestinations by viewModel.regionDestinations.collectAsState()
    val searchDestinations by viewModel.searchDestinations.collectAsState()
    val destinationPhotoUrls by viewModel.destinationPhotoUrls.collectAsState()
    val activePoll by viewModel.activePoll.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val regionLoading by viewModel.regionLoading.collectAsState()
    val tripDestination by viewModel.tripDestination.collectAsState()

    var selectedTabIndex by rememberSaveable { mutableStateOf(0) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    var pollBannerDismissed by rememberSaveable { mutableStateOf(false) }
    var selectedDestination by remember { mutableStateOf<StoredDestination?>(null) }

    val showPollBanner = activePoll != null && !pollBannerDismissed

    LaunchedEffect(Unit) {
        viewModel.selectRegion(REGIONS[0])
    }

    val displayedDestinations: List<StoredDestination> = when {
        isSearchActive -> searchDestinations
        else -> regionDestinations
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Destinos",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                    )
                },
                actions = {
                    if (currentMember != null && currentMember?.role != MemberRole.ADMIN) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Text(
                                    "Solo admins pueden configurar",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier.padding(end = 8.dp),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { navController.navigate("poll_detail") },
                icon = { Icon(Icons.Default.HowToVote, contentDescription = null) },
                text = { Text(if (activePoll != null) "Ver encuesta" else "Crear encuesta") },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            AnimatedVisibility(
                visible = showPollBanner,
                enter = expandVertically(),
                exit = shrinkVertically(),
            ) {
                PollBanner(
                    onDismiss = { pollBannerDismissed = true },
                    onTap = { navController.navigate("poll_detail") },
                )
            }

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { q ->
                    searchQuery = q
                    if (q.isNotBlank()) {
                        isSearchActive = true
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

            if (activePoll != null && pollBannerDismissed) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
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

            if (!isSearchActive) {
                ScrollableTabRow(
                    selectedTabIndex = selectedTabIndex,
                    edgePadding = 16.dp,
                ) {
                    REGIONS.forEachIndexed { index, region ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = {
                                selectedTabIndex = index
                                viewModel.selectRegion(region)
                            },
                            text = { Text(region) },
                        )
                    }
                }
            }

            when {
                isSearchActive && displayedDestinations.isEmpty() -> {
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
                }
                isSearchActive -> {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(displayedDestinations) { destination ->
                            CompactDestinationRow(
                                destination = destination,
                                imageUrl = destinationPhotoUrls[destinationCardKey(destination)].orEmpty(),
                                onClick = { selectedDestination = destination },
                            )
                        }
                    }
                }
                displayedDestinations.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (regionLoading) {
                            CircularProgressIndicator()
                        } else {
                            Text(
                                text = "Sin destinos en esta región",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(bottom = 24.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        val hero = displayedDestinations.first()
                        item(key = "hero-${hero.id}") {
                            HeroDestinationCard(
                                destination = hero,
                                imageUrl = destinationPhotoUrls[destinationCardKey(hero)].orEmpty(),
                                onClick = { selectedDestination = hero },
                            )
                        }
                        items(displayedDestinations.drop(1), key = { it.id }) { destination ->
                            CompactDestinationRow(
                                destination = destination,
                                imageUrl = destinationPhotoUrls[destinationCardKey(destination)].orEmpty(),
                                onClick = { selectedDestination = destination },
                            )
                        }
                    }
                }
            }
        }
    }

    selectedDestination?.let { destination ->
        CityBottomSheet(
            destination = destination,
            imageUrl = destinationPhotoUrls[destinationCardKey(destination)].orEmpty(),
            currentMember = currentMember,
            tripDestination = tripDestination,
            hasActivePoll = activePoll != null,
            onDismiss = { selectedDestination = null },
            onSetDestination = {
                viewModel.setTripDestination(destination)
                selectedDestination = null
            },
            onCreatePollForDestination = {
                viewModel.createPollWithDestination(destination) {
                    navController.navigate("poll_detail")
                    selectedDestination = null
                }
            },
            onAddToPoll = { viewModel.addDestinationToPoll(destination) },
        )
    }
}

@Composable
private fun HeroDestinationCard(
    destination: StoredDestination,
    imageUrl: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            DestinationImage(
                imageUrl = imageUrl,
                contentDescription = destination.name,
                title = destination.name,
                subtitle = destination.province,
                icon = Icons.Default.LocationOn,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color(0x99000000)),
                            startY = 80f,
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.displaySmall,
                    fontFamily = FrauncesFamily,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = destination.province,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }
    }
}

@Composable
private fun CompactDestinationRow(
    destination: StoredDestination,
    imageUrl: String,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DestinationImage(
                imageUrl = imageUrl,
                contentDescription = destination.name,
                title = destination.name,
                subtitle = destination.province,
                icon = Icons.Default.LocationOn,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = destination.province,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PollBanner(onDismiss: () -> Unit, onTap: () -> Unit) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
        onClick = onTap,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CityBottomSheet(
    destination: StoredDestination,
    imageUrl: String,
    currentMember: GroupMember?,
    tripDestination: TripDestinationState,
    hasActivePoll: Boolean,
    onDismiss: () -> Unit,
    onSetDestination: () -> Unit,
    onCreatePollForDestination: () -> Unit,
    onAddToPoll: () -> Unit,
) {
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
            DestinationImage(
                imageUrl = imageUrl,
                contentDescription = destination.name,
                title = destination.name,
                subtitle = destination.province,
                icon = Icons.Default.LocationOn,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp)),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = destination.province,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                val isAdmin = currentMember?.role == MemberRole.ADMIN
                when {
                    currentMember == null -> {
                        Spacer(modifier = Modifier.height(24.dp))
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                    }
                    isAdmin -> {
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
                    else -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Info,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Solo los administradores pueden establecer el destino",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (hasActivePoll && currentMember != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { onAddToPoll(); onDismiss() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            Icons.Default.HowToVote,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir a encuesta")
                    }
                }
            }
        }
    }

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
                TextButton(onClick = {
                    showPollPromptDialog = false
                    onCreatePollForDestination()
                }) { Text("Crear encuesta") }
            },
            dismissButton = {
                Button(onClick = {
                    showPollPromptDialog = false
                    onSetDestination()
                }) { Text("Saltear y establecer") }
            },
        )
    }

    if (showReplaceDialog) {
        val currentName = (tripDestination as? TripDestinationState.Set)?.name.orEmpty()
        AlertDialog(
            onDismissRequest = { showReplaceDialog = false },
            title = { Text("¿Reemplazar destino?") },
            text = {
                Text(
                    "El destino actual es \"$currentName\". ¿Querés reemplazarlo por \"${destination.name}\"?",
                )
            },
            confirmButton = {
                Button(onClick = {
                    showReplaceDialog = false
                    onSetDestination()
                }) { Text("Reemplazar") }
            },
            dismissButton = {
                TextButton(onClick = { showReplaceDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

private fun destinationCardKey(destination: StoredDestination): String =
    if (destination.id.isNotBlank()) destination.id else "${destination.source}:${destination.sourceId}"

@Composable
private fun DestinationImage(
    imageUrl: String,
    contentDescription: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Default.Image,
) {
    val fallbackRegionSlug = DestinationFallbackImage.regionSlugFromToken(imageUrl)
    if (fallbackRegionSlug != null) {
        Image(
            painter = painterResource(destinationFallbackResId(fallbackRegionSlug)),
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
        return
    }

    if (imageUrl.isBlank()) {
        DestinationImageFallback(
            title = title,
            subtitle = subtitle,
            icon = icon,
            modifier = modifier,
        )
        return
    }

    AsyncImage(
        model = imageUrl,
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier,
    )
}

private fun destinationFallbackResId(regionSlug: String): Int = when (regionSlug) {
    "patagonia" -> R.drawable.destination_fallback_patagonia
    "cuyo" -> R.drawable.destination_fallback_cuyo
    "noroeste" -> R.drawable.destination_fallback_noroeste
    "litoral" -> R.drawable.destination_fallback_litoral
    "buenos_aires" -> R.drawable.destination_fallback_buenos_aires
    "cordoba" -> R.drawable.destination_fallback_cordoba
    else -> R.drawable.destination_fallback_argentina
}

@Composable
private fun DestinationImageFallback(
    title: String,
    subtitle: String?,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.secondaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant,
                        Color.White.copy(alpha = 0.35f),
                    ),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f),
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(10.dp),
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            subtitle?.takeIf { it.isNotBlank() }?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.82f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
