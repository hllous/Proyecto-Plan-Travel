package com.hllous.plantravel.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val items by viewModel.expenseItems.collectAsState(initial = emptyList())
    val assignments by viewModel.assignments.collectAsState(initial = emptyList())
    val settlements by viewModel.settlements.collectAsState(initial = emptyList())
    val settlementWarnings by viewModel.settlementWarnings.collectAsState(initial = emptyList())
    val peerToPerDebts by viewModel.peerToPerDebts.collectAsState(initial = emptyList())
    val peerToPerDebtsWithLinks by viewModel.peerToPerDebtsWithLinks.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val currentMember by viewModel.currentMember.collectAsState(initial = null)
    val currentMemberId: String? = currentMember?.id

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
            snackbarHostState = snackbarHostState,
            scrollBehavior = scrollBehavior,
            onCreateGroup = { name -> viewModel.createExpenseGroup(name) },
            onDeleteGroup = { id -> viewModel.deleteExpenseGroup(id) },
            onSelectGroup = { id -> viewModel.selectExpenseGroup(id) },
        )
    } else {
        // ── Expense Item Drill-in ──────────────────────────────────────────
        val selectedGroup = expenseGroups.firstOrNull { it.id == selectedExpenseGroupId }
        val isFinalized = selectedGroup?.state == ExpenseGroupState.Finalized
        val totalCents = items.sumOf { it.totalPriceCents }
        val pendingCents = calculatePendingCents(settlementWarnings)
        val density = LocalDensity.current
        var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
        val bottomPanelHeightDp = with(density) { bottomPanelHeightPx.toDp() }

        var addItemExpanded by rememberSaveable { mutableStateOf(false) }
        var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
        var showFinalizeDialog by rememberSaveable { mutableStateOf(false) }
        var itemName by rememberSaveable { mutableStateOf("") }
        var price by rememberSaveable { mutableStateOf("") }
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
                                    Text("Finalizar")
                                }
                            }
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
                    if (selectedGroupId != null) {
                        ExpenseSummaryStrip(
                            totalCents = totalCents,
                            pendingCents = pendingCents,
                        )
                    }

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

                        if (addItemExpanded && !isFinalized) {
                            item {
                                AddExpenseItemPanel(
                                    itemName = itemName,
                                    price = price,
                                    quantity = quantity,
                                    onItemNameChange = { itemName = it },
                                    onPriceChange = { price = it },
                                    onQuantityChange = { quantity = it },
                                    onDismiss = { addItemExpanded = false },
                                    onConfirm = {
                                        if (itemName.isNotBlank() && price.isNotBlank() && quantity.isNotBlank()) {
                                            viewModel.addExpenseItem(itemName, price, quantity)
                                            itemName = ""; price = ""; quantity = ""
                                            addItemExpanded = false
                                        }
                                    },
                                )
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

            if (!isFinalized) {
                ExtendedFloatingActionButton(
                    onClick = {
                        addItemExpanded = true
                        settlementsExpanded = false
                    },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text("Agregar gasto") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(bottom = bottomPanelHeightDp + 16.dp, end = 16.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .onSizeChanged { bottomPanelHeightPx = it.height },
            ) {
                ExpenseBottomPanel(
                    member = currentMember,
                    amountCents = settlements
                        .firstOrNull { it.memberId == currentMemberId }?.amountCents ?: 0L,
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
    snackbarHostState: SnackbarHostState,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    onCreateGroup: (String) -> Unit,
    onDeleteGroup: (String) -> Unit,
    onSelectGroup: (String) -> Unit,
) {
    var createPanelExpanded by rememberSaveable { mutableStateOf(false) }
    var groupName by rememberSaveable { mutableStateOf("") }
    var groupToDelete by remember { mutableStateOf<ExpenseGroup?>(null) }

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
                        containerColor = MaterialTheme.colorScheme.background,
                        scrolledContainerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (createPanelExpanded) {
                    item {
                        AddExpenseGroupPanel(
                            groupName = groupName,
                            onGroupNameChange = { groupName = it },
                            onDismiss = { createPanelExpanded = false; groupName = "" },
                            onConfirm = {
                                if (groupName.isNotBlank()) {
                                    onCreateGroup(groupName)
                                    groupName = ""
                                    createPanelExpanded = false
                                }
                            },
                        )
                    }
                }

                if (groups.isEmpty() && !createPanelExpanded) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "Creá un grupo de gastos para empezar a repartir.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                } else {
                    items(groups) { group ->
                        ExpenseGroupCard(
                            group = group,
                            onClick = { onSelectGroup(group.id) },
                            onDelete = { groupToDelete = group },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { createPanelExpanded = true },
            icon = { Icon(Icons.Default.Add, contentDescription = null) },
            text = { Text("Nuevo grupo") },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 16.dp, end = 16.dp),
        )
    }

    groupToDelete?.let { group ->
        AlertDialog(
            onDismissRequest = { groupToDelete = null },
            title = { Text("Eliminar grupo") },
            text = { Text("¿Eliminar \"${group.name}\"? Esta acción no se puede deshacer.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteGroup(group.id)
                        groupToDelete = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { groupToDelete = null }) { Text("Cancelar") }
            },
        )
    }
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

@Composable
private fun AddExpenseGroupPanel(
    groupName: String,
    onGroupNameChange: (String) -> Unit,
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
                    text = "Nuevo grupo de gastos",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }
            OutlinedTextField(
                value = groupName,
                onValueChange = onGroupNameChange,
                label = { Text("Nombre del grupo") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = travelTextFieldColors(),
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Crear") }
            }
        }
    }
}

private fun calculatePendingCents(warnings: List<SettlementWarning>): Long =
    warnings.sumOf { it.unassignedAmountCents }

@Composable
private fun ExpenseSummaryStrip(
    totalCents: Long,
    pendingCents: Long,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = "Total del grupo",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.08.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = formatCurrency(totalCents),
                fontFamily = FrauncesFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        if (pendingCents > 0) {
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = MaterialTheme.shapes.extraLarge,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = "${formatCurrency(pendingCents)} por liquidar",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
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
    // Optimistic local state: updates immediately on click, resets when server confirms
    var localMyAssigned by remember(serverMyAssigned) { mutableIntStateOf(serverMyAssigned) }

    val assignedByOthers = itemAssignments
        .filter { it.memberId != currentMemberId }
        .sumOf { it.quantity }
    val assignedTotal = assignedByOthers + localMyAssigned
    val localRemaining = (item.quantity - assignedTotal).coerceAtLeast(0)
    val unitPrice = if (item.quantity > 0) item.totalPriceCents / item.quantity else item.totalPriceCents

    // Progress bar data
    val serverAssignedTotal = itemAssignments.sumOf { it.quantity }
    val assignmentFraction = if (item.quantity > 0) serverAssignedTotal.toFloat() / item.quantity else 0f
    val isFullyAssigned = serverAssignedTotal >= item.quantity
    val progressColor = when {
        isFullyAssigned -> MaterialTheme.colorScheme.primary
        serverAssignedTotal > 0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }
    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = assignmentFraction.coerceIn(0f, 1f),
        label = "assignment_progress",
    )

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 12.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
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
                        text = "${formatCurrency(unitPrice)} c/u · x${item.quantity} unidades",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(
                        text = formatCurrency(item.totalPriceCents),
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.01).sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (!isGroupFinalized) {
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Eliminar",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            )
                        }
                    }
                }
            }

            if (itemAssignments.any { it.quantity > 0 }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(itemAssignments.filter { it.quantity > 0 }) { assignment ->
                        val member = members.firstOrNull { it.id == assignment.memberId }
                        if (member != null) {
                            val accent = memberColor(member.id)
                            Surface(
                                color = accent.copy(alpha = 0.14f),
                                shape = CircleShape,
                            ) {
                                Text(
                                    text = "${memberInitial(member.name)} · x${assignment.quantity}",
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                )
                            }
                        }
                    }
                }
            }

            if (currentMemberId != null && !isGroupFinalized) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                    shape = RoundedCornerShape(10.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "Mis unidades: $localMyAssigned",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
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
                                modifier = Modifier.padding(horizontal = 8.dp),
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
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "$serverAssignedTotal / ${item.quantity} asignadas",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.04.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isFullyAssigned) {
                        Text(
                            text = "Completo",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(CircleShape),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
            }
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
    price: String,
    quantity: String,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
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
                    colors = travelTextFieldColors(),
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = onQuantityChange,
                    label = { Text("Cantidad") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
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

@Composable
private fun ExpenseBottomPanel(
    member: GroupMember?,
    amountCents: Long,
    settlements: List<MemberSettlement>,
    peerToPerDebts: List<PeerToPerDebtUiModel>,
    currentMemberId: String?,
    settlementsExpanded: Boolean,
    onToggleSettlements: () -> Unit,
    onMarkDebtorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
    onMarkCreditorConfirmed: (fromMemberId: String, toMemberId: String) -> Unit,
) {
    val accent = memberColor(member?.id ?: "")
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
                            Text(
                                text = formatCurrency(amountCents),
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
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
                visible = settlementsExpanded && (settlements.isNotEmpty() || peerToPerDebts.isNotEmpty()),
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut(),
            ) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    settlements.forEach { settlement -> SettlementCard(settlement) }
                    if (peerToPerDebts.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Deudas entre miembros",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.08.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 4.dp),
                        )
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
