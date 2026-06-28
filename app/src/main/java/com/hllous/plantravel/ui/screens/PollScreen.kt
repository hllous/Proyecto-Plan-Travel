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
import androidx.compose.material3.Surface
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
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
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private data class WinnerOverlayData(
    val name: String,
    val voteCount: Int,
    val totalVotes: Int,
    val isDestination: Boolean,
    val key: Int,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollScreen(
    viewModel: PollViewModel,
    navController: NavHostController,
) {
    val poll by viewModel.poll.collectAsState()
    val allPolls by viewModel.allPolls.collectAsState()
    val activeActivityPolls by viewModel.activeActivityPolls.collectAsState()
    val closedActivityPolls by viewModel.closedActivityPolls.collectAsState()
    val candidatesState by viewModel.candidates.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val isTied by viewModel.isTied.collectAsState()
    val memberCount by viewModel.memberCount.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreationSheet by rememberSaveable { mutableStateOf(false) }
    var showWinnerDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    // Poll whose candidates the winner dialog will display (null = destination poll)
    var selectedPollForWinner by remember { mutableStateOf<Poll?>(null) }
    var winnerOverlayData by remember { mutableStateOf<WinnerOverlayData?>(null) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(showWinnerDialog, selectedPollForWinner) {
        viewModel.setScreenPoll(if (showWinnerDialog) selectedPollForWinner else null)
    }

    val isAdmin = currentMember?.role == MemberRole.ADMIN
    val closedPolls = allPolls.filter { it.state == PollState.CLOSED }
    val hasContent = poll != null || activeActivityPolls.isNotEmpty() || closedPolls.isNotEmpty()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Encuesta",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                        )
                    }
                },
                actions = {
                    if (isAdmin && poll != null) {
                        if (poll?.state == PollState.OPEN) {
                            TextButton(
                                onClick = { viewModel.setScreenPoll(null); viewModel.closePoll() },
                                enabled = !isSubmitting,
                            ) {
                                Text("Finalizar")
                            }
                        }
                        TextButton(
                            onClick = { showDeleteDialog = true },
                            enabled = !isSubmitting,
                        ) {
                            Text("Eliminar", color = MaterialTheme.colorScheme.error)
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                !hasContent && candidatesState is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                !hasContent -> {
                    PollEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreatePoll = { showCreationSheet = true },
                    )
                }

                poll == null -> {
                    NoActivePollContent(
                        closedPolls = closedPolls,
                        activeActivityPolls = activeActivityPolls,
                        isAdmin = isAdmin,
                        modifier = Modifier.fillMaxSize(),
                        onCreatePoll = { showCreationSheet = true },
                        onCloseActivity = { ap ->
                            viewModel.setScreenPoll(ap)
                            viewModel.closePoll()
                        },
                        onSelectActivityWinner = { ap ->
                            selectedPollForWinner = ap
                            showWinnerDialog = true
                        },
                    )
                }

                else -> {
                    PollContent(
                        poll = poll!!,
                        candidatesState = candidatesState,
                        activeActivityPolls = activeActivityPolls,
                        isAdmin = isAdmin,
                        isSubmitting = isSubmitting,
                        closedPolls = closedPolls,
                        onToggleVote = { candidateId ->
                            viewModel.setScreenPoll(null)
                            viewModel.toggleVote(candidateId)
                        },
                        onSelectWinner = {
                            selectedPollForWinner = null
                            showWinnerDialog = true
                        },
                        onCloseActivity = { ap ->
                            viewModel.setScreenPoll(ap)
                            viewModel.closePoll()
                        },
                        onSelectActivityWinner = { ap ->
                            selectedPollForWinner = ap
                            showWinnerDialog = true
                        },
                    )
                }
            }
        }
    }

    if (showCreationSheet) {
        PollCreationBottomSheet(
            onDismiss = { showCreationSheet = false },
            onCreate = { type, name, expiresAt ->
                viewModel.createPoll(type, name, expiresAt)
                showCreationSheet = false
            },
        )
    }

    if (showWinnerDialog) {
        val dialogCandidates = (candidatesState as? UiState.Success)?.data ?: emptyList()
        WinnerSelectionDialog(
            candidates = dialogCandidates,
            isTied = isTied,
            onDismiss = { showWinnerDialog = false; selectedPollForWinner = null },
            onSelectWinner = { candidateId ->
                val winner = dialogCandidates.firstOrNull { it.candidate.id == candidateId }
                viewModel.selectWinner(candidateId)
                if (winner != null) {
                    val isDestination = selectedPollForWinner?.type != PollType.ACTIVITY
                    winnerOverlayData = WinnerOverlayData(
                        name = winner.candidate.name,
                        voteCount = winner.voteCount,
                        totalVotes = memberCount,
                        isDestination = isDestination,
                        key = System.currentTimeMillis().toInt(),
                    )
                }
                showWinnerDialog = false
                selectedPollForWinner = null
            },
        )
    }

    winnerOverlayData?.let { data ->
        WinnerOverlay(
            isDestinationPoll = data.isDestination,
            winnerName = data.name,
            voteCount = data.voteCount,
            totalVotes = data.totalVotes,
            animKey = data.key,
            onDismiss = { winnerOverlayData = null },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar encuesta") },
            text = { Text("¿Eliminar esta encuesta y todos sus candidatos? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deletePoll()
                    showDeleteDialog = false
                }) { Text("Eliminar", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun PollEmptyState(
    modifier: Modifier = Modifier,
    onCreatePoll: () -> Unit,
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.HowToVote,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No hay ninguna encuesta activa. ¡Creá una para que el grupo vote!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onCreatePoll) {
            Text("Crear encuesta")
        }
    }
}

@Composable
private fun PollContent(
    poll: Poll,
    candidatesState: UiState<List<PollCandidateUiModel>>,
    activeActivityPolls: List<Poll>,
    isAdmin: Boolean,
    isSubmitting: Boolean,
    closedPolls: List<Poll>,
    onToggleVote: (String) -> Unit,
    onSelectWinner: () -> Unit,
    onCloseActivity: (Poll) -> Unit,
    onSelectActivityWinner: (Poll) -> Unit,
) {
    val isClosed = poll.state == PollState.CLOSED
    var historialExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val chipLabel = when {
                    isClosed -> if (poll.type == PollType.DESTINATION) "Destino · Cerrada" else "Actividad · Cerrada"
                    else -> if (poll.type == PollType.DESTINATION) "Encuesta de Destino" else "Encuesta de Actividad"
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(chipLabel) },
                    colors = if (isClosed)
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    else
                        SuggestionChipDefaults.suggestionChipColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                )
            }
        }

        when (candidatesState) {
            is UiState.Loading -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            is UiState.Error -> {
                item {
                    Text(
                        text = candidatesState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }

            is UiState.Success -> {
                items(candidatesState.data, key = { it.candidate.id }) { uiModel ->
                    PollCandidateCard(
                        uiModel = uiModel,
                        isWinner = uiModel.candidate.placeId == poll.winnerPlaceId,
                        votingEnabled = !isClosed && !isSubmitting,
                        onToggleVote = { onToggleVote(uiModel.candidate.id) },
                    )
                }

                if (candidatesState.data.isEmpty()) {
                    item {
                        Text(
                            text = "Todavía no hay candidatos.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (isClosed && isAdmin && poll.winnerPlaceId == null) {
            item {
                FilledTonalButton(
                    onClick = onSelectWinner,
                    enabled = !isSubmitting,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Seleccionar ganador")
                }
            }
        }

        if (activeActivityPolls.isNotEmpty()) {
            item {
                Text(
                    text = "Encuestas de actividad",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(activeActivityPolls, key = { "act-open-${it.id}" }) { activityPoll ->
                ActivityPollCard(
                    poll = activityPoll,
                    isAdmin = isAdmin,
                    isSubmitting = isSubmitting,
                    onClose = { onCloseActivity(activityPoll) },
                    onSelectWinner = { onSelectActivityWinner(activityPoll) },
                )
            }
        }

        if (closedPolls.isNotEmpty()) {
            item {
                TextButton(
                    onClick = { historialExpanded = !historialExpanded },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                ) {
                    Text(
                        if (historialExpanded) "Ocultar historial" else "Ver historial (${closedPolls.size})",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
            if (historialExpanded) {
                items(closedPolls, key = { "history-${it.id}" }) { closedPoll ->
                    PollHistoryRow(poll = closedPoll)
                }
            }
        }
    }
}

@Composable
private fun NoActivePollContent(
    closedPolls: List<Poll>,
    activeActivityPolls: List<Poll>,
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    onCreatePoll: () -> Unit,
    onCloseActivity: (Poll) -> Unit,
    onSelectActivityWinner: (Poll) -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        if (activeActivityPolls.isNotEmpty()) {
            item {
                Text(
                    text = "Encuestas de actividad",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(activeActivityPolls, key = { "act-${it.id}" }) { activityPoll ->
                ActivityPollCard(
                    poll = activityPoll,
                    isAdmin = isAdmin,
                    isSubmitting = false,
                    onClose = { onCloseActivity(activityPoll) },
                    onSelectWinner = { onSelectActivityWinner(activityPoll) },
                )
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
        if (isAdmin) {
            item {
                Button(
                    onClick = onCreatePoll,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Crear nueva encuesta")
                }
            }
        }
        if (closedPolls.isNotEmpty()) {
            item {
                Text(
                    text = "Historial",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
            items(closedPolls, key = { it.id }) { poll ->
                PollHistoryRow(poll = poll)
            }
        }
    }
}

@Composable
private fun PollHistoryRow(poll: Poll) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SuggestionChip(
            onClick = {},
            label = {
                Text(
                    if (poll.type == PollType.DESTINATION) "Destino" else "Actividad",
                    style = MaterialTheme.typography.labelSmall,
                )
            },
        )
        SuggestionChip(
            onClick = {},
            label = { Text("Cerrada", style = MaterialTheme.typography.labelSmall) },
            colors = SuggestionChipDefaults.suggestionChipColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )
        if (poll.winnerPlaceId != null) {
            SuggestionChip(
                onClick = {},
                label = { Text("Con ganador", style = MaterialTheme.typography.labelSmall) },
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    }
}

@Composable
private fun ActivityPollCard(
    poll: Poll,
    isAdmin: Boolean,
    isSubmitting: Boolean,
    onClose: () -> Unit,
    onSelectWinner: () -> Unit,
) {
    val isClosed = poll.state == PollState.CLOSED
    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text(if (isClosed) "Cerrada" else "Abierta", style = MaterialTheme.typography.labelSmall) },
                colors = if (isClosed)
                    SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                else
                    SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        labelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
            )
            Text(
                text = poll.name.ifBlank { "Encuesta de actividad" },
                style = MaterialTheme.typography.titleSmall,
                fontFamily = FrauncesFamily,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isAdmin && !isClosed) {
                TextButton(onClick = onClose, enabled = !isSubmitting) {
                    Text("Finalizar")
                }
            }
            if (isAdmin && isClosed && poll.winnerPlaceId == null) {
                TextButton(onClick = onSelectWinner, enabled = !isSubmitting) {
                    Text("Ganador")
                }
            }
        }
    }
}

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
                            text = "Ganador",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = candidate.name,
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
                    Text(
                        text = "${uiModel.voteCount}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Nueva encuesta",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tipo de encuesta",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Column(modifier = Modifier.selectableGroup()) {
                listOf(PollType.DESTINATION to "Destino", PollType.ACTIVITY to "Actividad")
                    .forEach { (type, label) ->
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
                            RadioButton(
                                selected = selectedType == type,
                                onClick = null,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
            }

            OutlinedTextField(
                value = pollName,
                onValueChange = { pollName = it },
                label = { Text("Nombre de la encuesta") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Vencimiento (opcional)",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(expiresAt ?: "Seleccionar fecha de vencimiento")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Cancelar")
                }
                FilledTonalButton(
                    onClick = { onCreate(selectedType, pollName, expiresAt) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Crear")
                }
            }
        }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            expiresAt = localDate.atTime(23, 59, 59)
                                .atOffset(ZoneOffset.UTC)
                                .format(java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                        }
                        showDatePicker = false
                    },
                ) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun WinnerSelectionDialog(
    candidates: List<PollCandidateUiModel>,
    isTied: Boolean,
    onDismiss: () -> Unit,
    onSelectWinner: (String) -> Unit,
) {
    if (isTied) {
        val maxVotes = candidates.maxOfOrNull { it.voteCount } ?: 0
        val tiedCandidates = candidates.filter { it.voteCount == maxVotes }.shuffled()
        TiedCoinFlipDialog(
            tiedCandidates = tiedCandidates,
            onDismiss = onDismiss,
            onSelectWinner = onSelectWinner,
        )
    } else {
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
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Text(
                                    text = uiModel.candidate.name,
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
                                        text = "${uiModel.voteCount}",
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
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancelar") }
            },
        )
    }
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
            val landing = if (landFront) 0f else 180f
            rotation.animateTo(1440f + landing, tween(2600, easing = LinearOutSlowInEasing))
            winner = if (landFront) front else back
            winAlpha.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
            isFlipping = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    "¡Hay un empate! 🤝",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
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
                        .graphicsLayer {
                            rotationY = rotation.value
                            scaleX = coinScale.value
                            scaleY = coinScale.value
                        }
                        .clip(CircleShape)
                        .background(Brush.radialGradient(if (showFront) coinFront else coinBack)),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer { scaleX = if (showFront) 1f else -1f },
                    ) {
                        Text(
                            faceName.first().toString(),
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF7A4200),
                        )
                        Text(
                            faceName.take(4).uppercase(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF7A4200).copy(alpha = 0.6f),
                            letterSpacing = 2.sp,
                        )
                    }
                }

                when {
                    winner != null -> {
                        Column(
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
                                Button(onClick = { onSelectWinner(winner!!.candidate.id) }) {
                                    Text("Confirmar")
                                }
                            }
                        }
                    }
                    isFlipping -> Text(
                        "Girando...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    else -> Button(
                        onClick = { flip() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE6A000)),
                    ) {
                        Text("🪙  Tirar moneda", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                TextButton(onClick = onDismiss) { Text("Cancelar") }
            }
        }
    }
}

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
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow,
        ),
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
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "fall",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xED000000))
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { this.alpha = alpha }) {
            confetti.forEach { piece ->
                val t = ((progress + piece.startOffset) * piece.speed) % 1.35f
                val px = piece.xFrac * size.width + sin(t * 6.28f * 1.5f) * piece.wobble * size.width
                val py = (t - 0.15f) * size.height * 1.15f
                if (py < -piece.h || py > size.height + piece.h) return@forEach
                rotate(degrees = t * 540f * piece.speed + piece.rot0, pivot = Offset(px, py)) {
                    drawRect(
                        color = piece.color,
                        topLeft = Offset(px - piece.w / 2f, py - piece.h / 2f),
                        size = Size(piece.w, piece.h),
                    )
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
                text = if (isDestinationPoll) "¡Nos vamos a" else "¡Vamos a",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.72f),
                textAlign = TextAlign.Center,
            )
            Text(
                text = "$winnerName!",
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontFamily = FrauncesFamily,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFF2563EB).copy(alpha = 0.30f),
            ) {
                Text(
                    text = "👍  $voteCount de $totalVotes votos",
                    modifier = Modifier.padding(horizontal = 22.dp, vertical = 9.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "Toca para continuar",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.38f),
            )
        }
    }
}

private data class ConfettiPiece(
    val xFrac: Float,
    val startOffset: Float,
    val color: Color,
    val w: Float,
    val h: Float,
    val speed: Float,
    val rot0: Float,
    val wobble: Float,
)

private fun buildConfetti(): List<ConfettiPiece> {
    val palette = listOf(
        Color(0xFF2563EB), Color(0xFF7C3AED), Color(0xFFD97706),
        Color(0xFF10B981), Color(0xFFEF4444), Color(0xFFEC4899),
        Color(0xFFFFFFFF), Color(0xFF60A5FA), Color(0xFFFBB928),
    )
    return (0..80).map {
        ConfettiPiece(
            xFrac = Random.nextFloat(),
            startOffset = Random.nextFloat(),
            color = palette.random(),
            w = Random.nextFloat() * 14f + 7f,
            h = Random.nextFloat() * 9f + 5f,
            speed = Random.nextFloat() * 0.38f + 0.78f,
            rot0 = Random.nextFloat() * 360f,
            wobble = Random.nextFloat() * 0.055f + 0.008f,
        )
    }
}
