package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HowToVote
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PollScreen(
    viewModel: PollViewModel,
    navController: NavHostController,
) {
    val poll by viewModel.poll.collectAsState()
    val allPolls by viewModel.allPolls.collectAsState()
    val candidatesState by viewModel.candidates.collectAsState()
    val currentMember by viewModel.currentMember.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showCreationSheet by rememberSaveable { mutableStateOf(false) }
    var showWinnerDialog by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var winnerSnackbarShown by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(errorMessage) {
        if (errorMessage != null) {
            snackbarHostState.showSnackbar(errorMessage!!)
            viewModel.clearErrorMessage()
        }
    }

    val activePoll = poll
    LaunchedEffect(activePoll?.winnerPlaceId) {
        if (activePoll?.winnerPlaceId != null && !winnerSnackbarShown) {
            winnerSnackbarShown = true
            val msg = if (activePoll.type == PollType.DESTINATION) "¡Destino establecido!" else "¡Actividad seleccionada!"
            snackbarHostState.showSnackbar(msg)
        }
    }

    val isAdmin = currentMember?.role == MemberRole.ADMIN
    val closedPolls = allPolls.filter { it.state == PollState.CLOSED }
    val hasActivePoll = poll?.state == PollState.OPEN

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
                            TextButton(onClick = { viewModel.closePoll() }) {
                                Text("Cerrar")
                            }
                        }
                        TextButton(onClick = { showDeleteDialog = true }) {
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
                allPolls.isEmpty() && candidatesState is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                allPolls.isEmpty() -> {
                    PollEmptyState(
                        modifier = Modifier.align(Alignment.Center),
                        onCreatePoll = { showCreationSheet = true },
                    )
                }

                !hasActivePoll -> {
                    NoActivePollContent(
                        closedPolls = closedPolls,
                        isAdmin = isAdmin,
                        modifier = Modifier.fillMaxSize(),
                        onCreatePoll = { showCreationSheet = true },
                    )
                }

                else -> {
                    val currentPoll = poll!!
                    PollContent(
                        poll = currentPoll,
                        candidatesState = candidatesState,
                        isAdmin = isAdmin,
                        closedPolls = closedPolls,
                        onToggleVote = { candidateId -> viewModel.toggleVote(candidateId) },
                        onSelectWinner = { showWinnerDialog = true },
                    )
                }
            }
        }
    }

    if (showCreationSheet) {
        PollCreationBottomSheet(
            onDismiss = { showCreationSheet = false },
            onCreate = { type, expiresAt ->
                viewModel.createPoll(type, expiresAt)
                showCreationSheet = false
            },
        )
    }

    if (showWinnerDialog) {
        val candidates = (candidatesState as? UiState.Success)?.data ?: emptyList()
        WinnerSelectionDialog(
            candidates = candidates,
            onDismiss = { showWinnerDialog = false },
            onSelectWinner = { candidateId ->
                viewModel.selectWinner(candidateId)
                showWinnerDialog = false
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
    isAdmin: Boolean,
    closedPolls: List<Poll>,
    onToggleVote: (String) -> Unit,
    onSelectWinner: () -> Unit,
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
                        votingEnabled = !isClosed,
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text("Seleccionar ganador")
                }
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
    isAdmin: Boolean,
    modifier: Modifier = Modifier,
    onCreatePoll: () -> Unit,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
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
            item { Spacer(modifier = Modifier.height(16.dp)) }
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
    onCreate: (type: PollType, expiresAt: String?) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedType by rememberSaveable { mutableStateOf(PollType.DESTINATION) }
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
                                    onClick = { selectedType = type },
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
                    onClick = { onCreate(selectedType, expiresAt) },
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
