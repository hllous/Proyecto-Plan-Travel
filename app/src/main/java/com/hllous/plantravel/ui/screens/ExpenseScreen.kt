package com.hllous.plantravel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Hotel
import androidx.compose.material.icons.filled.LocalActivity
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Switch
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
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
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import androidx.navigation.NavHostController
import com.hllous.plantravel.domain.model.ExpenseGroup
import com.hllous.plantravel.domain.model.ExpenseGroupState
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.PeerToPerDebt
import com.hllous.plantravel.domain.model.PeerToPerDebtUiModel
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.ui.components.ErrorCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.formatCurrency
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val expenseGroups by viewModel.expenseGroups.collectAsState()
    val selectedExpenseGroupId by viewModel.selectedExpenseGroupId.collectAsState()
    val expenseItemsUiState by viewModel.expenseItemsUiState.collectAsState()
    val members by viewModel.members.collectAsState(initial = emptyList())
    val travelGroups by viewModel.groups.collectAsState(initial = emptyList())
    val items by viewModel.expenseItems.collectAsState(initial = emptyList())
    val assignments by viewModel.assignments.collectAsState(initial = emptyList())
    val settlements by viewModel.settlements.collectAsState(initial = emptyList())
    val settlementWarnings by viewModel.settlementWarnings.collectAsState(initial = emptyList())
    val peerToPerDebts by viewModel.peerToPerDebts.collectAsState(initial = emptyList())
    val peerToPerDebtsWithLinks by viewModel.peerToPerDebtsWithLinks.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val currentMember by viewModel.currentMember.collectAsState(initial = null)
    val dashboardState by viewModel.dashboardState.collectAsState()
    val currentMemberId: String? = currentMember?.id
    val selectedTravelGroup = travelGroups.firstOrNull { it.id == selectedGroupId }

    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState()

    LaunchedEffect(message) {
        val msg = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.clearMessage()
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (selectedExpenseGroupId == null) {
        // ── Expense Group List ─────────────────────────────────────────────
        ExpenseGroupListScreen(
            groups = expenseGroups,
            members = members,
            travelGroupName = selectedTravelGroup?.name ?: "Tu grupo",
            dashboardState = dashboardState,
            snackbarHostState = snackbarHostState,
            scrollBehavior = scrollBehavior,
            onCreateGroup = { name, category, onSuccess ->
                viewModel.createExpenseGroup(name, category, onSuccess)
            },
            onRenameGroup = { id, name -> viewModel.renameExpenseGroup(id, name) },
            onDeleteGroup = { id -> viewModel.deleteExpenseGroup(id) },
            onSetGroupPinned = { id, pinned -> viewModel.setExpenseGroupPinned(id, pinned) },
            onSelectGroup = { id -> viewModel.selectExpenseGroup(id) },
        )
    } else {
        // ── Expense Item Drill-in ──────────────────────────────────────────
        val selectedGroup = expenseGroups.firstOrNull { it.id == selectedExpenseGroupId }
        val isFinalized = selectedGroup?.state == ExpenseGroupState.Finalized
        val payerMember = members.firstOrNull { it.id == selectedGroup?.paidByMemberId }
        val isPayerView = currentMemberId != null && currentMemberId == selectedGroup?.paidByMemberId
        val myDebtAmountCents = if (isPayerView)
            peerToPerDebts.sumOf { it.amountCents }
        else
            settlements.firstOrNull { it.memberId == currentMemberId }?.amountCents ?: 0L
        val totalCents = items.sumOf { it.totalPriceCents }
        val pendingCents = calculatePendingCents(settlementWarnings)
        val density = LocalDensity.current
        var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
        val bottomPanelHeightDp = with(density) { bottomPanelHeightPx.toDp() }

        var addItemExpanded by rememberSaveable { mutableStateOf(false) }
        var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
        var showFinalizeDialog by rememberSaveable { mutableStateOf(false) }
        var divideEqually by rememberSaveable { mutableStateOf(false) }
        var itemName by rememberSaveable { mutableStateOf("") }
        var price by rememberSaveable(stateSaver = TextFieldValue.Saver) { mutableStateOf(TextFieldValue("")) }
        var quantity by rememberSaveable { mutableStateOf("") }

        LaunchedEffect(selectedExpenseGroupId, items, assignments) {
            if (selectedExpenseGroupId != null) viewModel.refreshSettlement()
        }

        val selectedGroupName = expenseGroups.firstOrNull { it.id == selectedExpenseGroupId }?.name ?: "Grupo"

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
                contentWindowInsets = WindowInsets(0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    LargeTopAppBar(
                        title = {
                            Text(
                                selectedGroupName,
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { viewModel.selectExpenseGroup(null) }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                            }
                        },
                        actions = {
                            if (!isFinalized && currentMember?.role == MemberRole.ADMIN) {
                                TextButton(onClick = { showFinalizeDialog = true }) {
                                    Text("Finalizar", color = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        },
                        scrollBehavior = scrollBehavior,
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            scrolledContainerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .imePadding(),
                ) {
                    ExpenseHeroCard(
                        group = selectedGroup,
                        totalCents = totalCents,
                        pendingCents = pendingCents,
                    )

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = bottomPanelHeightDp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        if (expenseItemsUiState is UiState.Error) {
                            item {
                                ErrorCard(
                                    message = (expenseItemsUiState as UiState.Error).message,
                                    onRetry = { viewModel.reloadExpenseItems() },
                                )
                            }
                        }

                        item {
                            PayerSelectorCard(
                                paidByMemberId = selectedGroup?.paidByMemberId,
                                payerMember = payerMember,
                                members = members,
                                isAdmin = currentMember?.role == MemberRole.ADMIN,
                                isFinalized = isFinalized,
                                onPayerSelected = { memberId -> viewModel.setPayer(memberId) },
                            )
                        }

                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "ITEMS",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.1.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (!isFinalized) {
                                    TextButton(
                                        onClick = {
                                            addItemExpanded = true
                                            settlementsExpanded = false
                                        },
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(15.dp),
                                        )
                                        Spacer(Modifier.width(3.dp))
                                        Text(
                                            "Añadir item",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                    }
                                }
                            }
                        }

                        if (!isFinalized && items.isNotEmpty()) {
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 4.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                        Text(
                                            text = "Dividir todo por igual",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                        )
                                        Text(
                                            text = "Asigna cantidades iguales a cada miembro",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Switch(
                                        checked = divideEqually,
                                        onCheckedChange = { checked ->
                                            divideEqually = checked
                                            if (checked) viewModel.divideEqually()
                                            else viewModel.resetAllAssignments()
                                        },
                                    )
                                }
                            }
                        }

                        if (items.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "Agregá el primer gasto para empezar a repartir.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        } else {
                            items(items) { item ->
                                ExpenseItemCard(
                                    item = item,
                                    members = members,
                                    itemAssignments = assignments.filter { it.itemId == item.id },
                                    currentMemberId = currentMemberId,
                                    isGroupFinalized = isFinalized,
                                    onDelete = { viewModel.deleteExpenseItem(item.id) },
                                    onAssignQuantity = { next ->
                                        currentMemberId?.let { memberId ->
                                            viewModel.assignItem(item.id, memberId, next.toString())
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomPanelHeightPx = it.height },
            ) {
                ExpenseBottomPanel(
                    member = currentMember,
                    amountCents = myDebtAmountCents,
                    isPayerView = isPayerView,
                    hasPayer = selectedGroup?.paidByMemberId != null,
                    payerMember = payerMember,
                    settlements = settlements,
                    peerToPerDebts = peerToPerDebtsWithLinks,
                    currentMemberId = currentMemberId,
                    settlementsExpanded = settlementsExpanded,
                    onToggleSettlements = { settlementsExpanded = !settlementsExpanded },
                    onMarkDebtorConfirmed = { from, to -> viewModel.markDebtorConfirmed(from, to) },
                    onMarkCreditorConfirmed = { from, to -> viewModel.markCreditorConfirmed(from, to) },
                )
                }
            }

            if (addItemExpanded && !isFinalized) {
                AddExpenseItemSheet(
                    itemName = itemName,
                    price = price,
                    quantity = quantity,
                    onItemNameChange = { itemName = it },
                    onPriceChange = { price = formatPriceInput(it) },
                    onQuantityChange = { quantity = it },
                    onDismiss = { addItemExpanded = false },
                    onConfirm = {
                        if (itemName.isNotBlank() && price.text.isNotBlank() && quantity.isNotBlank()) {
                            viewModel.addExpenseItem(itemName, price.text, quantity)
                            itemName = ""
                            price = TextFieldValue("")
                            quantity = ""
                            addItemExpanded = false
                        }
                    },
                )
            }

            if (showFinalizeDialog) {
                AlertDialog(
                    onDismissRequest = { showFinalizeDialog = false },
                title = { Text("Finalizar grupo") },
                text = { Text("¿Finalizar este grupo? No podrás agregar ni modificar gastos.") },
                confirmButton = {
                    Button(onClick = {
                        viewModel.finalizeExpenseGroup()
                        showFinalizeDialog = false
                    }) { Text("Finalizar") }
                },
                dismissButton = {
                    TextButton(onClick = { showFinalizeDialog = false }) { Text("Cancelar") }
                },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseGroupListScreen(
    groups: List<ExpenseGroup>,
    members: List<GroupMember>,
    travelGroupName: String,
    dashboardState: ExpenseViewModel.ExpenseDashboardState,
    snackbarHostState: SnackbarHostState,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    onCreateGroup: (String, String?, () -> Unit) -> Unit,
    onRenameGroup: (String, String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSetGroupPinned: (String, Boolean) -> Unit,
    onSelectGroup: (String) -> Unit,
) {
    var showCreateScreen by rememberSaveable { mutableStateOf(false) }
    var showAllMovements by rememberSaveable { mutableStateOf(false) }
    var groupOptionsTarget by remember { mutableStateOf<ExpenseViewModel.ExpenseDashboardMovement?>(null) }
    var renameTarget by remember { mutableStateOf<ExpenseViewModel.ExpenseDashboardMovement?>(null) }
    var renameValue by rememberSaveable { mutableStateOf("") }

    if (showCreateScreen) {
        CreateExpenseGroupScreen(
            onDismiss = { showCreateScreen = false },
            onConfirm = { name, category ->
                onCreateGroup(name, category) {
                    showCreateScreen = false
                }
            },
        )
        return
    }

    val recentMovements = if (showAllMovements) {
        dashboardState.recentMovements
    } else {
        dashboardState.recentMovements.take(3)
    }
    val pinnedMovements = dashboardState.pinnedMovements
    val primaryGroup = (pinnedMovements + dashboardState.recentMovements).firstOrNull()?.group

    fun openRenameDialog(movement: ExpenseViewModel.ExpenseDashboardMovement) {
        renameTarget = movement
        renameValue = movement.group.name
        groupOptionsTarget = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0),
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            "Gastos",
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Medium,
                        )
                    },
                    scrollBehavior = scrollBehavior,
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        scrolledContainerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item {
                    ExpenseSharedHeader(
                        travelGroupName = travelGroupName,
                        members = members,
                    )
                }

                item {
                    ExpenseOverviewCard(
                        totalCents = dashboardState.totalCents,
                        pendingGroupsCount = dashboardState.pendingGroupsCount,
                    )
                }

                item {
                    ExpenseBalanceCard(memberNetCents = dashboardState.memberNetCents)
                }

                item {
                    ExpenseMainActionButton(
                        label = "Agregar gasto",
                        containerBrush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                            )
                        ),
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        onClick = { showCreateScreen = true },
                    )
                }

                if (pinnedMovements.isNotEmpty()) {
                    item {
                        Text(
                            text = "Fijados",
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    items(pinnedMovements) { movement ->
                        ExpenseRecentMovementCard(
                            movement = movement,
                            onClick = { onSelectGroup(movement.group.id) },
                            onLongPress = { groupOptionsTarget = movement },
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Movimientos recientes",
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        if (dashboardState.recentMovements.size > 3) {
                            TextButton(onClick = { showAllMovements = !showAllMovements }) {
                                Text(if (showAllMovements) "Ver menos" else "Ver todos")
                            }
                        }
                    }
                }

                if (recentMovements.isEmpty()) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLowest,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Column(
                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Text(
                                    text = "No hay movimientos todavía",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    text = "Creá tu primer gasto para empezar a dividir el viaje.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                } else {
                    items(recentMovements) { movement ->
                        ExpenseRecentMovementCard(
                            movement = movement,
                            onClick = { onSelectGroup(movement.group.id) },
                            onLongPress = { groupOptionsTarget = movement },
                        )
                    }
                }
            }
        }

    }

    groupOptionsTarget?.let { movement ->
        ExpenseGroupOptionsDialog(
            group = movement.group,
            onDismiss = { groupOptionsTarget = null },
            onRename = { openRenameDialog(movement) },
            onDelete = {
                onDeleteGroup(movement.group.id)
                groupOptionsTarget = null
            },
            onTogglePin = {
                onSetGroupPinned(movement.group.id, movement.group.pinnedAtMillis == null)
                groupOptionsTarget = null
            },
        )
    }

    if (renameTarget != null) {
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Renombrar grupo") },
            text = {
                OutlinedTextField(
                    value = renameValue,
                    onValueChange = { renameValue = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = travelTextFieldColors(),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = renameTarget ?: return@TextButton
                        onRenameGroup(target.group.id, renameValue)
                        renameTarget = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) {
                    Text("Cancelar")
                }
            },
        )
    }
}

@Composable
private fun ExpenseSharedHeader(
    travelGroupName: String,
    members: List<GroupMember>,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "Gastos compartidos",
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.primary,
        )
        Text(
            text = travelGroupName,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        ExpenseAvatarCluster(members = members)
    }
}

@Composable
private fun ExpenseAvatarCluster(members: List<GroupMember>) {
    val visibleMembers = members.take(3)
    val overflow = members.size - visibleMembers.size

    Row(
        horizontalArrangement = Arrangement.spacedBy((-10).dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        visibleMembers.forEach { member ->
            val accent = memberColor(member.id)
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .border(3.dp, MaterialTheme.colorScheme.background, CircleShape)
                    .background(accent.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = memberInitial(member.name),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = accent,
                )
            }
        }
        if (overflow > 0) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.06f),
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.24f),
                ),
                modifier = Modifier.size(42.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+$overflow",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseOverviewCard(
    totalCents: Long,
    pendingGroupsCount: Int,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        shadowElevation = 10.dp,
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.86f),
                        )
                    )
                )
                .padding(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "GASTO TOTAL DEL GRUPO",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                    )
                    Text(
                        text = formatDashboardCurrency(totalCents),
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(100.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    ) {
                        Text(
                            text = if (pendingGroupsCount == 1) {
                                "1 gasto pendiente"
                            } else {
                                "$pendingGroupsCount gastos pendientes"
                            },
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpenseBalanceCard(memberNetCents: Long) {
    val isPositive = memberNetCents < 0L
    val isNegative = memberNetCents > 0L
    val absoluteNet = abs(memberNetCents)
    val accent = when {
        isPositive -> Color(0xFF21B77A)
        isNegative -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    val title = when {
        isPositive -> "Te deben ${formatDashboardCurrency(absoluteNet)}"
        isNegative -> "Debes ${formatDashboardCurrency(absoluteNet)}"
        else -> "Estás al día"
    }
    val subtitle = when {
        isPositive -> "Estás en positivo"
        isNegative -> "Tenés pagos pendientes"
        else -> "Sin deuda pendiente"
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLowest,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "TU SALDO",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = title,
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.primary,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(accent, CircleShape),
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExpenseMainActionButton(
    label: String,
    containerBrush: Brush,
    contentColor: Color,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        shadowElevation = if (enabled) 4.dp else 0.dp,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(
            modifier = Modifier
                .background(containerBrush)
                .padding(vertical = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = if (label == "Agregar gasto") Icons.Default.Add else Icons.Default.Warning,
                    contentDescription = null,
                    tint = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor.copy(alpha = if (enabled) 1f else 0.5f),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun ExpenseRecentMovementCard(
    movement: ExpenseViewModel.ExpenseDashboardMovement,
    onClick: () -> Unit,
    onLongPress: () -> Unit,
) {
    val group = movement.group
    val category = group.category?.let { cat ->
        ExpenseGroupCategory.entries.firstOrNull { it.name.lowercase() == cat }
    } ?: ExpenseGroupCategory.OTROS
    val memberNet = movement.memberNetCents
    val balanceColor = when {
        memberNet < 0L -> Color(0xFF21B77A)
        memberNet > 0L -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val balanceText = when {
        memberNet < 0L -> "Te deben\n${formatDashboardCurrency(abs(memberNet))}"
        memberNet > 0L -> "Debes\n${formatDashboardCurrency(memberNet)}"
        else -> "Al día"
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongPress,
            ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = category.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (group.pinnedAtMillis != null) {
                    Text(
                        text = "Fijado arriba",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Text(
                    text = if (group.state == ExpenseGroupState.Open) {
                        "${category.label} • Pendiente"
                    } else {
                        "${category.label} • Finalizado"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = formatExpenseGroupDate(group.createdAtMillis),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = formatDashboardCurrency(group.totalPriceCents),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
                Text(
                    text = balanceText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = balanceColor,
                    textAlign = TextAlign.End,
                )
            }
        }
    }
}

@Composable
private fun ExpenseGroupOptionsDialog(
    group: ExpenseGroup,
    onDismiss: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onTogglePin: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(group.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onRename, modifier = Modifier.fillMaxWidth()) {
                    Text("Renombrar", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Start)
                }
                TextButton(onClick = onTogglePin, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (group.pinnedAtMillis == null) "Fijar" else "Quitar Fijado",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                    )
                }
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Eliminar",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Start,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
    )
}

private fun formatDashboardCurrency(cents: Long): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).apply {
        minimumFractionDigits = if (cents % 100L == 0L) 0 else 2
        maximumFractionDigits = if (cents % 100L == 0L) 0 else 2
    }
    return formatter.format(cents / 100.0)
}

private fun formatPriceInput(value: TextFieldValue): TextFieldValue {
    val digits = value.text.filter(Char::isDigit)
    if (digits.isEmpty()) return TextFieldValue("")
    val amount = digits.toLongOrNull() ?: return TextFieldValue("")
    val formatted = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-AR")).format(amount)
    val text = "$ $formatted"
    return TextFieldValue(text = text, selection = TextRange(text.length))
}

private fun formatExpenseGroupDate(createdAtMillis: Long?): String {
    if (createdAtMillis == null) return "Sin fecha"
    return SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-AR")).format(Date(createdAtMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseGroupCard(
    group: ExpenseGroup,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 14.dp, bottom = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = group.name,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    letterSpacing = (-0.01).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatCurrency(group.totalPriceCents),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ExpenseGroupStateChip(state = group.state)
                }
            }
            Spacer(Modifier.width(8.dp))
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Eliminar grupo",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                )
            }
        }
    }
}

@Composable
private fun ExpenseGroupStateChip(state: ExpenseGroupState) {
    val (label, containerColor, contentColor) = when (state) {
        ExpenseGroupState.Open -> Triple(
            "Abierto",
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
        )
        ExpenseGroupState.Finalized -> Triple(
            "Finalizado",
            MaterialTheme.colorScheme.surfaceVariant,
            MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    Surface(
        color = containerColor,
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = contentColor,
        )
    }
}

private enum class ExpenseGroupCategory(val label: String, val icon: ImageVector) {
    COMIDA("Comida", Icons.Default.Fastfood),
    TRANSPORTE("Transporte", Icons.Default.DirectionsCar),
    ALOJAMIENTO("Alojamiento", Icons.Default.Hotel),
    ENTRETENIMIENTO("Entretenimiento", Icons.Default.LocalActivity),
    OTROS("Otros", Icons.Default.MoreHoriz),
}

private val quickSuggestions = listOf("Combustible", "Peajes", "Supermercado")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CreateExpenseGroupScreen(
    onDismiss: () -> Unit,
    onConfirm: (name: String, category: String?) -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var selectedCategory by rememberSaveable { mutableStateOf<ExpenseGroupCategory?>(null) }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Text(
                        "Agregar Gasto",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "NOMBRE DEL GASTO",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Ej: Cena en El Hornito") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = travelTextFieldColors(),
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "SUGERENCIAS RÁPIDAS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(quickSuggestions) { suggestion ->
                        FilterChip(
                            selected = false,
                            onClick = { name = suggestion },
                            label = { Text(suggestion) },
                            shape = MaterialTheme.shapes.extraLarge,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "CATEGORÍA",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.1.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(ExpenseGroupCategory.entries) { category ->
                        val isSelected = selectedCategory == category
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                selectedCategory = if (isSelected) null else category
                            },
                            label = { Text(category.label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = category.icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                                )
                            },
                            shape = MaterialTheme.shapes.extraLarge,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                                selectedLeadingIconColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    if (name.isNotBlank()) onConfirm(name, selectedCategory?.name?.lowercase())
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = name.isNotBlank(),
            ) {
                Text("Crear gasto", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun calculatePendingCents(warnings: List<SettlementWarning>): Long =
    warnings.sumOf { it.unassignedAmountCents }

@Composable
private fun ExpenseHeroCard(
    group: ExpenseGroup?,
    totalCents: Long,
    pendingCents: Long,
) {
    val category = group?.category?.let { cat ->
        ExpenseGroupCategory.entries.firstOrNull { it.name.lowercase() == cat }
    }
    val formattedDate = remember(group?.createdAtMillis) {
        group?.createdAtMillis?.let { millis ->
            SimpleDateFormat("d 'de' MMMM yyyy", Locale.forLanguageTag("es")).format(Date(millis))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary),
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 36.dp, y = 36.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.07f),
                    shape = CircleShape,
                ),
        )
        Box(
            modifier = Modifier
                .size(90.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 10.dp, y = 10.dp)
                .background(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.06f),
                    shape = CircleShape,
                ),
        )

        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (category != null) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = category.icon,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = category.label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }

            Text(
                text = formatCurrency(totalCents),
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 34.sp,
                letterSpacing = (-0.5).sp,
                color = MaterialTheme.colorScheme.onPrimary,
            )

            if (formattedDate != null) {
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
                )
            }

            if (pendingCents > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onPrimary,
                        )
                        Text(
                            text = "${formatCurrency(pendingCents)} por liquidar",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExpenseItemCard(
    item: ExpenseItem,
    members: List<GroupMember>,
    itemAssignments: List<ItemAssignment>,
    currentMemberId: String?,
    isGroupFinalized: Boolean,
    onDelete: () -> Unit,
    onAssignQuantity: (Int) -> Unit,
) {
    val serverMyAssigned = itemAssignments.firstOrNull { it.memberId == currentMemberId }?.quantity ?: 0
    var localMyAssigned by remember(serverMyAssigned) { mutableIntStateOf(serverMyAssigned) }

    val assignedByOthers = itemAssignments.filter { it.memberId != currentMemberId }.sumOf { it.quantity }
    val localRemaining = (item.quantity - assignedByOthers - localMyAssigned).coerceAtLeast(0)
    val unitPrice = if (item.quantity > 0) item.totalPriceCents / item.quantity else item.totalPriceCents
    val assignedWithQuantity = itemAssignments.filter { it.quantity > 0 }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primary),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Text(
                            text = item.name,
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = (-0.01).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "${formatCurrency(unitPrice)} c/u · ${formatCurrency(item.totalPriceCents)} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        if (currentMemberId != null && !isGroupFinalized) {
                            Surface(
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(50),
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                ) {
                                    StepperButton(
                                        label = "−",
                                        onClick = {
                                            val next = (localMyAssigned - 1).coerceAtLeast(0)
                                            localMyAssigned = next
                                            onAssignQuantity(next)
                                        },
                                    )
                                    Text(
                                        text = "$localMyAssigned",
                                        modifier = Modifier.padding(horizontal = 6.dp),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                    StepperButton(
                                        label = "+",
                                        enabled = localRemaining > 0,
                                        onClick = {
                                            val next = localMyAssigned + 1
                                            localMyAssigned = next
                                            onAssignQuantity(next)
                                        },
                                    )
                                }
                            }
                        }
                        if (!isGroupFinalized) {
                            IconButton(
                                onClick = onDelete,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                )
                            }
                        }
                    }
                }

                if (assignedWithQuantity.isNotEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "ASIGNADO A:",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.08.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        StackedAvatars(
                            assignments = assignedWithQuantity,
                            members = members,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StackedAvatars(
    assignments: List<ItemAssignment>,
    members: List<GroupMember>,
) {
    val avatarSize = 24.dp
    val stepSize = 16.dp
    val validAssignments = assignments.filter { a -> members.any { it.id == a.memberId } }
    val maxVisible = 5
    val visible = validAssignments.take(maxVisible)
    val extra = validAssignments.size - maxVisible
    if (visible.isEmpty()) return

    val boxWidth = stepSize * (visible.size - 1).coerceAtLeast(0) + avatarSize

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .height(avatarSize)
                .width(boxWidth),
        ) {
            visible.forEachIndexed { index, assignment ->
                val member = members.first { it.id == assignment.memberId }
                val accent = memberColor(member.id)
                Box(
                    modifier = Modifier
                        .offset(x = stepSize * index)
                        .size(avatarSize)
                        .border(1.5.dp, MaterialTheme.colorScheme.surface, CircleShape)
                        .background(accent.copy(alpha = 0.22f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = memberInitial(member.name),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
        }
        if (extra > 0) {
            Text(
                text = "+$extra",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepperButton(
    label: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        shadowElevation = if (enabled) 2.dp else 0.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.size(28.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled)
                    MaterialTheme.colorScheme.onSurface
                else
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
            )
        }
    }
}

@Composable
private fun SettlementCard(settlement: MemberSettlement) {
    val accent = memberColor(settlement.memberId)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                color = accent.copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.extraLarge,
                modifier = Modifier.size(36.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = memberInitial(settlement.memberName),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent,
                    )
                }
            }
            Text(
                text = settlement.memberName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = formatCurrency(settlement.amountCents),
            fontFamily = FrauncesFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 17.sp,
            color = accent,
        )
    }
}

@Composable
private fun PeerToPerDebtRow(
    uiModel: PeerToPerDebtUiModel,
    currentMemberId: String?,
    onMarkDebtorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
    onMarkCreditorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
) {
    val debt = uiModel.debt
    val isDebtor = currentMemberId == debt.fromMemberId
    val isCreditor = currentMemberId == debt.toMemberId

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = "${debt.fromMemberName} → ${debt.toMemberName}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = formatCurrency(debt.amountCents),
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (isDebtor) {
                if (uiModel.deepLink != null) {
                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uiModel.deepLink))
                            runCatching { context.startActivity(intent) }
                        },
                        modifier = Modifier.padding(start = 8.dp),
                    ) {
                        Text("Pagar con MP", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Text(
                        text = "Sin alias de MP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isDebtor && !uiModel.debtorConfirmed) {
                OutlinedButton(
                    onClick = { onMarkDebtorConfirmed(debt.fromMemberId, debt.toMemberId) },
                ) {
                    Text("Marcar como pagado", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (isCreditor && uiModel.debtorConfirmed && !uiModel.creditorConfirmed) {
                Button(
                    onClick = { onMarkCreditorConfirmed(debt.fromMemberId, debt.toMemberId) },
                ) {
                    Text("Confirmar pago", style = MaterialTheme.typography.labelSmall)
                }
            }
            if (uiModel.debtorConfirmed) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.extraLarge,
                ) {
                    Text(
                        text = if (uiModel.creditorConfirmed) "Pagado ✓" else "Enviado",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

@Composable
private fun AddExpenseItemPanel(
    itemName: String,
    price: TextFieldValue,
    quantity: String,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (TextFieldValue) -> Unit,
    onQuantityChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Nuevo gasto",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
            OutlinedTextField(
                value = itemName,
                onValueChange = onItemNameChange,
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = travelTextFieldColors(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = price,
                    onValueChange = onPriceChange,
                    label = { Text("Precio unitario") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = travelTextFieldColors(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = travelTextFieldColors(),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Agregar") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddExpenseItemSheet(
    itemName: String,
    price: TextFieldValue,
    quantity: String,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (TextFieldValue) -> Unit,
    onQuantityChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            AddExpenseItemPanel(
                itemName = itemName,
                price = price,
                quantity = quantity,
                onItemNameChange = onItemNameChange,
                onPriceChange = onPriceChange,
                onQuantityChange = onQuantityChange,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayerSelectorCard(
    paidByMemberId: String?,
    payerMember: GroupMember?,
    members: List<GroupMember>,
    isAdmin: Boolean,
    isFinalized: Boolean,
    onPayerSelected: (memberId: String?) -> Unit,
) {
    var dropdownExpanded by remember { mutableStateOf(false) }
    val canChange = isAdmin && !isFinalized
    val hasPayer = paidByMemberId != null
    val payerAccent = if (hasPayer) memberColor(paidByMemberId!!) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = if (hasPayer)
            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        else
            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
    ) {
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(
                        if (canChange) Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                        else Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Surface(
                    color = payerAccent.copy(alpha = 0.18f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (hasPayer && payerMember != null) {
                            Text(
                                text = memberInitial(payerMember.name),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = payerAccent,
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                            )
                        }
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "¿QUIÉN PAGÓ?",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.08.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = payerMember?.name ?: "Sin pagador seleccionado",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasPayer) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                }

                if (canChange) {
                    IconButton(onClick = { dropdownExpanded = true }) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Seleccionar pagador",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            DropdownMenu(
                expanded = dropdownExpanded,
                onDismissRequest = { dropdownExpanded = false },
            ) {
                if (paidByMemberId != null) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Sin pagador",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        onClick = {
                            onPayerSelected(null)
                            dropdownExpanded = false
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                    )
                }
                members.forEach { member ->
                    val isSelected = member.id == paidByMemberId
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = member.name,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                            )
                        },
                        onClick = {
                            onPayerSelected(member.id)
                            dropdownExpanded = false
                        },
                        leadingIcon = {
                            val accent = memberColor(member.id)
                            Surface(
                                color = accent.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.size(28.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = memberInitial(member.name),
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpenseBottomPanel(
    member: GroupMember?,
    amountCents: Long,
    isPayerView: Boolean,
    hasPayer: Boolean,
    payerMember: GroupMember?,
    settlements: List<MemberSettlement>,
    peerToPerDebts: List<PeerToPerDebtUiModel>,
    currentMemberId: String?,
    settlementsExpanded: Boolean,
    onToggleSettlements: () -> Unit,
    onMarkDebtorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
    onMarkCreditorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
) {
    val accent = memberColor(member?.id ?: "")
    val amountLabel = when {
        !hasPayer -> "Consumido"
        isPayerView -> "Te deben"
        else -> "Debés"
    }
    val amountColor = when {
        !hasPayer -> MaterialTheme.colorScheme.primary
        isPayerView -> Color(0xFF21B77A)
        else -> MaterialTheme.colorScheme.error
    }

    Surface(
        shadowElevation = 4.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Column {
            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp,
            )
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (member == null) {
                        Text(
                            text = "Cargando tu perfil...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Surface(
                                color = accent.copy(alpha = 0.18f),
                                shape = MaterialTheme.shapes.extraLarge,
                                modifier = Modifier.size(38.dp),
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = memberInitial(member.name),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = accent,
                                    )
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = amountLabel,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(
                                        text = formatCurrency(amountCents),
                                        fontFamily = FrauncesFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = amountColor,
                                    )
                                }
                            }
                        }
                    }
                    TextButton(onClick = onToggleSettlements) {
                        Text(
                            text = if (settlementsExpanded) "Ocultar ›" else "Ver resumen ›",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = settlementsExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Column {
                        Spacer(Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(8.dp))

                        if (!hasPayer) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Seleccioná quién pagó para ver el resumen",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        } else {
                            val sectionLabel = when {
                                isPayerView -> "Lo que te deben"
                                else -> "Lo que debés"
                            }
                            Text(
                                text = sectionLabel.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.08.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp),
                            )
                            Spacer(Modifier.height(4.dp))

                            if (peerToPerDebts.isEmpty()) {
                                Text(
                                    text = if (isPayerView) "Nadie te debe nada aún" else "No tenés deudas registradas",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp),
                                )
                            } else {
                                peerToPerDebts.forEach { uiModel ->
                                    PeerToPerDebtRow(
                                        uiModel = uiModel,
                                        currentMemberId = currentMemberId,
                                        onMarkDebtorConfirmed = onMarkDebtorConfirmed,
                                        onMarkCreditorConfirmed = onMarkCreditorConfirmed,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
