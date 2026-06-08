package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.itinerary.ItineraryEventByDay
import com.hllous.plantravel.presentation.itinerary.ItineraryEventDraft
import com.hllous.plantravel.presentation.itinerary.ItineraryViewModel
import com.hllous.plantravel.ui.theme.FrauncesFamily
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SpanishLocale = Locale("es")
private val DateHeaderFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SpanishLocale)
private val StorageDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    viewModel: ItineraryViewModel,
    navController: NavHostController,
    initialDraft: ItineraryEventDraft? = null,
) {
    val eventsState by viewModel.events.collectAsState()

    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ItineraryEvent?>(null) }
    var pendingDeleteEvent by remember { mutableStateOf<ItineraryEvent?>(null) }

    // If the screen was opened from a POI, open the creation sheet immediately.
    LaunchedEffect(initialDraft) {
        if (initialDraft != null) showSheet = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Itinerario",
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
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEvent = null
                    showSheet = true
                },
            ) {
                Icon(Icons.Default.Add, contentDescription = "Agregar evento")
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (val state = eventsState) {
                is UiState.Loading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                is UiState.Error -> {
                    Text(
                        text = state.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(24.dp),
                    )
                }

                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        ItineraryEmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        ItineraryList(
                            days = state.data,
                            onEditEvent = { event ->
                                editingEvent = event
                                showSheet = true
                            },
                            onDeleteEvent = { event ->
                                pendingDeleteEvent = event
                            },
                        )
                    }
                }
            }
        }
    }

    if (showSheet) {
        EventBottomSheet(
            editingEvent = editingEvent,
            initialDraft = if (editingEvent == null) initialDraft else null,
            onDismiss = { showSheet = false },
            onSave = { name, date, timeOfDay, description, endDate ->
                val event = editingEvent
                if (event != null) {
                    viewModel.updateEvent(event.id, name, date, timeOfDay, description, endDate)
                } else {
                    val draft = if (initialDraft != null) initialDraft else null
                    viewModel.createEvent(
                        name = name,
                        date = date,
                        timeOfDay = timeOfDay,
                        description = description,
                        placeId = draft?.placeId,
                        endDate = endDate,
                    )
                }
                showSheet = false
                editingEvent = null
            },
        )
    }

    pendingDeleteEvent?.let { event ->
        AlertDialog(
            onDismissRequest = { pendingDeleteEvent = null },
            title = { Text("¿Eliminar evento?") },
            text = { Text("\"${event.name}\" será eliminado del itinerario.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteEvent(event.id)
                        pendingDeleteEvent = null
                    },
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteEvent = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ItineraryEmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Default.CalendarMonth,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Todavía no hay eventos. ¡Explorá destinos y empezá a planificar!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

@Composable
private fun ItineraryList(
    days: List<ItineraryEventByDay>,
    onEditEvent: (ItineraryEvent) -> Unit,
    onDeleteEvent: (ItineraryEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 88.dp),
    ) {
        days.forEach { day ->
            stickyHeader(key = day.date) {
                DateSectionHeader(date = day.date)
            }
            items(day.events, key = { it.id }) { event ->
                EventCard(
                    event = event,
                    onEdit = { onEditEvent(event) },
                    onDelete = { onDeleteEvent(event) },
                )
            }
        }
    }
}

@Composable
private fun DateSectionHeader(date: String) {
    val label = remember(date) {
        runCatching {
            val localDate = LocalDate.parse(date, StorageDateFormatter)
            localDate.format(DateHeaderFormatter).replaceFirstChar { it.uppercaseChar() }
        }.getOrElse { date }
    }
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
        )
    }
}

@Composable
private fun EventCard(
    event: ItineraryEvent,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (event.endDate != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${event.date} → ${event.endDate}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (!event.timeOfDay.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.timeOfDay,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
                if (!event.description.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // 48dp touch targets — two icon buttons side by side
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "Editar evento",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar evento",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventBottomSheet(
    editingEvent: ItineraryEvent?,
    initialDraft: ItineraryEventDraft?,
    onDismiss: () -> Unit,
    onSave: (name: String, date: String, timeOfDay: String?, description: String?, endDate: String?) -> Unit,
) {
    val isEditing = editingEvent != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by rememberSaveable { mutableStateOf(editingEvent?.name ?: initialDraft?.name ?: "") }
    var timeOfDay by rememberSaveable { mutableStateOf(editingEvent?.timeOfDay ?: "") }
    var description by rememberSaveable {
        mutableStateOf(editingEvent?.description ?: initialDraft?.description ?: "")
    }

    // Date state — store as "yyyy-MM-dd" string, null means not yet chosen
    var selectedDate by rememberSaveable { mutableStateOf(editingEvent?.date) }
    var selectedEndDate by rememberSaveable { mutableStateOf(editingEvent?.endDate) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let {
            runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        },
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedEndDate?.let {
            runCatching { LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
        },
    )

    val isValid = name.isNotBlank() && selectedDate != null

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = if (isEditing) "Editar evento" else "Nuevo evento",
                style = MaterialTheme.typography.titleLarge,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(4.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // Start date picker trigger field
            OutlinedTextField(
                value = selectedDate?.let { date ->
                    runCatching {
                        LocalDate.parse(date).format(DateHeaderFormatter).replaceFirstChar { c -> c.uppercaseChar() }
                    }.getOrElse { date }
                } ?: "",
                onValueChange = {},
                label = { Text("Fecha *") },
                placeholder = { Text("Seleccioná una fecha") },
                readOnly = true,
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    IconButton(onClick = { showDatePicker = true }) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha")
                    }
                },
            )

            // End date picker — only shown once a start date is chosen
            if (selectedDate != null) {
                OutlinedTextField(
                    value = selectedEndDate?.let { date ->
                        runCatching {
                            LocalDate.parse(date).format(DateHeaderFormatter).replaceFirstChar { c -> c.uppercaseChar() }
                        }.getOrElse { date }
                    } ?: "",
                    onValueChange = {},
                    label = { Text("Fecha de salida (opcional)") },
                    placeholder = { Text("Seleccioná fecha de salida") },
                    readOnly = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { showEndDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Seleccionar fecha de salida")
                        }
                    },
                )
            }

            OutlinedTextField(
                value = timeOfDay,
                onValueChange = { timeOfDay = it },
                label = { Text("Hora (opcional)") },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                minLines = 2,
                maxLines = 4,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(4.dp))

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
                Button(
                    onClick = {
                        onSave(
                            name.trim(),
                            selectedDate!!,
                            timeOfDay.trim().ifBlank { null },
                            description.trim().ifBlank { null },
                            selectedEndDate,
                        )
                    },
                    enabled = isValid,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (isEditing) "Guardar" else "Agregar")
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
                            selectedDate = localDate.format(StorageDateFormatter)
                            // Reset end date if it's now before the new start date
                            val parsedEnd = selectedEndDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            if (parsedEnd != null && parsedEnd < localDate) selectedEndDate = null
                        }
                        showDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showEndDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDatePickerState.selectedDateMillis?.let { millis ->
                            val localDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            val startDate = selectedDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
                            if (startDate == null || localDate >= startDate) {
                                selectedEndDate = localDate.format(StorageDateFormatter)
                            }
                        }
                        showEndDatePicker = false
                    },
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancelar")
                }
            },
        ) {
            DatePicker(state = endDatePickerState)
        }
    }
}
