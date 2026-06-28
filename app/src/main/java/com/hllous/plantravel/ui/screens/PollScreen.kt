package com.hllous.plantravel.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.Poll
import com.hllous.plantravel.domain.model.PollState
import com.hllous.plantravel.domain.model.PollType
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.poll.PollCandidateUiModel
import com.hllous.plantravel.presentation.poll.PollViewModel
import com.hllous.plantravel.ui.theme.FrauncesFamily
import java.time.Instant
import java.time.ZoneOffset
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

// ── State helpers ─────────────────────────────────────────────────────────────

private enum class WinnerFlowStep { NONE, TIE_CHOICE, MANUAL_SELECT, COIN_FLIP }

private data class WinnerOverlayData(
    val name: String,
    val voteCount: Int,
    val totalVotes: Int,
    val isDestination: Boolean,
    val key: Int,
)

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollScreen(
    viewModel: PollViewModel,
    navController: NavHostController,
) {
    val allPolls by viewModel.allPolls.collectAsState()
    val candidatesState by viewModel.candidates.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isTied by viewModel.isTied.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreationSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }

    // Level 1 → Level 2 navigation
    var detailPollId by rememberSaveable { mutableStateOf<String?>(null) }
    val detailPoll = detailPollId?.let { id -> allPolls.firstOrNull { it.id == id } }

    // Winner selection flow (triggered by Finalizar)
    var winnerFlowStep by remember { mutableStateOf(WinnerFlowStep.NONE) }
    var pendingWinner by remember { mutableStateOf<PollCandidateUiModel?>(null) }
    var winnerOverlayData by remember { mutableStateOf<WinnerOverlayData?>(null) }
    var showConfirmDestination by remember { mutableStateOf(false) }

    // Keep screenPoll in sync with the Level 2 poll so candidatesState is correct
    LaunchedEffect(detailPollId) {
        viewModel.setScreenPoll(detailPoll)
    }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearErrorMessage()
        }
    }

    val isAdmin = currentMember?.role == MemberRole.ADMIN

    fun onFinalizeTapped() {
        val willBeTied = isTied
        viewModel.closePoll()
        winnerFlowStep = if (willBeTied) WinnerFlowStep.TIE_CHOICE else WinnerFlowStep.MANUAL_SELECT
    }

    fun onWinnerConfirmed(candidateId: String) {
        val cs = candidatesState as? UiState.Success ?: return
        val winner = cs.data.firstOrNull { it.candidate.id == candidateId } ?: return
        pendingWinner = winner
        viewModel.selectWinner(candidateId)
        winnerFlowStep = WinnerFlowStep.NONE
        winnerOverlayData = WinnerOverlayData(
            name = winner.candidate.name,
            voteCount = winner.voteCount,
            totalVotes = memberCount,
            isDestination = detailPoll?.type == PollType.DESTINATION,
            key = System.currentTimeMillis().toInt(),
        )
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = detailPoll?.name?.ifBlank { "Encuesta" } ?: "Encuestas",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (detailPollId != null) {
                            detailPollId = null
                            winnerFlowStep = WinnerFlowStep.NONE
                        } else {
                            navController.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (detailPoll != null && isAdmin) {
                        if (detailPoll.state == PollState.OPEN) {
                            TextButton(
                                onClick = { onFinalizeTapped() },
                                enabled = !isSubmitting,
                            ) { Text("Finalizar") }
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSubmitting,
                        ) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            if (detailPoll == null) {
                PollListContent(
                    allPolls = allPolls,
                    isAdmin = isAdmin,
                    modifier = Modifier.fillMaxSize(),
                    onViewDetails = { poll ->
                        detailPollId = poll.id
                    },
                    onDelete = { poll ->
                        detailPollId = poll.id
                        showDeleteDialog = true
                    },
                    onCreatePoll = { showCreationSheet = true },
                )
            } else {
                PollDetailContent(
                    poll = detailPoll,
                    candidatesState = candidatesState,
                    isAdmin = isAdmin,
                    isSubmitting = isSubmitting,
                    modifier = Modifier.fillMaxSize(),
                    onToggleVote = { candidateId ->
                        viewModel.setScreenPoll(detailPoll)
                        viewModel.toggleVote(candidateId)
                    },
                    onSetAsDestination = {
                        val cs = candidatesState as? UiState.Success ?: return@PollDetailContent
                        val winner = cs.data.firstOrNull { it.candidate.placeId == detailPoll.winnerPlaceId }
                            ?: return@PollDetailContent
                        pendingWinner = winner
                        showConfirmDestination = true
                    },
                )
            }
        }
    }

    // ── Dialogs (outside Scaffold so they float above everything) ────────────

    if (showCreationSheet) {
        PollCreationBottomSheet(
            onDismiss = { showCreationSheet = false },
            onCreate = { type, name, expiresAt ->
                viewModel.createPoll(type, name, expiresAt)
                showCreationSheet = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar encuesta") },
            text = { Text("¿Eliminar esta encuesta y todos sus candidatos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setScreenPoll(detailPoll)
                    viewModel.deletePoll()
                    showDeleteDialog = false
                    detailPollId = null
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }

    // Winner flow dialogs
    val dialogCandidates = (candidatesState as? UiState.Success)?.data ?: emptyList()

    when (winnerFlowStep) {
        WinnerFlowStep.TIE_CHOICE -> TieChoiceDialog(
            onDismiss = { winnerFlowStep = WinnerFlowStep.NONE },
            onManual = { winnerFlowStep = WinnerFlowStep.MANUAL_SELECT },
            onCoinFlip = { winnerFlowStep = WinnerFlowStep.COIN_FLIP },
        )
        WinnerFlowStep.MANUAL_SELECT -> WinnerSelectionDialog(
            candidates = dialogCandidates,
            onDismiss = { winnerFlowStep = WinnerFlowStep.NONE },
            onSelectWinner = { candidateId -> onWinnerConfirmed(candidateId) },
        )
        WinnerFlowStep.COIN_FLIP -> {
            val maxVotes = dialogCandidates.maxOfOrNull { it.voteCount } ?: 0
            TiedCoinFlipDialog(
                tiedCandidates = dialogCandidates.filter { it.voteCount == maxVotes }.shuffled(),
                onDismiss = { winnerFlowStep = WinnerFlowStep.NONE },
                onSelectWinner = { candidateId -> onWinnerConfirmed(candidateId) },
            )
        }
        WinnerFlowStep.NONE -> Unit
    }

    winnerOverlayData?.let { data ->
        WinnerOverlay(
            isDestinationPoll = data.isDestination,
            winnerName = data.name,
            voteCount = data.voteCount,
            totalVotes = data.totalVotes,
            animKey = data.key,
            onDismiss = {
                winnerOverlayData = null
                val pw = pendingWinner
                if (data.isDestination && pw != null) {
                    showConfirmDestination = true
                } else {
                    pendingWinner = null
                }
            },
        )
    }

    if (showConfirmDestination) {
        val pw = pendingWinner
        AlertDialog(
            onDismissRequest = { showConfirmDestination = false; pendingWinner = null },
            title = { Text("¿Seleccionar destino?") },
            text = {
                Text("¿Querés seleccionar ${pw?.candidate?.name ?: "el ganador"} como destino del viaje?")
            },
            confirmButton = {
                Button(onClick = {
                    if (pw != null) {
                        viewModel.setWinnerAsDestination(
                            placeId = pw.candidate.placeId,
                            name = pw.candidate.name,
                            lat = pw.candidate.lat,
                            lng = pw.candidate.lng,
                        )
                    }
                    showConfirmDestination = false
                    pendingWinner = null
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDestination = false; pendingWinner = null }) {
                    Text("No por ahora")
                }
            },
        )
    }
}

// ── Level 1: Poll list ────────────────────────────────────────────────────────

@Composable
private fun PollListContent(
    allPolls: List<Poll>,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    onViewDetails: (Poll) -> Unit,
    onDelete: (Poll) -> Unit,
    onCreatePoll: () -> Unit,
) {
    val activePolls = allPolls.filter { it.state == PollState.OPEN }
    val finalizedPolls = allPolls.filter { it.state == PollState.CLOSED }

    if (allPolls.isEmpty()) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(32.dp),
            ) {
                Icon(
                    Icons.Default.HowToVote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(64.dp),
                )
                Text(
                    "No hay encuestas todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
                if (isAdmin) {
                    Button(onClick = onCreatePoll) { Text("Crear encuesta") }
                }
            }
        }
        return
    }

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        if (isAdmin) {
            item {
                Button(
                    onClick = onCreatePoll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) { Text("+ Nueva encuesta") }
            }
        }

        if (activePolls.isNotEmpty()) {
            item {
                Text(
                    "Activas",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            items(activePolls, key = { "active-${it.id}" }) { poll ->
                PollListCard(
                    poll = poll,
                    isAdmin = isAdmin,
                    onViewDetails = { onViewDetails(poll) },
                    onDelete = { onDelete(poll) },
                )
            }
        }

        if (finalizedPolls.isNotEmpty()) {
            item {
                Text(
                    "Finalizadas",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp).padding(top = 8.dp),
                )
            }
            items(finalizedPolls, key = { "final-${it.id}" }) { poll ->
                PollListCard(
                    poll = poll,
                    isAdmin = isAdmin,
                    onViewDetails = { onViewDetails(poll) },
                    onDelete = { onDelete(poll) },
                )
            }
        }
    }
}

@Composable
private fun PollListCard(
    poll: Poll,
    isAdmin: Boolean,
    onViewDetails: () -> Unit,
    onDelete: () -> Unit,
) {
    val context = LocalContext.current
    Card(
        onClick = onViewDetails,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Thumbnail: winner photo or placeholder
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center,
            ) {
                val cardPhotoUrl = poll.winnerPhotoUrl ?: poll.thumbnailPhotoUrl
                if (cardPhotoUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(cardPhotoUrl)
                            .allowHardware(false)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Icon(
                        Icons.Default.HowToVote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = poll.name.ifBlank { if (poll.type == PollType.DESTINATION) "¿A dónde vamos?" else "¿Qué hacemos?" },
                    style = MaterialTheme.typography.titleSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    SuggestionChip(
                        onClick = {},
                        label = {
                            Text(
                                if (poll.type == PollType.DESTINATION) "Destino" else "Actividad",
                                style = MaterialTheme.typography.labelSmall,
                            )
                        },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = if (poll.type == PollType.DESTINATION)
                                MaterialTheme.colorScheme.primaryContainer
                            else
                                MaterialTheme.colorScheme.secondaryContainer,
                            labelColor = if (poll.type == PollType.DESTINATION)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    )
                    if (poll.winnerPlaceId != null) {
                        SuggestionChip(
                            onClick = {},
                            label = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Icon(Icons.Default.EmojiEvents, null, modifier = Modifier.size(12.dp))
                                    Text("Con ganador", style = MaterialTheme.typography.labelSmall)
                                }
                            },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                labelColor = MaterialTheme.colorScheme.onTertiaryContainer,
                            ),
                        )
                    }
                }
            }

            if (isAdmin) {
                IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

// ── Level 2: Poll detail ──────────────────────────────────────────────────────

@Composable
private fun PollDetailContent(
    poll: Poll,
    candidatesState: UiState<List<PollCandidateUiModel>>,
    isAdmin: Boolean,
    isSubmitting: Boolean,
    modifier: Modifier = Modifier,
    onToggleVote: (String) -> Unit,
    onSetAsDestination: (() -> Unit)? = null,
) {
    val isClosed = poll.state == PollState.CLOSED

    LazyColumn(modifier = modifier, contentPadding = PaddingValues(bottom = 24.dp)) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chipLabel = when {
                    isClosed -> if (poll.type == PollType.DESTINATION) "Destino · Finalizada" else "Actividad · Finalizada"
                    else -> if (poll.type == PollType.DESTINATION) "Encuesta de Destino" else "Encuesta de Actividad"
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(chipLabel) },
                    colors = if (isClosed)
                        SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    else
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }

        // Show "Establecer como destino / actividad" button for closed polls with a winner
        if (isClosed && poll.winnerPlaceId != null && isAdmin && onSetAsDestination != null) {
            item {
                val buttonLabel = if (poll.type == PollType.DESTINATION) "Establecer como destino" else "Agregar al itinerario"
                FilledTonalButton(
                    onClick = onSetAsDestination,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Default.EmojiEvents, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(buttonLabel)
                }
            }
        }

        when (candidatesState) {
            is UiState.Loading -> item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) { CircularProgressIndicator() }
            }
            is UiState.Error -> item {
                Text(
                    candidatesState.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp),
                )
            }
            is UiState.Success -> {
                if (candidatesState.data.isEmpty()) {
                    item {
                        Text(
                            "Todavía no hay candidatos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
                items(candidatesState.data, key = { it.candidate.id }) { uiModel ->
                    PollCandidateCard(
                        uiModel = uiModel,
                        isWinner = uiModel.candidate.placeId == poll.winnerPlaceId,
                        votingEnabled = !isClosed && !isSubmitting,
                        onToggleVote = { onToggleVote(uiModel.candidate.id) },
                    )
                }
            }
        }
    }
}

// ── Shared composables ────────────────────────────────────────────────────────

@Composable
private fun PollCandidateCard(
    uiModel: PollCandidateUiModel,
    isWinner: Boolean,
    votingEnabled: Boolean,
    onToggleVote: () -> Unit,
) {
    val candidate = uiModel.candidate
    val context = LocalContext.current

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        border = if (isWinner)
            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = if (isWinner)
            CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
        else
            CardDefaults.outlinedCardColors(),
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(candidate.photoUrl)
                    .allowHardware(false)
                    .build(),
                contentDescription = candidate.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )

            Column(modifier = Modifier.padding(12.dp)) {
                if (isWinner) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Ganador",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    candidate.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconToggleButton(
                        checked = uiModel.votedByCurrentMember,
                        onCheckedChange = { if (votingEnabled) onToggleVote() },
                        enabled = votingEnabled,
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (uiModel.votedByCurrentMember) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                            contentDescription = if (uiModel.votedByCurrentMember) "Quitar voto" else "Votar",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${uiModel.voteCount}", style = MaterialTheme.typography.bodyMedium)
                }
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { uiModel.voteProgress },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ── Winner flow dialogs ───────────────────────────────────────────────────────

@Composable
private fun TieChoiceDialog(
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onCoinFlip: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¡Hay un empate! 🤝") },
        text = { Text("¿Cómo querés elegir el ganador?") },
        confirmButton = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Button(onClick = onCoinFlip, modifier = Modifier.fillMaxWidth()) {
                    Text("🪙  Tirar la moneda")
                }
                OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
                    Text("Seleccionar manualmente")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}

@Composable
private fun WinnerSelectionDialog(
    candidates: List<PollCandidateUiModel>,
    onDismiss: () -> Unit,
    onSelectWinner: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Seleccionar ganador") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                candidates.forEach { uiModel ->
                    Card(
                        onClick = { onSelectWinner(uiModel.candidate.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                uiModel.candidate.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.ThumbUp,
                                    contentDescription = null,
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "${uiModel.voteCount}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun TiedCoinFlipDialog(
    tiedCandidates: List<PollCandidateUiModel>,
    onDismiss: () -> Unit,
    onSelectWinner: (String) -> Unit,
) {
    val front = tiedCandidates.getOrNull(0) ?: return
    val back = tiedCandidates.getOrNull(1) ?: front

    var winner by remember { mutableStateOf<PollCandidateUiModel?>(null) }
    var isFlipping by remember { mutableStateOf(false) }
    val rotation = remember { Animatable(0f) }
    val coinScale = remember { Animatable(0.72f) }
    val winAlpha = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()

    fun flip() {
        if (isFlipping) return
        scope.launch {
            isFlipping = true
            winner = null
            winAlpha.snapTo(0f)
            rotation.snapTo(0f)
            coinScale.snapTo(0.72f)
            launch { coinScale.animateTo(1f, tween(750, easing = FastOutSlowInEasing)) }
            val landFront = Random.nextBoolean()
            rotation.animateTo(1440f + if (landFront) 0f else 180f, tween(2600, easing = LinearOutSlowInEasing))
            winner = if (landFront) front else back
            winAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            isFlipping = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(28.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text("¡Hay un empate! 🤝", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Text(
                    tiedCandidates.joinToString("  ·  ") { it.candidate.name.take(14) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )

                val angle = rotation.value % 360f
                val showFront = angle < 90f || angle >= 270f
                val faceName = if (showFront) front.candidate.name else back.candidate.name
                val coinFront = listOf(Color(0xFFFFE566), Color(0xFFE6A000))
                val coinBack = listOf(Color(0xFFD4B200), Color(0xFF9A7A00))

                Box(
                    modifier = Modifier
                        .size(148.dp)
                        .graphicsLayer { rotationY = rotation.value; scaleX = coinScale.value; scaleY = coinScale.value }
                        .clip(CircleShape)
                        .background(Brush.radialGradient(if (showFront) coinFront else coinBack)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = if (showFront) 1f else -1f },
                    ) {
                        Text(faceName.first().toString(), fontSize = 52.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF7A4200))
                        Text(faceName.take(4).uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7A4200).copy(alpha = 0.6f), letterSpacing = 2.sp)
                    }
                }

                when {
                    winner != null -> Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.graphicsLayer { this.alpha = winAlpha.value },
                    ) {
                        Text("🎉", fontSize = 40.sp)
                        Text(
                            "¡Ganó ${winner!!.candidate.name}!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = {
                                scope.launch {
                                    winner = null
                                    winAlpha.snapTo(0f)
                                    rotation.snapTo(0f)
                                    coinScale.snapTo(0.72f)
                                }
                            }) { Text("↺  Volver a tirar") }
                            Button(onClick = { onSelectWinner(winner!!.candidate.id) }) { Text("Confirmar") }
                        }
                    }
                    isFlipping -> Text("Girando...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    else -> Button(
                        onClick = { flip() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6A000)),
                    ) { Text("🪙  Tirar moneda", color = Color.White, fontWeight = FontWeight.Bold) }
                }

                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

// ── Winner overlay (confetti) ─────────────────────────────────────────────────

@Composable
private fun WinnerOverlay(
    isDestinationPoll: Boolean,
    winnerName: String,
    voteCount: Int,
    totalVotes: Int,
    animKey: Int,
    onDismiss: () -> Unit,
) {
    var started by remember(animKey) { mutableStateOf(false) }

    LaunchedEffect(animKey) {
        kotlinx.coroutines.delay(80)
        started = true
        kotlinx.coroutines.delay(4500)
        onDismiss()
    }

    val contentScale by animateFloatAsState(
        targetValue = if (started) 1f else 0.5f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "scale",
    )
    val alpha by animateFloatAsState(
        targetValue = if (started) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "alpha",
    )

    val confetti = remember(animKey) { buildConfetti() }
    val inf = rememberInfiniteTransition(label = "confetti")
    val progress by inf.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "fall",
    )

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xED000000)).clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            confetti.forEach { piece ->
                val t = ((progress + piece.startOffset) * piece.speed) % 1.35f
                val px = piece.xFrac * size.width + sin(t * 6.28f * 1.5f) * piece.wobble * size.width
                val py = (t - 0.15f) * size.height * 1.15f
                if (py < -piece.h || py > size.height + piece.h) return@forEach
                rotate(degrees = t * 540f * piece.speed + piece.rot0, pivot = Offset(px, py)) {
                    drawRect(piece.color, topLeft = Offset(px - piece.w / 2f, py - piece.h / 2f), size = Size(piece.w, piece.h))
                }
            }
        }

        Column(
            modifier = Modifier
                .graphicsLayer { scaleX = contentScale; scaleY = contentScale; this.alpha = alpha }
                .padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("🏆", fontSize = 68.sp)
            Text(
                if (isDestinationPoll) "¡Nos vamos a" else "¡Vamos a",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Text(
                "$winnerName!",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FrauncesFamily,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Surface(shape = RoundedCornerShape(50), color = Color(0xFF2563EB).copy(alpha = 0.30f)) {
                Text(
                    "👍  $voteCount de $totalVotes votos",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text("Toca para continuar", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.38f))
        }
    }
}

private data class ConfettiPiece(
    val xFrac: Float, val startOffset: Float, val color: Color,
    val w: Float, val h: Float, val speed: Float, val rot0: Float, val wobble: Float,
)

private fun buildConfetti(): List<ConfettiPiece> {
    val palette = listOf(
        Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFD97706),
        Color(0xFF10B981), Color(0xFFEF4444), Color(0xFFEC4899),
        Color(0xFFFFFFFF), Color(0xFF60A5FA), Color(0xFFFBB928),
    )
    return (0..80).map {
        ConfettiPiece(
            xFrac = Random.nextFloat(), startOffset = Random.nextFloat(),
            color = palette.random(),
            w = Random.nextFloat() * 14f + 7f, h = Random.nextFloat() * 9f + 5f,
            speed = Random.nextFloat() * 0.38f + 0.78f,
            rot0 = Random.nextFloat() * 360f,
            wobble = Random.nextFloat() * 0.055f + 0.008f,
        )
    }
}

// ── Poll creation bottom sheet ────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PollCreationBottomSheet(
    onDismiss: () -> Unit,
    onCreate: (type: PollType, name: String, expiresAt: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by rememberSaveable { mutableStateOf(PollType.DESTINATION) }
    var pollName by rememberSaveable { mutableStateOf("¿A dónde vamos?") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var expiresAt by rememberSaveable { mutableStateOf<String?>(null) }
    val datePickerState = rememberDatePickerState()

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("Nueva encuesta", style = MaterialTheme.typography.titleLarge, fontFamily = FrauncesFamily, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Tipo de encuesta", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Column(modifier = Modifier.selectableGroup()) {
                listOf(PollType.DESTINATION to "Destino", PollType.ACTIVITY to "Actividad").forEach { (type, label) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedType == type,
                                onClick = {
                                    selectedType = type
                                    pollName = if (type == PollType.DESTINATION) "¿A dónde vamos?" else "¿Qué hacemos?"
                                },
                                role = Role.RadioButton,
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selectedType == type, onClick = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            OutlinedTextField(
                value = pollName, onValueChange = { pollName = it },
                label = { Text("Nombre de la encuesta") },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))
            Text("Vencimiento (opcional)", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                Text(expiresAt ?: "Seleccionar fecha de vencimiento")
            }
            Spacer(modifier = Modifier.height(8.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                FilledTonalButton(onClick = { onCreate(selectedType, pollName, expiresAt) }, modifier = Modifier.weight(1f)) { Text("Crear") }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        expiresAt = Instant.ofEpochMilli(millis)
                            .atZone(ZoneOffset.UTC).toLocalDate()
                            .atTime(23, 59, 59).atOffset(ZoneOffset.UTC)
                            .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) { DatePicker(state = datePickerState) }
    }
}
