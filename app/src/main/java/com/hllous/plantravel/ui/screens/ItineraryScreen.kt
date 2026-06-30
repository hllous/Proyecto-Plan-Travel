package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import android.content.Intent
import android.net.Uri
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.allowHardware
import androidx.compose.ui.draw.shadow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Museum
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import androidx.navigation.NavHostController
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItineraryEvent
import com.hllous.plantravel.domain.model.PlaceResult
import com.hllous.plantravel.domain.model.PollCandidate
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

private val SpanishLocale = Locale.forLanguageTag("es")
private val DateHeaderFormatter = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", SpanishLocale)
private val ShortDateFormatter = DateTimeFormatter.ofPattern("d MMM", SpanishLocale)
private val StorageDateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
private const val TimelineStartHour = 0
private const val TimelineEndHour = 23
private val TimelineHourHeight = 56.dp
private val GanttLabelColumnWidth = 144.dp
private val GanttDayCellWidth = 44.dp
private data class TimelineDaySection(
    val date: LocalDate,
    val dayNumber: Int,
    val spans: List<ActiveSpanEvent>,
    val noTimeEvents: List<ItineraryEvent>,
    val timedEvents: List<ItineraryEvent>,
    val startHour: Int,
    val endHour: Int,
)

private data class ActiveSpanEvent(
    val event: ItineraryEvent,
    val dayIndex: Int,
    val totalDays: Int,
)

private data class ActivityDescriptor(
    val label: String,
    val description: String,
    val icon: ImageVector,
)

private enum class SpanPosition { Start, Middle, End, Solo }

private fun formatTimeInput(input: String): String {
    val digits = input.filter { it.isDigit() }.take(4)
    return if (digits.length >= 3) "${digits.take(2)}:${digits.drop(2)}" else digits
}

internal fun formatTimeInputValue(previous: TextFieldValue, next: TextFieldValue): TextFieldValue {
    val formatted = formatTimeInput(next.text)
    val selectionAtEnd = TextRange(formatted.length)
    return if (formatted == previous.text && next.selection == previous.selection) previous
    else TextFieldValue(text = formatted, selection = selectionAtEnd)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItineraryScreen(
    viewModel: ItineraryViewModel,
    navController: NavHostController,
    initialDraft: ItineraryEventDraft? = null,
) {
    val eventsState by viewModel.events.collectAsState()
    val isSubmitting by viewModel.isSubmitting.collectAsState()
    val message by viewModel.message.collectAsState()
    val activityCandidates by viewModel.activityCandidates.collectAsState()
    val members by viewModel.members.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var editingEvent by remember { mutableStateOf<ItineraryEvent?>(null) }
    var pendingDeleteEvent by remember { mutableStateOf<ItineraryEvent?>(null) }

    LaunchedEffect(message) {
        message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    LaunchedEffect(initialDraft) {
        if (initialDraft != null) showSheet = true
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp),
                onClick = {
                    if (!isSubmitting) {
                        editingEvent = null
                        showSheet = true
                    }
                },
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Icon(Icons.Default.Add, contentDescription = "Agregar evento")
                }
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
                    val events = remember(state.data) { state.data.flatMap(ItineraryEventByDay::events).distinctBy(ItineraryEvent::id) }
                    if (events.isEmpty()) {
                        ItineraryEmptyState(modifier = Modifier.align(Alignment.Center))
                    } else {
                        ItineraryList(
                            events = events,
                            members = members,
                            activityCandidates = activityCandidates,
                            onEditEvent = { event ->
                                editingEvent = event
                                showSheet = true
                            },
                            onMoveEvent = { event, newDate, newTime ->
                                viewModel.updateEvent(
                                    eventId = event.id,
                                    name = event.name,
                                    date = newDate,
                                    timeOfDay = newTime,
                                    description = event.description,
                                    endDate = event.endDate,
                                )
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
            activityCandidates = activityCandidates,
            onDismiss = { showSheet = false },
            onSave = { name, date, timeOfDay, description, endDate, placeId ->
                val event = editingEvent
                if (event != null) {
                    viewModel.updateEvent(event.id, name, date, timeOfDay, description, endDate)
                } else {
                    viewModel.createEvent(
                        name = name,
                        date = date,
                        timeOfDay = timeOfDay,
                        description = description,
                        placeId = placeId ?: initialDraft?.placeId,
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
                    enabled = !isSubmitting,
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
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ItineraryList(
    events: List<ItineraryEvent>,
    members: List<GroupMember>,
    activityCandidates: List<PollCandidate>,
    onEditEvent: (ItineraryEvent) -> Unit,
    onMoveEvent: (ItineraryEvent, String, String?) -> Unit,
    onDeleteEvent: (ItineraryEvent) -> Unit,
) {
    val timelineDays = remember(events) { buildTimelineDays(events) }
    val multiDayEvents = remember(events) { events.filter(::isMultiDayEvent).sortedBy(ItineraryEvent::date) }
    val dayKeys = remember(timelineDays) { timelineDays.map { it.date.format(StorageDateFormatter) } }
    var expandedDays by rememberSaveable(dayKeys) { mutableStateOf(emptySet<String>()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        item(key = "hint-banner") {
            TimelineHintBanner()
        }

        if (multiDayEvents.isNotEmpty()) {
            item(key = "multi-day-overview") {
                MultiDayOverviewCard(
                    timelineDays = timelineDays,
                    multiDayEvents = multiDayEvents,
                )
            }
        }

        timelineDays.forEach { day ->
            val dayKey = day.date.format(StorageDateFormatter)
            val canFold = day.timedEvents.isNotEmpty()
            val isCollapsed = canFold && dayKey !in expandedDays
            stickyHeader(key = "header-${day.date}") {
                DateSectionHeader(
                    day = day,
                    isCollapsed = isCollapsed,
                    canFold = canFold,
                    onToggle = {
                        expandedDays = expandedDays.toMutableSet().also { set ->
                            if (!set.add(dayKey)) set.remove(dayKey)
                        }
                    },
                )
            }

            if (day.spans.isNotEmpty()) {
                item(key = "spans-${day.date}") {
                    ActiveSpanStrip(
                        spans = day.spans,
                        onEditEvent = onEditEvent,
                        onDeleteEvent = onDeleteEvent,
                    )
                }
            }

            if (day.noTimeEvents.isNotEmpty()) {
                item(key = "notime-${day.date}") {
                    NoTimeEventsSection(
                        events = day.noTimeEvents,
                        onEditEvent = onEditEvent,
                        onDeleteEvent = onDeleteEvent,
                    )
                }
            }

            if (day.timedEvents.isNotEmpty()) {
                item(key = "timed-${day.date}") {
                    TimedEventsTimeline(
                        day = day,
                        members = members,
                        activityCandidates = activityCandidates,
                        condensed = isCollapsed,
                        onEditEvent = onEditEvent,
                        onMoveEvent = onMoveEvent,
                        onDeleteEvent = onDeleteEvent,
                    )
                }
            } else if (day.noTimeEvents.isEmpty() && day.spans.isEmpty()) {
                item(key = "empty-${day.date}") {
                    EmptyDayState()
                }
            }
        }
    }
}

@Composable
private fun TimelineHintBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Tocá una tarjeta para ver el detalle. Mantené presionado y arrastrá para mover el horario. Tocá el encabezado del día para expandirlo.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MultiDayOverviewCard(
    timelineDays: List<TimelineDaySection>,
    multiDayEvents: List<ItineraryEvent>,
) {
    val scrollState = rememberScrollState()
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        color = colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "Alojamiento y reservas multi-día",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )

            Column(
                modifier = Modifier.horizontalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Spacer(modifier = Modifier.width(GanttLabelColumnWidth))
                    timelineDays.forEach { day ->
                        Column(
                            modifier = Modifier.width(GanttDayCellWidth),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, SpanishLocale),
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                            )
                            Text(
                                text = day.date.dayOfMonth.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }

                multiDayEvents.forEach { event ->
                    val activeDates = eventDateRange(event)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            modifier = Modifier.width(GanttLabelColumnWidth),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = colorScheme.primaryContainer,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = eventTypeIcon(event),
                                        contentDescription = null,
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            }
                            Text(
                                text = event.name,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }

                        timelineDays.forEach { day ->
                            val position = spanPosition(event, day.date)
                            val cellColor = when (position) {
                                SpanPosition.Start, SpanPosition.Solo -> colorScheme.primary
                                SpanPosition.Middle -> colorScheme.primary.copy(alpha = 0.45f)
                                SpanPosition.End -> colorScheme.tertiary
                                null -> Color.Transparent
                            }
                            val shape = when (position) {
                                SpanPosition.Start -> RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)
                                SpanPosition.Middle -> RoundedCornerShape(0.dp)
                                SpanPosition.End -> RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp)
                                SpanPosition.Solo -> RoundedCornerShape(10.dp)
                                null -> RoundedCornerShape(0.dp)
                            }
                            val horizontalPadding = when (position) {
                                SpanPosition.Start, SpanPosition.End, SpanPosition.Solo -> 2.dp
                                SpanPosition.Middle, null -> 0.dp
                            }
                            Box(
                                modifier = Modifier
                                    .width(GanttDayCellWidth)
                                    .height(20.dp)
                                    .padding(horizontal = horizontalPadding, vertical = 1.dp)
                                    .background(cellColor, shape),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DateSectionHeader(
    day: TimelineDaySection,
    isCollapsed: Boolean,
    canFold: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = canFold,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary,
            ) {
                Text(
                    text = "Día ${day.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            Text(
                text = day.date.format(DateHeaderFormatter).replaceFirstChar(Char::uppercaseChar),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (canFold) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = condensedTimelineLabel(day),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (isCollapsed) "Compacto" else "Completo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium,
                        )
                        Icon(
                            imageVector = if (isCollapsed) Icons.Default.ExpandMore else Icons.Default.ExpandLess,
                            contentDescription = if (isCollapsed) "Expandir día" else "Contraer día",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveSpanStrip(
    spans: List<ActiveSpanEvent>,
    onEditEvent: (ItineraryEvent) -> Unit,
    onDeleteEvent: (ItineraryEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        spans.forEach { span ->
            val event = span.event
            var showActions by remember(event.id) { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = { showActions = true },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.14f),
                        modifier = Modifier.size(34.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = eventTypeIcon(event),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${formatShortDate(event.date)} → ${formatShortDate(event.endDate ?: event.date)}${event.description?.let { " · $it" } ?: ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.tertiary,
                    ) {
                        Text(
                            text = "Día ${span.dayIndex}/${span.totalDays}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }
            if (showActions) {
                EventActionsDialog(
                    eventName = event.name,
                    onDismiss = { showActions = false },
                    onEdit = {
                        showActions = false
                        onEditEvent(event)
                    },
                    onDelete = {
                        showActions = false
                        onDeleteEvent(event)
                    },
                )
            }
        }
    }
}

@Composable
private fun NoTimeEventsSection(
    events: List<ItineraryEvent>,
    onEditEvent: (ItineraryEvent) -> Unit,
    onDeleteEvent: (ItineraryEvent) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = "Sin hora fija",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        events.forEach { event ->
            var showActions by remember(event.id) { mutableStateOf(false) }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                        onLongClick = { showActions = true },
                    ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, top = 10.dp, end = 4.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = eventTypeIcon(event),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!event.description.isNullOrBlank()) {
                            Text(
                                text = event.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
            if (showActions) {
                EventActionsDialog(
                    eventName = event.name,
                    onDismiss = { showActions = false },
                    onEdit = {
                        showActions = false
                        onEditEvent(event)
                    },
                    onDelete = {
                        showActions = false
                        onDeleteEvent(event)
                    },
                )
            }
        }
    }
}

@Composable
private fun TimedEventsTimeline(
    day: TimelineDaySection,
    members: List<GroupMember>,
    activityCandidates: List<PollCandidate>,
    condensed: Boolean,
    onEditEvent: (ItineraryEvent) -> Unit,
    onMoveEvent: (ItineraryEvent, String, String?) -> Unit,
    onDeleteEvent: (ItineraryEvent) -> Unit,
) {
    val bounds = remember(day.timedEvents, condensed) { timelineBoundsForDay(day.timedEvents, condensed) }
    val totalHours = (bounds.last - bounds.first + 1).coerceAtLeast(1)
    val timelineHeight = TimelineHourHeight * totalHours
    val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
    val connectorColor = MaterialTheme.colorScheme.primary
    val connectorLineColor = connectorColor.copy(alpha = 0.45f)
    val pinHighlightColor = MaterialTheme.colorScheme.onPrimaryContainer
    val dragGuideColor = MaterialTheme.colorScheme.primary
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val hourHeightPx = with(density) { TimelineHourHeight.toPx() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 6.dp, end = 12.dp, top = 8.dp, bottom = 14.dp),
    ) {
        Column(
            modifier = Modifier
                .width(48.dp)
                .height(timelineHeight),
        ) {
            repeat(totalHours) { index ->
                val hour = bounds.first + index
                Box(
                    modifier = Modifier.height(TimelineHourHeight),
                    contentAlignment = Alignment.TopEnd,
                ) {
                    Text(
                        text = "%02d:00".format(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp, end = 8.dp),
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .height(timelineHeight),
        ) {
            Canvas(modifier = Modifier.matchParentSize()) {
                repeat(totalHours + 1) { index ->
                    val y = index * TimelineHourHeight.toPx()
                    drawLine(
                        color = gridLineColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                    )
                }
            }

            day.timedEvents.forEach { event ->
                val topOffset = eventTopOffset(event, bounds.first)
                val cardMinHeight = if (event.description.isNullOrBlank()) 64.dp else 82.dp
                val topOffsetPx = with(density) { topOffset.toPx() }
                val pinOffsetPx = with(density) { (cardMinHeight / 2).toPx() }
                val cardBaseOffsetPx = (topOffsetPx - pinOffsetPx).coerceAtLeast(0f)
                val cardBaseOffset = with(density) { cardBaseOffsetPx.toDp() }
                val timelineHeightPx = with(density) { timelineHeight.toPx() }
                var dragOffsetPx by remember(event.id) { mutableStateOf(0f) }
                var isDragging by remember(event.id) { mutableStateOf(false) }
                var dragPreviewMinutes by remember(event.id) { mutableStateOf<Int?>(null) }
                var showBubble by remember(event.id) { mutableStateOf(false) }
                var showPlaceSheet by remember(event.id) { mutableStateOf(false) }
                val creator = remember(event.id, members) { members.firstOrNull { it.id == event.createdByMemberId } }
                val relatedPlace = remember(event.id, event.placeId, event.description, activityCandidates) {
                    resolveItineraryPlace(event, activityCandidates)
                }
                val draggableState = remember(event.id, day.date, topOffsetPx, timelineHeightPx, hapticFeedback) {
                    object {
                        private fun previewMinutesFor(offsetPx: Float): Int =
                            snapMinutesToQuarterHour(
                                (bounds.first * 60) + ((((cardBaseOffsetPx + pinOffsetPx + offsetPx) / hourHeightPx) * 60f).roundToInt())
                            )

                        fun applyDelta(delta: Float) {
                            val maxOffset = (timelineHeightPx - cardBaseOffsetPx - with(density) { cardMinHeight.toPx() }).coerceAtLeast(0f)
                            dragOffsetPx = (dragOffsetPx + delta).coerceIn(-cardBaseOffsetPx, maxOffset)
                            dragPreviewMinutes = previewMinutesFor(dragOffsetPx)
                        }

                        fun start() {
                            isDragging = true
                            showBubble = false
                            dragPreviewMinutes = previewMinutesFor(dragOffsetPx)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        }

                        fun commit() {
                            val droppedMinutes = dragPreviewMinutes ?: previewMinutesFor(dragOffsetPx)
                            val newTime = minutesToTimeString(droppedMinutes)
                            if (newTime != normalizeTimeForDisplay(event.timeOfDay)) {
                                onMoveEvent(event, day.date.format(StorageDateFormatter), newTime)
                                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            }
                            isDragging = false
                            dragPreviewMinutes = null
                            dragOffsetPx = 0f
                        }

                        fun cancel() {
                            isDragging = false
                            dragPreviewMinutes = null
                            dragOffsetPx = 0f
                        }
                    }
                }
                val previewOffset = dragPreviewMinutes?.let { previewMinutes ->
                    ((((previewMinutes - (bounds.first * 60)).coerceAtLeast(0)) / 60f) * TimelineHourHeight.value).dp
                }
                if (isDragging && previewOffset != null && dragPreviewMinutes != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = (previewOffset - 24.dp).coerceAtLeast(0.dp))
                            .zIndex(3f),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                modifier = Modifier.align(Alignment.End),
                            ) {
                                Text(
                                    text = minutesToTimeString(dragPreviewMinutes!!),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                )
                            }
                            Canvas(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(18.dp),
                            ) {
                                val lineY = 6.dp.toPx()
                                drawCircle(
                                    color = dragGuideColor,
                                    radius = 4.dp.toPx(),
                                    center = Offset(6.dp.toPx(), lineY),
                                )
                                drawLine(
                                    color = dragGuideColor,
                                    start = Offset(10.dp.toPx(), lineY),
                                    end = Offset(size.width, lineY),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = cardBaseOffset + with(density) { dragOffsetPx.toDp() })
                        .padding(start = 6.dp, end = 4.dp),
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .zIndex(if (showBubble) 2f else 0f),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Canvas(
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(cardMinHeight),
                            ) {
                                val y = size.height / 2f
                                val pinHeadRadius = 6.dp.toPx()
                                val pinCenterX = 6.dp.toPx()
                                drawCircle(
                                    color = connectorColor,
                                    radius = pinHeadRadius,
                                    center = Offset(pinCenterX, y),
                                )
                                drawCircle(
                                    color = pinHighlightColor,
                                    radius = 2.dp.toPx(),
                                    center = Offset(pinCenterX - 1.dp.toPx(), y - 1.dp.toPx()),
                                )
                                drawLine(
                                    color = connectorColor,
                                    start = Offset(pinCenterX, y + pinHeadRadius - 1.dp.toPx()),
                                    end = Offset(pinCenterX + 2.dp.toPx(), y + 16.dp.toPx()),
                                    strokeWidth = 1.5.dp.toPx(),
                                )
                                drawLine(
                                    color = connectorLineColor,
                                    start = Offset(pinCenterX + pinHeadRadius, y),
                                    end = Offset(size.width, y),
                                    strokeWidth = 2.dp.toPx(),
                                )
                            }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDragging) {
                                        MaterialTheme.colorScheme.secondaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.primaryContainer
                                    }
                                ),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isDragging) 6.dp else 1.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = cardMinHeight)
                                    .zIndex(if (isDragging) 4f else 0f)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onClick = {
                                            showBubble = !showBubble
                                        },
                                    )
                                    .pointerInput(event.id, day.date, topOffsetPx, cardBaseOffsetPx, timelineHeightPx) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = {
                                                draggableState.start()
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                draggableState.applyDelta(dragAmount.y)
                                            },
                                            onDragEnd = {
                                                draggableState.commit()
                                            },
                                            onDragCancel = {
                                                draggableState.cancel()
                                            },
                                        )
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 0.dp, top = 8.dp, end = 10.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(0.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(4.dp)
                                            .heightIn(min = cardMinHeight)
                                            .background(MaterialTheme.colorScheme.primary),
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp, vertical = 2.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = normalizeTimeForDisplay(event.timeOfDay),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                                fontWeight = FontWeight.Bold,
                                            )
                                            Icon(
                                                imageVector = eventTypeIcon(event),
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(14.dp),
                                            )
                                        }
                                        Text(
                                            text = event.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        if (!event.description.isNullOrBlank()) {
                                            Text(
                                                text = event.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        InlineCreatorChip(
                                            creator = creator,
                                            fallbackMemberId = event.createdByMemberId,
                                        )
                                    }
                                }
                            }
                        }

                        if (showBubble) {
                            EventInfoBubble(
                                event = event,
                                creator = creator,
                                relatedPlace = relatedPlace,
                                onOpenPlace = { showPlaceSheet = true },
                                onEdit = {
                                    showBubble = false
                                    onEditEvent(event)
                                },
                                onDelete = {
                                    showBubble = false
                                    onDeleteEvent(event)
                                },
                            )
                        }
                        if (showPlaceSheet && relatedPlace != null) {
                            ItineraryPlaceBottomSheet(
                                place = relatedPlace,
                                onDismiss = { showPlaceSheet = false },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyDayState() {
    Text(
        text = "Sin eventos programados",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    )
}

@Composable
private fun EventInfoBubble(
    event: ItineraryEvent,
    creator: GroupMember?,
    relatedPlace: PlaceResult?,
    onOpenPlace: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 8.dp)
            .shadow(8.dp, RoundedCornerShape(18.dp)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Detalle del evento",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
            )
            BubbleField("Hora", normalizeTimeForDisplay(event.timeOfDay))
            BubbleField("Nombre", event.name)
            if (!event.description.isNullOrBlank()) {
                BubbleField("Descripción", event.description)
            }
            BubbleField("Día", formatHeaderDate(event.date))
            CreatorBadge(creator = creator, fallbackMemberId = event.createdByMemberId)
            if (!event.description.isNullOrBlank()) {
                BubbleField("Ubicación", event.description)
            }
            if (relatedPlace != null) {
                ItineraryPlacePreviewCard(
                    place = relatedPlace,
                    onClick = onOpenPlace,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Editar")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun EventActionsDialog(
    eventName: String,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(eventName) },
        text = { Text("Elegí una acción para este evento.") },
        confirmButton = {
            Button(onClick = onEdit) {
                Text("Editar")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onDelete) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        titleContentColor = MaterialTheme.colorScheme.onSurface,
        textContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun BubbleField(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun CreatorBadge(
    creator: GroupMember?,
    fallbackMemberId: String,
) {
    val memberName = creator?.name ?: "Miembro"
    val accent = memberAccentColor(creator?.id ?: fallbackMemberId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(accent.copy(alpha = 0.22f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = memberInitials(memberName),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Agregado por",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = memberName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun InlineCreatorChip(
    creator: GroupMember?,
    fallbackMemberId: String,
) {
    val memberName = creator?.name ?: "Miembro"
    val accent = memberAccentColor(creator?.id ?: fallbackMemberId)
    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .background(accent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = memberInitials(memberName),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = accent,
            )
        }
        Text(
            text = "Agregado por $memberName",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun ItineraryPlacePreviewCard(
    place: PlaceResult,
    onClick: () -> Unit,
) {
    val context = LocalContext.current
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (place.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(place.photoUrl)
                        .allowHardware(false)
                        .build(),
                    contentDescription = place.name,
                    modifier = Modifier
                        .size(68.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(68.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Actividad",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (place.address.isNotBlank()) {
                    Text(
                        text = place.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.84f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Ver detalle de la actividad",
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItineraryPlaceBottomSheet(
    place: PlaceResult,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
        ) {
            if (place.photoUrl.isNotBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(place.photoUrl)
                        .allowHardware(false)
                        .build(),
                    contentDescription = place.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .padding(horizontal = 16.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp)),
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 24.dp)) {
                Text(
                    text = place.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                )

                if (place.rating != 0.0) {
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

                if (place.reviews.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    val pagerState = rememberPagerState { place.reviews.size }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Reseñas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (place.reviews.size > 1) {
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                repeat(place.reviews.size) { i ->
                                    Box(
                                        modifier = Modifier
                                            .size(if (i == pagerState.currentPage) 7.dp else 5.dp)
                                            .background(
                                                if (i == pagerState.currentPage)
                                                    MaterialTheme.colorScheme.primary
                                                else
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                CircleShape,
                                            )
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxWidth(),
                        beyondViewportPageCount = 0,
                    ) { page ->
                        val review = place.reviews[page]
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = review.authorName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = review.relativeTime,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                repeat(5) { i ->
                                    Icon(
                                        Icons.Default.Star,
                                        contentDescription = null,
                                        tint = if (i < review.rating) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.outlineVariant,
                                        modifier = Modifier.size(13.dp),
                                    )
                                }
                            }
                            if (review.text.isNotBlank()) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = review.text,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 3,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventBottomSheet(
    editingEvent: ItineraryEvent?,
    initialDraft: ItineraryEventDraft?,
    activityCandidates: List<PollCandidate>,
    onDismiss: () -> Unit,
    onSave: (name: String, date: String, timeOfDay: String?, description: String?, endDate: String?, placeId: String?) -> Unit,
) {
    val isEditing = editingEvent != null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialTimeValue = remember(editingEvent?.id, initialDraft?.placeId) {
        val text = normalizeTimeForDisplay(editingEvent?.timeOfDay)
        TextFieldValue(text = text, selection = TextRange(text.length))
    }

    var name by rememberSaveable { mutableStateOf(editingEvent?.name ?: initialDraft?.name ?: "") }
    var timeOfDay by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(initialTimeValue) }
    var description by rememberSaveable {
        mutableStateOf(editingEvent?.description ?: initialDraft?.description ?: "")
    }
    var selectedPlaceId by rememberSaveable { mutableStateOf(editingEvent?.placeId ?: initialDraft?.placeId) }
    var selectedDate by rememberSaveable { mutableStateOf(editingEvent?.date ?: initialDraft?.date) }
    var selectedEndDate by rememberSaveable { mutableStateOf(editingEvent?.endDate) }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showEndDatePicker by rememberSaveable { mutableStateOf(false) }

    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate?.let {
            runCatching {
                LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
        },
    )
    val endDatePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedEndDate?.let {
            runCatching {
                LocalDate.parse(it).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            }.getOrNull()
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

            if (!isEditing && activityCandidates.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Actividades de la encuesta",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(activityCandidates, key = { it.id }) { candidate ->
                            FilterChip(
                                selected = candidate.placeId == selectedPlaceId,
                                onClick = {
                                    if (candidate.placeId == selectedPlaceId) {
                                        selectedPlaceId = null
                                        if (name == candidate.name) name = ""
                                    } else {
                                        name = candidate.name
                                        selectedPlaceId = candidate.placeId
                                    }
                                },
                                label = { Text(candidate.name) },
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = selectedDate?.let { formatHeaderDate(it) } ?: "",
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

            if (selectedDate != null) {
                OutlinedTextField(
                    value = selectedEndDate?.let { formatHeaderDate(it) } ?: "",
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
                onValueChange = { timeOfDay = formatTimeInputValue(timeOfDay, it) },
                label = { Text("Hora (opcional)") },
                placeholder = { Text("HH:MM") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
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
                            normalizeTimeForStorage(timeOfDay.text),
                            description.trim().ifBlank { null },
                            selectedEndDate,
                            selectedPlaceId,
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
                            val parsedEnd = selectedEndDate?.let {
                                runCatching { LocalDate.parse(it) }.getOrNull()
                            }
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

private fun buildTimelineDays(events: List<ItineraryEvent>): List<TimelineDaySection> {
    val uniqueEvents = events.distinctBy(ItineraryEvent::id)
    val allDates = uniqueEvents
        .flatMap(::eventDateRange)
        .distinct()
        .sorted()

    return allDates.mapIndexed { index, date ->
        val activeEvents = uniqueEvents.filter { date in eventDateRange(it) }
        val spans = activeEvents
            .filter(::isMultiDayEvent)
            .sortedWith(compareBy<ItineraryEvent> { it.date }.thenBy { it.name.lowercase(SpanishLocale) })
            .map { event ->
                val range = eventDateRange(event)
                ActiveSpanEvent(
                    event = event,
                    dayIndex = range.indexOf(date) + 1,
                    totalDays = range.size,
                )
            }
        val singleDayEvents = activeEvents
            .filterNot(::isMultiDayEvent)
            .sortedWith(compareBy<ItineraryEvent> { it.timeOfDay ?: "99:99" }.thenBy { it.name.lowercase(SpanishLocale) })
        val timedEvents = singleDayEvents.filter { !it.timeOfDay.isNullOrBlank() }
        val noTimeEvents = singleDayEvents.filter { it.timeOfDay.isNullOrBlank() }
        val bounds = timelineBoundsForDay(timedEvents)

        TimelineDaySection(
            date = date,
            dayNumber = index + 1,
            spans = spans,
            noTimeEvents = noTimeEvents,
            timedEvents = timedEvents,
            startHour = bounds.first,
            endHour = bounds.last,
        )
    }
}

internal fun timelineBoundsForDay(timedEvents: List<ItineraryEvent>): IntRange {
    return timelineBoundsForDay(timedEvents, condensed = false)
}

internal fun timelineBoundsForDay(
    timedEvents: List<ItineraryEvent>,
    condensed: Boolean,
): IntRange {
    if (timedEvents.isEmpty()) return TimelineStartHour..TimelineEndHour
    if (!condensed) return TimelineStartHour..TimelineEndHour
    val minutes = timedEvents.mapNotNull { it.timeOfDay?.let(::timeToMinutes) }
    if (minutes.isEmpty()) return TimelineStartHour..TimelineEndHour
    val startHour = (minutes.minOrNull()!! / 60).coerceIn(TimelineStartHour, TimelineEndHour)
    val endHour = ((minutes.maxOrNull()!! / 60) + 1).coerceIn(startHour, TimelineEndHour)
    return startHour..endHour
}

private fun eventDateRange(event: ItineraryEvent): List<LocalDate> {
    val start = parseStorageDate(event.date) ?: return emptyList()
    val rawEnd = event.endDate?.let(::parseStorageDate) ?: start
    val end = if (rawEnd.isBefore(start)) start else rawEnd
    return generateSequence(start) { current ->
        current.plusDays(1).takeIf { !it.isAfter(end) }
    }.toList()
}

private fun spanPosition(event: ItineraryEvent, date: LocalDate): SpanPosition? {
    val range = eventDateRange(event)
    if (date !in range) return null
    return when {
        range.size == 1 -> SpanPosition.Solo
        date == range.first() -> SpanPosition.Start
        date == range.last() -> SpanPosition.End
        else -> SpanPosition.Middle
    }
}

private fun isMultiDayEvent(event: ItineraryEvent): Boolean =
    event.endDate != null && event.endDate != event.date

private fun parseStorageDate(value: String): LocalDate? =
    runCatching { LocalDate.parse(value, StorageDateFormatter) }.getOrNull()

private fun formatShortDate(value: String): String =
    parseStorageDate(value)?.format(ShortDateFormatter)?.replaceFirstChar(Char::uppercaseChar) ?: value

private fun formatHeaderDate(value: String): String =
    parseStorageDate(value)?.format(DateHeaderFormatter)?.replaceFirstChar(Char::uppercaseChar) ?: value

private fun normalizeTimeForDisplay(value: String?): String {
    if (value.isNullOrBlank()) return ""
    val parts = value.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return value.take(5)
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return "%02d:%02d".format(hours, minutes)
}

private fun normalizeTimeForStorage(value: String): String? =
    normalizeTimeForDisplay(value).ifBlank { null }

private fun timeToMinutes(time: String): Int {
    val parts = time.split(":")
    val hours = parts.getOrNull(0)?.toIntOrNull() ?: return 0
    val minutes = parts.getOrNull(1)?.toIntOrNull() ?: 0
    return hours * 60 + minutes
}

private fun minutesToTimeString(totalMinutes: Int): String {
    val clamped = totalMinutes.coerceIn(0, (23 * 60) + 59)
    val hours = clamped / 60
    val minutes = clamped % 60
    return "%02d:%02d".format(hours, minutes)
}

private fun snapMinutesToQuarterHour(totalMinutes: Int): Int =
    ((totalMinutes / 15f).roundToInt() * 15).coerceIn(0, (23 * 60) + 45)

private fun eventTopOffset(event: ItineraryEvent, startHour: Int) =
    (((event.timeOfDay?.let(::timeToMinutes) ?: startHour * 60) - startHour * 60) / 60f * TimelineHourHeight.value).dp

private fun condensedTimelineLabel(day: TimelineDaySection): String {
    if (day.timedEvents.isEmpty()) return "Sin horarios"
    val bounds = timelineBoundsForDay(day.timedEvents, condensed = true)
    return "%02d:00-%02d:00".format(bounds.first, bounds.last)
}

private fun resolveItineraryPlace(
    event: ItineraryEvent,
    activityCandidates: List<PollCandidate>,
): PlaceResult? {
    val candidate = event.placeId?.let { placeId ->
        activityCandidates.firstOrNull { it.placeId == placeId }
    }
    if (candidate != null) {
        return PlaceResult(
            placeId = candidate.placeId,
            name = candidate.name,
            photoUrl = candidate.photoUrl,
            rating = 0.0,
            reviewCount = 0,
            address = event.description.orEmpty(),
            lat = candidate.lat,
            lng = candidate.lng,
        )
    }
    return event.placeId?.let { placeId ->
        PlaceResult(
            placeId = placeId,
            name = event.name,
            photoUrl = "",
            rating = 0.0,
            reviewCount = 0,
            address = event.description.orEmpty(),
            lat = 0.0,
            lng = 0.0,
        )
    }
}

private fun activityDescriptorFor(event: ItineraryEvent): ActivityDescriptor {
    val text = buildString {
        append(event.name.lowercase(SpanishLocale))
        append(' ')
        append(event.description.orEmpty().lowercase(SpanishLocale))
    }
    return when {
        text.contains("hotel") || text.contains("hostel") || text.contains("caba") || text.contains("check-in") || text.contains("check out") || text.contains("check-out") || text.contains("aloj") ->
            ActivityDescriptor("Alojamiento", "Hospedaje, check-in, check-out o reserva de estadía.", Icons.Default.Hotel)
        text.contains("desayuno") || text.contains("almuerzo") || text.contains("cena") || text.contains("rest") || text.contains("café") || text.contains("cafe") || text.contains("comida") ->
            ActivityDescriptor("Gastronomía", "Comidas, cafés, restaurantes o paradas gastronómicas.", Icons.Default.Restaurant)
        text.contains("vuelo") || text.contains("aeropuerto") || text.contains("auto") || text.contains("traslado") || text.contains("terminal") || text.contains("ruta") ->
            ActivityDescriptor("Transporte", "Traslados, vuelos, autos o movimientos logísticos del viaje.", Icons.Default.DirectionsCar)
        text.contains("museo") || text.contains("arte") ->
            ActivityDescriptor("Cultura", "Museos, arte y actividades culturales.", Icons.Default.Museum)
        text.contains("parque") || text.contains("lago") || text.contains("trek") || text.contains("sender") || text.contains("bosque") ->
            ActivityDescriptor("Naturaleza", "Parques, paisajes, trekking y actividades al aire libre.", Icons.Default.Park)
        text.contains("tour") || text.contains("excurs") || text.contains("paseo") || text.contains("visita") ->
            ActivityDescriptor("Excursión", "Tours, paseos guiados y salidas turísticas.", Icons.Default.Explore)
        event.placeId != null ->
            ActivityDescriptor("Ubicación", "Evento vinculado a un lugar o punto de interés del viaje.", Icons.Default.LocationOn)
        else ->
            ActivityDescriptor("Actividad", "Actividad general del itinerario sin una categoría más específica.", Icons.Default.LocalActivity)
    }
}

@Composable
private fun memberAccentColor(memberId: String): Color {
    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.78f),
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.78f),
    )
    val index = memberId.hashCode().mod(palette.size).let { if (it < 0) it + palette.size else it }
    return palette[index]
}

private fun memberInitials(name: String): String =
    name.trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .take(2)
        .joinToString("") { part -> part.first().uppercase() }
        .ifBlank { "?" }

private fun eventTypeIcon(event: ItineraryEvent): ImageVector {
    return activityDescriptorFor(event).icon
}
