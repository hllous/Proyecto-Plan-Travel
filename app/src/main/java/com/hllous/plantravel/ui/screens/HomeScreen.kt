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
import com.hllous.plantravel.domain.model.PollState
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
                    GroupHeaderBlock(group = currentGroup!!, members = members)
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
private fun GroupHeaderBlock(group: TravelGroup, members: List<GroupMember>) {
    Column(
        Modifier
            .fillMaxWidth()
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
    val homeFeed by destinationViewModel.homeFeed.collectAsState()
    val tripDestination by destinationViewModel.tripDestination.collectAsState()

    LaunchedEffect(tripDestination) {
        if (tripDestination is TripDestinationState.Set) destinationViewModel.loadHomeFeed()
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // ── Poll summary card ──────────────────────────────────────────────────
        if (poll != null && poll!!.state == PollState.OPEN) {
            val winningCandidate = (candidates as? UiState.Success)?.data
                ?.maxByOrNull { it.voteCount }?.candidate
            val voteSubtitle = if (winningCandidate != null) {
                "${winningCandidate.voteCount} voto${if (winningCandidate.voteCount != 1) "s" else ""}"
            } else {
                "Sin votos aún"
            }
            ElevatedCard(
                onClick = { navController.navigate("poll_detail") },
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

        // ── Quick actions ──────────────────────────────────────────────────────
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
            }
        }

        // ── Recommendations section ────────────────────────────────────────────
        when (homeFeed) {
            is UiState.Loading -> if (tripDestination is TripDestinationState.Set) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    HomeSectionLabel("Para tu viaje")
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }
            }
            is UiState.Success -> {
                val places = (homeFeed as UiState.Success<List<HomeFeedItem>>).data
                if (places.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        HomeSectionLabel("Para tu viaje")
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
private fun HomeTileCard(
    emoji: String,
    tag: String,
    title: String,
    subtitle: String,
    tagContainerColor: Color,
    tagContentColor: Color,
    onClick: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.width(130.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(emoji, fontSize = 24.sp)
            Surface(
                shape = RoundedCornerShape(100.dp),
                color = tagContainerColor
            ) {
                Text(
                    text = tag.uppercase(),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = tagContentColor,
                    letterSpacing = 0.6.sp,
                    fontSize = 9.sp
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column {
            AsyncImage(
                model = item.place.photoUrl.ifBlank { null }?.let { url ->
                    ImageRequest.Builder(context)
                        .data(url)
                        .allowHardware(false)
                        .build()
                },
                contentDescription = item.place.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)),
                contentScale = ContentScale.Crop,
            )
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
