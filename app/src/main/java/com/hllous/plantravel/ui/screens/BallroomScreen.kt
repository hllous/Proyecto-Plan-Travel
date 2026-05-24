package com.hllous.plantravel.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.ui.components.CollapsibleHeader
import com.hllous.plantravel.ui.components.SectionCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.utils.formatCurrency
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@Suppress("UNUSED_PARAMETER")
@Composable
fun BallroomScreen(viewModel: MainViewModel, navController: NavHostController) {
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val members by viewModel.members.collectAsState(initial = emptyList())
    val items by viewModel.expenseItems.collectAsState(initial = emptyList())
    val assignments by viewModel.assignments.collectAsState(initial = emptyList())
    val settlements by viewModel.settlements.collectAsState(initial = emptyList())
    val settlementWarnings by viewModel.settlementWarnings.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val currentMember by viewModel.currentMember.collectAsState(initial = null)
    val currentMemberId = currentMember?.id
    var itemName by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var groupsExpanded by rememberSaveable { mutableStateOf(true) }
    var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
    var addItemExpanded by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(selectedGroupId, items, assignments) {
        if (selectedGroupId != null) {
            viewModel.refreshSettlement()
        }
    }

    val density = LocalDensity.current
    var bottomPanelHeightPx by remember { mutableStateOf(0) }
    val bottomPanelHeightDp = with(density) { bottomPanelHeightPx.toDp() }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val totalExpenseCents = items.sumOf { it.totalPriceCents }
    val pendingCents = calculatePendingCents(settlementWarnings)
    val mySettlementCents = settlements.firstOrNull { it.memberId == currentMemberId }?.amountCents ?: 0L

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .padding(bottom = bottomPanelHeightDp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ExpenseOverviewCard(
                    selectedGroup = selectedGroup,
                    itemsCount = items.size,
                    totalExpenseCents = totalExpenseCents,
                    pendingCents = pendingCents
                )
            }
            item {
                CollapsibleHeader(
                    title = "Grupo activo",
                    expanded = groupsExpanded,
                    onToggle = { groupsExpanded = !groupsExpanded }
                )
            }
            if (groupsExpanded) {
                if (groups.isEmpty()) {
                    item {
                        SectionCard(title = "Sin grupos") {
                            Text("Crea un grupo para comenzar a cargar y repartir gastos.")
                        }
                    }
                } else {
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(groups) { group ->
                                val isSelected = selectedGroupId == group.id
                                Surface(
                                    onClick = { viewModel.selectGroup(group.id) },
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
                                    tonalElevation = if (isSelected) 2.dp else 0.dp,
                                    shape = MaterialTheme.shapes.large
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                        Column {
                                            Text(group.name, fontWeight = FontWeight.SemiBold)
                                            Text(
                                                if (isSelected) "Grupo activo" else "Tocar para usar",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            if (selectedGroupId != null) {
                item {
                    SectionCard(
                        title = "Tu perfil en el grupo",
                        subtitle = "Selecciona el integrante para ver y editar su reparto en tiempo real."
                    ) {
                        if (members.isEmpty()) {
                            Text("No hay integrantes cargados todavia.")
                        } else {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                items(members) { member ->
                                    ProfileChip(
                                        member = member,
                                        selected = currentMemberId == member.id,
                                        onClick = {}
                                    )
                                }
                            }
                        }
                    }
                }
                if (items.isEmpty()) {
                    item {
                        SectionCard(title = "Sin items cargados") {
                            Text("Agrega el primer gasto para empezar a repartir consumos.")
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
                            }
                        )
                    }
                }
                if (settlementWarnings.isNotEmpty()) {
                    item {
                        Text(
                            "Advertencias de division",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    items(settlementWarnings) { warning ->
                        SettlementWarningCard(warning)
                    }
                }
            }
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .onSizeChanged { bottomPanelHeightPx = it.height }
        ) {
            ExpenseBottomPanel(
                member = currentMember,
                amountCents = mySettlementCents,
                pendingCents = pendingCents,
                settlements = settlements,
                settlementsExpanded = settlementsExpanded,
                addItemExpanded = addItemExpanded,
                itemName = itemName,
                price = price,
                quantity = quantity,
                onToggleSettlements = {
                    settlementsExpanded = !settlementsExpanded
                    if (settlementsExpanded) addItemExpanded = false
                },
                onToggleAddItem = {
                    addItemExpanded = !addItemExpanded
                    if (addItemExpanded) settlementsExpanded = false
                },
                onItemNameChange = { itemName = it },
                onPriceChange = { price = it },
                onQuantityChange = { quantity = it },
                onDismissAddItem = { addItemExpanded = false },
                onConfirmAddItem = {
                    if (itemName.isNotBlank() && price.isNotBlank() && quantity.isNotBlank()) {
                        viewModel.addExpenseItem(itemName, price, quantity)
                        itemName = ""
                        price = ""
                        quantity = ""
                        addItemExpanded = false
                    }
                }
            )
        }
    }
}

private fun calculatePendingCents(warnings: List<SettlementWarning>): Long =
    warnings.sumOf { it.unassignedAmountCents }

@Composable
private fun ExpenseOverviewCard(
    selectedGroup: TravelGroup?,
    itemsCount: Int,
    totalExpenseCents: Long,
    pendingCents: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.tertiary
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = selectedGroup?.name ?: "Gestion de gastos",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Visualiza el total del viaje y mantiene el reparto actualizado mientras asignas consumos.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.88f)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    item { SummaryPill(title = "Items", value = itemsCount.toString(), onColor = MaterialTheme.colorScheme.onPrimary) }
                    item { SummaryPill(title = "Total cargado", value = formatCurrency(totalExpenseCents), onColor = MaterialTheme.colorScheme.onPrimary) }
                    item { SummaryPill(title = "Pendiente", value = formatCurrency(pendingCents), onColor = MaterialTheme.colorScheme.onPrimary) }
                }
            }
        }
    }
}

@Composable
private fun SummaryPill(title: String, value: String, onColor: Color) {
    Surface(
        color = onColor.copy(alpha = 0.14f),
        modifier = Modifier.clip(MaterialTheme.shapes.large)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, color = onColor.copy(alpha = 0.82f), style = MaterialTheme.typography.labelMedium)
            Text(value, color = onColor, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ProfileChip(member: GroupMember, selected: Boolean, onClick: () -> Unit) {
    val accent = memberColor(member.id)
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.large,
        color = if (selected) accent.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface,
        tonalElevation = if (selected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(color = accent.copy(alpha = 0.22f), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = memberInitial(member.name),
                        color = accent,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column {
                Text(member.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    if (selected) "Perfil activo" else "Tocar para usar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ExpenseItemCard(
    item: ExpenseItem,
    members: List<GroupMember>,
    itemAssignments: List<ItemAssignment>,
    currentMemberId: Long?,
    onDelete: () -> Unit,
    onAssignQuantity: (Int) -> Unit
) {
    val assignedTotal = itemAssignments.sumOf { it.quantity }
    val remaining = (item.quantity - assignedTotal).coerceAtLeast(0)
    val myAssigned = itemAssignments.firstOrNull { it.memberId == currentMemberId }?.quantity ?: 0
    val unitPrice = if (item.quantity > 0) item.totalPriceCents / item.quantity else item.totalPriceCents
    val progress = if (item.quantity > 0) assignedTotal.toFloat() / item.quantity.toFloat() else 0f
    val statusColor = if (remaining == 0) Color(0xFF0F9D58) else Color(0xFFF39C12)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "${formatCurrency(unitPrice)} · ${formatCurrency(item.totalPriceCents)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(MaterialTheme.shapes.large)
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(statusColor)
                    )
                }
                repeat(item.quantity.coerceAtMost(6)) { index ->
                    Icon(
                        imageVector = Icons.Default.Circle,
                        contentDescription = null,
                        tint = if (index < assignedTotal.coerceAtMost(6)) statusColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                        modifier = Modifier.size(9.dp)
                    )
                }
            }

            if (itemAssignments.any { it.quantity > 0 }) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(itemAssignments.filter { it.quantity > 0 }) { assignment ->
                        val member = members.firstOrNull { it.id == assignment.memberId }
                        if (member != null) {
                            val accent = memberColor(member.id)
                            Surface(color = accent.copy(alpha = 0.14f), shape = MaterialTheme.shapes.large) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = MaterialTheme.shapes.large
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = myAssigned.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { onAssignQuantity((myAssigned - 1).coerceAtLeast(0)) }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Quitar")
                            }
                            IconButton(enabled = remaining > 0, onClick = { onAssignQuantity(myAssigned + 1) }) {
                                Icon(Icons.Default.ArrowDropUp, contentDescription = "Agregar")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                warning.itemName,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onErrorContainer,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                "Quedan ${warning.unassignedQuantity} uds sin asignar por ${formatCurrency(warning.unassignedAmountCents)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun SettlementCard(settlement: MemberSettlement) {
    val accent = memberColor(settlement.memberId)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accent.copy(alpha = 0.12f))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(color = accent.copy(alpha = 0.22f), modifier = Modifier.size(40.dp)) {
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
                fontWeight = FontWeight.SemiBold
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
    onConfirm: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Nuevo item", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text("Carga un gasto sin salir de la vista principal.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar formulario")
            }
        }
        OutlinedTextField(value = itemName, onValueChange = onItemNameChange, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth(), colors = travelTextFieldColors())
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(value = price, onValueChange = onPriceChange, label = { Text("Precio unitario") }, singleLine = true, modifier = Modifier.weight(1f), colors = travelTextFieldColors())
            OutlinedTextField(value = quantity, onValueChange = onQuantityChange, label = { Text("Cantidad") }, singleLine = true, modifier = Modifier.weight(1f), colors = travelTextFieldColors())
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text("Cancelar") }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) { Text("Agregar") }
        }
    }
}

@Composable
private fun ExpenseBottomPanel(
    member: GroupMember?,
    amountCents: Long,
    pendingCents: Long,
    settlements: List<MemberSettlement>,
    settlementsExpanded: Boolean,
    addItemExpanded: Boolean,
    itemName: String,
    price: String,
    quantity: String,
    onToggleSettlements: () -> Unit,
    onToggleAddItem: () -> Unit,
    onItemNameChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onDismissAddItem: () -> Unit,
    onConfirmAddItem: () -> Unit
) {
    val accent = memberColor(member?.id ?: 0)
    Surface(
        tonalElevation = 10.dp,
        shadowElevation = 12.dp,
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth().imePadding()
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .background(brush = Brush.verticalGradient(listOf(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))))
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                if (member == null) {
                    Text("Selecciona tu perfil para ver tu monto final.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(color = accent.copy(alpha = 0.18f), modifier = Modifier.size(38.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = memberInitial(member.name), color = accent, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            }
                        }
                        Column {
                            Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(formatCurrency(amountCents), style = MaterialTheme.typography.headlineSmall, color = accent, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    IconButton(onClick = onToggleSettlements) {
                        Icon(
                            imageVector = if (settlementsExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                            contentDescription = if (settlementsExpanded) "Ocultar montos" else "Mostrar montos"
                        )
                    }
                    FloatingActionButton(onClick = onToggleAddItem, containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar item")
                    }
                }
            }

            if (addItemExpanded) {
                Spacer(Modifier.height(14.dp))
                AddExpenseItemPanel(
                    itemName = itemName, price = price, quantity = quantity,
                    onItemNameChange = onItemNameChange, onPriceChange = onPriceChange, onQuantityChange = onQuantityChange,
                    onDismiss = onDismissAddItem, onConfirm = onConfirmAddItem
                )
            }

            if (settlementsExpanded) {
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    settlements.forEach { settlement -> SettlementCard(settlement = settlement) }
                }
            }
        }
    }
}
