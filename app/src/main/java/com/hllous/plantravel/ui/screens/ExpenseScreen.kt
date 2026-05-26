package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.ui.components.ErrorCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.formatCurrency
import com.hllous.plantravel.ui.utils.isItemFullyAssigned
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseScreen(viewModel: ExpenseViewModel, navController: NavHostController) {
    val expenseItemsUiState by viewModel.expenseItemsUiState.collectAsState()
    val members by viewModel.members.collectAsState(initial = emptyList())
    val items by viewModel.expenseItems.collectAsState(initial = emptyList())
    val assignments by viewModel.assignments.collectAsState(initial = emptyList())
    val settlements by viewModel.settlements.collectAsState(initial = emptyList())
    val settlementWarnings by viewModel.settlementWarnings.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val currentMember by viewModel.currentMember.collectAsState(initial = null)
    val currentMemberId: String? = currentMember?.id

    var addItemExpanded by rememberSaveable { mutableStateOf(false) }
    var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
    var itemName by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(selectedGroupId, items, assignments) {
        if (selectedGroupId != null) viewModel.refreshSettlement()
    }

    val density = LocalDensity.current
    var bottomPanelHeightPx by remember { mutableIntStateOf(0) }
    val bottomPanelHeightDp = with(density) { bottomPanelHeightPx.toDp() }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            contentWindowInsets = WindowInsets(0),
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
                )
            },
            floatingActionButton = {
                if (selectedGroupId != null) {
                    ExtendedFloatingActionButton(
                        onClick = {
                            addItemExpanded = true
                            settlementsExpanded = false
                        },
                        icon = { Icon(Icons.Default.Add, contentDescription = null) },
                        text = { Text("Agregar gasto") },
                    )
                }
            },
        ) { innerPadding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(bottom = bottomPanelHeightDp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (expenseItemsUiState is UiState.Error) {
                    item {
                        ErrorCard(
                            message = (expenseItemsUiState as UiState.Error).message,
                            onRetry = { viewModel.reloadExpenseItems() },
                        )
                    }
                }

                if (addItemExpanded) {
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

                if (selectedGroupId != null) {
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
                                onDelete = { viewModel.deleteExpenseItem(item.id) },
                                onAssignQuantity = { next ->
                                    currentMemberId?.let { memberId ->
                                        viewModel.assignItem(item.id, memberId, next.toString())
                                    }
                                },
                            )
                        }
                    }

                    if (settlementWarnings.isNotEmpty()) {
                        item {
                            Text(
                                "Advertencias",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        items(settlementWarnings) { warning -> SettlementWarningCard(warning) }
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
                amountCents = settlements
                    .firstOrNull { it.memberId == currentMemberId }?.amountCents ?: 0L,
                settlements = settlements,
                settlementsExpanded = settlementsExpanded,
                onToggleSettlements = { settlementsExpanded = !settlementsExpanded },
            )
        }
    }
}

private fun calculatePendingCents(warnings: List<SettlementWarning>): Long =
    warnings.sumOf { it.unassignedAmountCents }

@Composable
private fun ExpenseItemCard(
    item: ExpenseItem,
    members: List<GroupMember>,
    itemAssignments: List<ItemAssignment>,
    currentMemberId: String?,
    onDelete: () -> Unit,
    onAssignQuantity: (Int) -> Unit,
) {
    val assignedTotal = itemAssignments.sumOf { it.quantity }
    val remaining = (item.quantity - assignedTotal).coerceAtLeast(0)
    val myAssigned = itemAssignments.firstOrNull { it.memberId == currentMemberId }?.quantity ?: 0
    val unitPrice = if (item.quantity > 0) item.totalPriceCents / item.quantity else item.totalPriceCents
    val fullyAssigned = isItemFullyAssigned(assignedTotal, item.quantity)
    val accentColor = if (fullyAssigned) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.errorContainer

    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(accentColor),
            )
            Column(
                modifier = Modifier
                    .padding(14.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top,
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text(
                            "${formatCurrency(unitPrice)} · ${formatCurrency(item.totalPriceCents)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                    }
                }

                if (itemAssignments.any { it.quantity > 0 }) {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(itemAssignments.filter { it.quantity > 0 }) { assignment ->
                            val member = members.firstOrNull { it.id == assignment.memberId }
                            if (member != null) {
                                val accent = memberColor(member.id)
                                Surface(
                                    color = accent.copy(alpha = 0.14f),
                                    shape = MaterialTheme.shapes.large,
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(memberInitial(member.name), color = accent, fontWeight = FontWeight.Bold)
                                        Text("${member.name} · x${assignment.quantity}", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }
                }

                if (currentMemberId != null) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = MaterialTheme.shapes.large,
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Mis unidades: $myAssigned",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { onAssignQuantity((myAssigned - 1).coerceAtLeast(0)) }) {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Quitar")
                                }
                                IconButton(
                                    enabled = remaining > 0,
                                    onClick = { onAssignQuantity(myAssigned + 1) },
                                ) {
                                    Icon(Icons.Default.ArrowDropUp, contentDescription = "Agregar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettlementWarningCard(warning: SettlementWarning) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.errorContainer)
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    warning.itemName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "Quedan ${warning.unassignedQuantity} uds sin asignar por ${formatCurrency(warning.unassignedAmountCents)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
private fun SettlementCard(settlement: MemberSettlement) {
    val accent = memberColor(settlement.memberId)
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Surface(
                    color = accent.copy(alpha = 0.22f),
                    shape = MaterialTheme.shapes.extraLarge,
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(memberInitial(settlement.memberName), color = accent, fontWeight = FontWeight.Bold)
                    }
                }
                Column {
                    Text(settlement.memberName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                    Text("Total asignado", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                formatCurrency(settlement.amountCents),
                style = MaterialTheme.typography.titleLarge,
                color = accent,
                fontWeight = FontWeight.SemiBold,
            )
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
                Text("Nuevo gasto", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
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
    settlementsExpanded: Boolean,
    onToggleSettlements: () -> Unit,
) {
    val accent = memberColor(member?.id ?: "")
    Surface(
        shadowElevation = 8.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .imePadding(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (member == null) {
                    Text(
                        "Cargando tu perfil...",
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
                                    color = accent,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        Column {
                            Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatCurrency(amountCents),
                                style = MaterialTheme.typography.titleMedium,
                                color = accent,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }
                }
                TextButton(onClick = onToggleSettlements) {
                    Text(if (settlementsExpanded) "Ocultar" else "Ver resumen")
                }
            }

            if (settlementsExpanded && settlements.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    settlements.forEach { settlement -> SettlementCard(settlement) }
                }
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
