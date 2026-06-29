package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollCandidate
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.StoredDestination
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.destination.HomeFeedItem
import com.hllous.plantravel.presentation.destination.TripDestinationState
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.poll.PollViewModel
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.greetingForHour
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial
import java.time.LocalTime

@Composable
fun HomeScreen(
    navController: NavHostController,
    displayName: String,
    groupViewModel: GroupViewModel,
    pollViewModel: PollViewModel,
    destinationViewModel: DestinationViewModel,
    isDarkTheme: Boolean = false,
    onThemeChange: (Boolean, Offset?) -> Unit = { _, _ -> },
    onProfileClick: () -> Unit = {},
) {
    val currentGroup by groupViewModel.currentGroup.collectAsState()
    val members by groupViewModel.members.collectAsState()
    val hour = LocalTime.now().hour
    val greeting = greetingForHour(hour)
    var themeToggleCenter by remember { mutableStateOf<Offset?>(null) }

    Column(Modifier.fillMaxSize()) {
        // ── Immersive primary header ───────────────────────────────
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(bottom = 44.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.07f),
                    radius = 105.dp.toPx(),
                    center = Offset(size.width + 50.dp.toPx(), -60.dp.toPx())
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.04f),
                    radius = 75.dp.toPx(),
                    center = Offset(-30.dp.toPx(), size.height + 25.dp.toPx())
                )
            }

            Column(Modifier.fillMaxWidth()) {
                // Top row: menu + user avatar
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onThemeChange(!isDarkTheme, themeToggleCenter) },
                        modifier = Modifier.onGloballyPositioned { coords ->
                            themeToggleCenter = coords.boundsInRoot().center
                        }
                    ) {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = if (isDarkTheme) "Modo claro" else "Modo oscuro",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Surface(
                        modifier = Modifier.size(34.dp),
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.2f),
                        onClick = onProfileClick
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                            Text(
                                text = displayName.firstOrNull()?.uppercase() ?: "N",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }

                // Greeting
                Column(Modifier.padding(horizontal = 18.dp, vertical = 6.dp)) {
                    Text(
                        text = "$greeting,",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = FrauncesFamily,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            letterSpacing = 0.sp
                        ),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "${displayName.ifBlank { "Viajero" }} ✈",
                        style = MaterialTheme.typography.headlineLarge,
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Group block or no-group hint
                if (currentGroup != null) {
                    GroupHeaderBlock(
                        group = currentGroup!!,
                        members = members,
                        onTap = { navController.navigateSingleTopTo("groups") },
                    )
                } else {
                    NoGroupHeaderBlock()
                }
            }
        }

        // ── Content (rounded top, slides over header bottom padding) ──
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            if (currentGroup != null) {
                HomeGroupContent(
                    navController = navController,
                    currentGroup = currentGroup!!,
                    pollViewModel = pollViewModel,
                    destinationViewModel = destinationViewModel,
                )
            } else {
                HomeNoGroupContent(navController = navController)
            }
        }
    }
}

// ── Header blocks ───────────────────────────────────────────────────────────────

@Composable
private fun GroupHeaderBlock(group: TravelGroup, members: List<GroupMember>, onTap: () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        AvatarStack(members)
        Text(
            text = group.name,
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimary,
            letterSpacing = (-0.3).sp
        )
        Box(
            Modifier
                .background(Color.White.copy(alpha = 0.18f), RoundedCornerShape(100.dp))
                .padding(horizontal = 14.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${members.size} integrante${if (members.size != 1) "s" else ""}",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
                letterSpacing = 0.5.sp
            )
        }
    }
}

@Composable
private fun AvatarStack(members: List<GroupMember>) {
    val visible = members.take(3)
    val overflow = members.size - visible.size
    Row(horizontalArrangement = Arrangement.spacedBy((-8).dp)) {
        visible.forEach { member ->
            Box(
                Modifier
                    .size(30.dp)
                    .background(memberColor(member.id).copy(alpha = 0.35f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = memberInitial(member.name),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 11.sp
                )
            }
        }
        if (overflow > 0) {
            Box(
                Modifier
                    .size(30.dp)
                    .background(Color.White.copy(alpha = 0.15f), CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$overflow",
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 9.sp
                )
            }
        }
    }
}

@Composable
private fun NoGroupHeaderBlock() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text("🌍", style = MaterialTheme.typography.headlineMedium)
        Text(
            text = "Todavía no tenés un grupo de viaje",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
        )
    }
}

// ── Content sections ────────────────────────────────────────────────────────────

@Composable
private fun HomeGroupContent(
    navController: NavHostController,
    currentGroup: TravelGroup,
    pollViewModel: PollViewModel,
    destinationViewModel: DestinationViewModel,
) {
    val poll by pollViewModel.poll.collectAsState()
    val candidates by pollViewModel.candidates.collectAsState()
    val latestDestPoll by pollViewModel.latestDestinationPoll.collectAsState()
    val latestActivityPoll by pollViewModel.latestActivityPoll.collectAsState()
    val destPollCandidates by pollViewModel.destPollCandidates.collectAsState()
    val activityPollCandidates by pollViewModel.activityPollCandidates.collectAsState()
    val homeFeed by destinationViewModel.homeFeed.collectAsState()
    val tripDestination by destinationViewModel.tripDestination.collectAsState()
    val recommendedDestinations by destinationViewModel.recommendedDestinations.collectAsState()
    val destinationPhotoUrls by destinationViewModel.destinationPhotoUrls.collectAsState()

    val tripDestSet = tripDestination as? TripDestinationState.Set

    LaunchedEffect(tripDestination) {
        if (tripDestination is TripDestinationState.Set) {
            destinationViewModel.loadHomeFeed()
        } else {
            destinationViewModel.loadRecommendedDestinations()
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ① Poll summary card (existing behaviour, kept as-is)
        if (poll != null && poll!!.state == PollState.OPEN) {
            val winningCandidate = (candidates as? UiState.Success)?.data
                ?.maxByOrNull { it.voteCount }?.candidate
            val voteSubtitle = if (winningCandidate != null) {
                "${winningCandidate.voteCount} voto${if (winningCandidate.voteCount != 1) "s" else ""}"
            } else {
                "Sin votos aún"
            }
            ElevatedCard(
                onClick = { navController.navigate("poll_detail?pollId=${poll!!.id}") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier
                            .size(44.dp)
                            .background(
                                MaterialTheme.colorScheme.primaryContainer,
                                RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🗳️", fontSize = 22.sp)
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Encuesta activa",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = winningCandidate?.name ?: "Sin candidatos",
                            style = MaterialTheme.typography.titleSmall,
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = voteSubtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // ② ¿A dónde vamos?
        WhereAreWeGoingCard(
            tripDestination = tripDestination,
            latestDestPoll = latestDestPoll,
            destPollCandidates = destPollCandidates,
            onNavigateToPoll = { pollId -> navController.navigate("poll_detail?pollId=$pollId") },
            onCreatePoll = { navController.navigate("poll_detail") },
        )

        // ③ ¿Qué hacemos?
        WhatAreWeDoingCard(
            latestActivityPoll = latestActivityPoll,
            activityPollCandidates = activityPollCandidates,
            onNavigateToPoll = { pollId -> navController.navigate("poll_detail?pollId=$pollId") },
            onCreatePoll = { navController.navigate("poll_detail") },
        )

        // ④ Quick-access buttons (contextual)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            HomeSectionLabel("Acciones rápidas")
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                HomeActionButton(
                    emoji = "💸",
                    label = "Agregar\ngasto",
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                    contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigateSingleTopTo("gastos") }
                )
                if (tripDestSet == null) {
                    HomeActionButton(
                        emoji = "📍",
                        label = "Ver\ndestinos",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigateSingleTopTo("destinations") }
                    )
                    HomeActionButton(
                        emoji = "📨",
                        label = "Invitar\npersona",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigateSingleTopTo("groups") }
                    )
                } else {
                    HomeActionButton(
                        emoji = "🗺️",
                        label = "Actividades",
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigateSingleTopTo("destinations") }
                    )
                    HomeActionButton(
                        emoji = "📅",
                        label = "Itinerario",
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                        onClick = { navController.navigate("itinerary") }
                    )
                }
            }
        }

        // ⑤ Lateral scroll (contextual)
        if (tripDestSet != null) {
            // Trip destination set → show home feed
            when (homeFeed) {
                is UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeSectionLabel("Qué hacer en ${tripDestSet.name}")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                is UiState.Success -> {
                    val places = (homeFeed as UiState.Success<List<HomeFeedItem>>).data
                    if (places.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeSectionLabel("Qué hacer en ${tripDestSet.name}")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 1.dp)
                            ) {
                                items(places) { item ->
                                    HomeRecommendationCard(
                                        item = item,
                                        onClick = {
                                            destinationViewModel.requestOpenPoi(item.place, item.category)
                                            navController.navigateSingleTopTo("destinations")
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        } else {
            // No trip destination → show curated recommendations
            when (recommendedDestinations) {
                is UiState.Loading -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeSectionLabel("Destinos recomendados")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
                is UiState.Success -> {
                    val destinations = (recommendedDestinations as UiState.Success<List<StoredDestination>>).data
                    if (destinations.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            HomeSectionLabel("Destinos recomendados")
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(horizontal = 1.dp)
                            ) {
                                items(destinations) { dest ->
                                    val photoKey = if (dest.id.isNotBlank()) dest.id else "${dest.source}:${dest.sourceId}"
                                    HomeStoredDestinationCard(
                                        destination = dest,
                                        photoUrl = destinationPhotoUrls[photoKey] ?: dest.displayPhotoUrl,
                                        onClick = { navController.navigateSingleTopTo("destinations") }
                                    )
                                }
                            }
                        }
                    }
                }
                else -> Unit
            }
        }
    }
}

// ── Poll summary cards (② and ③) ───────────────────────────────────────────────
// Layout: colored Surface (primaryContainer / secondaryContainer) with horizontal row
// icon-box (solid primary/secondary) | body | optional chevron

@Composable
private fun WhereAreWeGoingCard(
    tripDestination: TripDestinationState,
    latestDestPoll: Poll?,
    destPollCandidates: List<PollCandidate>,
    onNavigateToPoll: (String) -> Unit,
    onCreatePoll: () -> Unit,
) {
    val tripDestSet = tripDestination as? TripDestinationState.Set
    val leader = destPollCandidates.maxByOrNull { it.voteCount }
    val isTied = leader != null && leader.voteCount > 0 &&
        destPollCandidates.count { it.voteCount == leader.voteCount } >= 2
    val secondTied = if (isTied)
        destPollCandidates.firstOrNull { it !== leader && it.voteCount == leader!!.voteCount }
    else null
    val winnerCandidate = latestDestPoll?.winnerPlaceId?.let { wId ->
        destPollCandidates.firstOrNull { it.placeId == wId }
    }
    val isViaPool = tripDestSet != null &&
        latestDestPoll?.state == PollState.CLOSED &&
        latestDestPoll.winnerPlaceId != null

    val pollId = latestDestPoll?.id
    val isCardClickable = when {
        tripDestSet != null && !isViaPool -> false
        latestDestPoll == null -> false
        else -> true
    }

    SectionCard(
        isClickable = isCardClickable,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        onClick = { if (pollId != null) onNavigateToPoll(pollId) }
    ) {
        DestCardRow(
            iconEmoji = "🗺️",
            iconBgColor = MaterialTheme.colorScheme.primary,
            label = "¿A dónde vamos?",
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
            showChevron = isCardClickable,
            chevronColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.45f),
        ) {
            when {
                tripDestSet != null && isViaPool -> {
                    DestCardTitle(tripDestSet.name.let { "📍 $it" }, MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardSub("Destino del viaje · Ver encuesta", MaterialTheme.colorScheme.onPrimaryContainer)
                }
                tripDestSet != null -> {
                    DestCardTitle("📍 ${tripDestSet.name}", MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardSub("Destino del viaje", MaterialTheme.colorScheme.onPrimaryContainer)
                }
                latestDestPoll == null -> {
                    DestCardTitle("Sin encuesta activa", MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardSub("Creá una para votar en grupo", MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardCta("Crear encuesta", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, onCreatePoll)
                }
                latestDestPoll.state == PollState.OPEN && (leader == null || leader.voteCount == 0) -> {
                    DestCardTitle("Sin candidatos aún", MaterialTheme.colorScheme.onPrimaryContainer)
                }
                latestDestPoll.state == PollState.OPEN -> {
                    DestCardTitle(leader!!.name, MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardSub("🔥 ${leader.voteCount} voto${if (leader.voteCount != 1) "s" else ""} · líder actual", MaterialTheme.colorScheme.onPrimaryContainer)
                }
                winnerCandidate != null -> {
                    DestCardTitle("🏆 ${winnerCandidate.name}", MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardSub("Ganador de la encuesta", MaterialTheme.colorScheme.onPrimaryContainer)
                }
                leader != null -> {
                    DestCardTitle(leader.name, MaterialTheme.colorScheme.onPrimaryContainer)
                    DestCardEmpateBadge(secondTied?.name, MaterialTheme.colorScheme.onPrimaryContainer)
                }
                else -> {
                    DestCardTitle("Sin resultados", MaterialTheme.colorScheme.onPrimaryContainer)
                }
            }
        }
    }
}

@Composable
private fun WhatAreWeDoingCard(
    latestActivityPoll: Poll?,
    activityPollCandidates: List<PollCandidate>,
    onNavigateToPoll: (String) -> Unit,
    onCreatePoll: () -> Unit,
) {
    val leader = activityPollCandidates.maxByOrNull { it.voteCount }
    val isTied = leader != null && leader.voteCount > 0 &&
        activityPollCandidates.count { it.voteCount == leader.voteCount } >= 2
    val secondTied = if (isTied)
        activityPollCandidates.firstOrNull { it !== leader && it.voteCount == leader!!.voteCount }
    else null
    val winnerCandidate = latestActivityPoll?.winnerPlaceId?.let { wId ->
        activityPollCandidates.firstOrNull { it.placeId == wId }
    }

    val pollId = latestActivityPoll?.id
    val isCardClickable = pollId != null

    SectionCard(
        isClickable = isCardClickable,
        containerColor = MaterialTheme.colorScheme.secondaryContainer,
        onClick = { if (pollId != null) onNavigateToPoll(pollId) }
    ) {
        DestCardRow(
            iconEmoji = "✨",
            iconBgColor = MaterialTheme.colorScheme.secondary,
            label = "¿Qué hacemos?",
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.65f),
            showChevron = isCardClickable,
            chevronColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.45f),
        ) {
            when {
                latestActivityPoll == null -> {
                    DestCardTitle("Sin encuesta activa", MaterialTheme.colorScheme.onSecondaryContainer)
                    DestCardSub("Creá una para votar en grupo", MaterialTheme.colorScheme.onSecondaryContainer)
                    DestCardCta("Crear encuesta", MaterialTheme.colorScheme.secondary, MaterialTheme.colorScheme.onSecondary, onCreatePoll)
                }
                latestActivityPoll.state == PollState.OPEN && (leader == null || leader.voteCount == 0) -> {
                    DestCardTitle("Sin candidatos aún", MaterialTheme.colorScheme.onSecondaryContainer)
                }
                latestActivityPoll.state == PollState.OPEN -> {
                    DestCardTitle(leader!!.name, MaterialTheme.colorScheme.onSecondaryContainer)
                    DestCardSub("🔥 ${leader.voteCount} voto${if (leader.voteCount != 1) "s" else ""} · líder actual", MaterialTheme.colorScheme.onSecondaryContainer)
                }
                winnerCandidate != null -> {
                    DestCardTitle("🏆 ${winnerCandidate.name}", MaterialTheme.colorScheme.onSecondaryContainer)
                    DestCardSub("Ganador de la encuesta", MaterialTheme.colorScheme.onSecondaryContainer)
                }
                leader != null -> {
                    DestCardTitle(leader.name, MaterialTheme.colorScheme.onSecondaryContainer)
                    DestCardEmpateBadge(secondTied?.name, MaterialTheme.colorScheme.onSecondaryContainer)
                }
                else -> {
                    DestCardTitle("Sin resultados", MaterialTheme.colorScheme.onSecondaryContainer)
                }
            }
        }
    }
}

// ── Section card primitives ──────────────────────────────────────────────────────

@Composable
private fun SectionCard(
    isClickable: Boolean,
    containerColor: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    if (isClickable) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
        ) { content() }
    } else {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = containerColor,
        ) { content() }
    }
}

@Composable
private fun DestCardRow(
    iconEmoji: String,
    iconBgColor: Color,
    label: String,
    labelColor: Color,
    showChevron: Boolean,
    chevronColor: Color,
    body: @Composable () -> Unit,
) {
    Row(
        Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            Modifier
                .size(46.dp)
                .background(iconBgColor, RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(iconEmoji, fontSize = 24.sp)
        }
        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                color = labelColor,
                letterSpacing = 1.sp,
                fontSize = 10.sp
            )
            body()
        }
        if (showChevron) {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = chevronColor,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DestCardTitle(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        fontFamily = FrauncesFamily,
        fontWeight = FontWeight.Bold,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DestCardSub(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = color.copy(alpha = 0.7f),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DestCardCta(label: String, bgColor: Color, textColor: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(100.dp),
        color = bgColor,
        modifier = Modifier.padding(top = 7.dp)
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = textColor
        )
    }
}

@Composable
private fun DestCardEmpateBadge(secondTiedName: String?, onContainerColor: Color) {
    Surface(
        shape = RoundedCornerShape(100.dp),
        color = onContainerColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = "⚖️ Empate${secondTiedName?.let { " con $it" } ?: ""}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = onContainerColor,
            fontSize = 10.sp
        )
    }
}

@Composable
private fun HomeNoGroupContent(navController: NavHostController) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "¿Listo para viajar?",
            style = MaterialTheme.typography.headlineSmall,
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.3).sp
        )
        Text(
            text = "Creá tu primer grupo o unite a uno ya existente",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        HomeCTACard(
            icon = Icons.Default.Add,
            iconContainerColor = MaterialTheme.colorScheme.primaryContainer,
            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
            label = "Crear grupo",
            description = "Invitá amigos y empezá a planear",
            onClick = { navController.navigateSingleTopTo("groups") }
        )
        HomeCTACard(
            icon = Icons.Default.Link,
            iconContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            iconTint = MaterialTheme.colorScheme.onSecondaryContainer,
            label = "Tengo un código",
            description = "Ingresá el código que te compartieron",
            onClick = { navController.navigateSingleTopTo("groups") }
        )
        HomeCTACard(
            icon = Icons.Default.QrCode,
            iconContainerColor = MaterialTheme.colorScheme.tertiaryContainer,
            iconTint = MaterialTheme.colorScheme.onTertiaryContainer,
            label = "Escanear QR",
            description = "Escaneá el QR de una invitación",
            onClick = { navController.navigate("qr_scanner") }
        )
    }
}

// ── Small reusable composables ──────────────────────────────────────────────────

@Composable
private fun HomeSectionLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp,
        fontSize = 10.sp
    )
}

@Composable
private fun HomeActionButton(
    emoji: String,
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(15.dp),
        color = containerColor
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(emoji, fontSize = 22.sp)
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                textAlign = TextAlign.Center,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun HomeRecommendationCard(
    item: HomeFeedItem,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.tertiaryContainer, MaterialTheme.colorScheme.primaryContainer)))
            ) {
                AsyncImage(
                    model = item.place.photoUrl.ifBlank { null }?.let { url ->
                        ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .build()
                    },
                    contentDescription = item.place.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = item.category.uppercase(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp
                    )
                }
                Text(
                    text = item.place.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                if (item.place.rating > 0.0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(11.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "%.1f".format(item.place.rating),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeStoredDestinationCard(
    destination: StoredDestination,
    photoUrl: String?,
    onClick: () -> Unit,
) {
    val context = LocalContext.current

    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(88.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp))
                    .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)))
            ) {
                AsyncImage(
                    model = photoUrl?.ifBlank { null }?.let { url ->
                        ImageRequest.Builder(context)
                            .data(url)
                            .allowHardware(false)
                            .build()
                    },
                    contentDescription = destination.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )
            }
            Column(
                Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(100.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = destination.region.uppercase(),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        letterSpacing = 0.5.sp,
                        fontSize = 9.sp
                    )
                }
                Text(
                    text = destination.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp
                )
                Text(
                    text = destination.province,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun HomeCTACard(
    icon: ImageVector,
    iconContainerColor: Color,
    iconTint: Color,
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                Modifier
                    .size(42.dp)
                    .background(iconContainerColor, RoundedCornerShape(13.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, Modifier.size(20.dp), tint = iconTint)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

private fun NavHostController.navigateSingleTopTo(route: String) {
    navigate(route) {
        popUpTo("home") {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
