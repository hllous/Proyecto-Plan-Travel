package com.hllous.plantravel.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.ui.components.ErrorCard
import com.hllous.plantravel.ui.components.SectionCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel,
    navController: NavHostController
) {
    val groupsUiState by groupViewModel.groupsUiState.collectAsState()
    val groups by groupViewModel.groups.collectAsState(initial = emptyList())
    var joinCode by rememberSaveable { mutableStateOf("") }
    var showCreateDialog by rememberSaveable { mutableStateOf(false) }
    var newGroupName by rememberSaveable { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = {
                showCreateDialog = false
                newGroupName = ""
            },
            title = { Text("Nuevo grupo") },
            text = {
                OutlinedTextField(
                    value = newGroupName,
                    onValueChange = { newGroupName = it },
                    label = { Text("Nombre del grupo") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = travelTextFieldColors()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        groupViewModel.createGroup(newGroupName)
                        newGroupName = ""
                        showCreateDialog = false
                    },
                    enabled = newGroupName.isNotBlank()
                ) { Text("Crear") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    newGroupName = ""
                }) { Text("Cancelar") }
            }
        )
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        contentWindowInsets = WindowInsets(0),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Grupos",
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium
                    )
                },
                scrollBehavior = scrollBehavior,
                windowInsets = WindowInsets(0)
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showCreateDialog = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Crear grupo") }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            if (groupsUiState is UiState.Error) {
                item {
                    ErrorCard(
                        message = (groupsUiState as UiState.Error).message,
                        onRetry = { groupViewModel.reloadGroups() }
                    )
                }
            }

            items(groups, key = { it.id }) { group ->
                GroupListItem(
                    group = group,
                    onClick = { navController.navigate("group_detail/${group.id}") }
                )
            }

            item {
                SectionCard(title = "Unirse a un grupo") {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase() },
                        label = { Text("Código de invitación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Button(
                        onClick = {
                            mainViewModel.consumeInvite(joinCode)
                            joinCode = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = joinCode.isNotBlank()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ingresar")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel,
    navController: NavHostController
) {
    val groups by groupViewModel.groups.collectAsState()
    val members by groupViewModel.members.collectAsState()
    val currentUserRole by groupViewModel.currentUserRole.collectAsState()
    val invites by mainViewModel.invites.collectAsState(initial = emptyList())
    val pendingKickMemberId by groupViewModel.pendingKickMemberId.collectAsState()
    val selectedGroupId by groupViewModel.selectedGroupId.collectAsState()

    val selectedGroup = groups.firstOrNull { it.id == groupId }

    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }
    var editableGroupName by rememberSaveable { mutableStateOf("") }
    var showQr by rememberSaveable { mutableStateOf(false) }
    var groupSelected by remember { mutableStateOf(false) }

    LaunchedEffect(groupId) {
        groupViewModel.selectGroup(groupId)
        groupSelected = true
    }

    LaunchedEffect(selectedGroup?.name) {
        val name = selectedGroup?.name ?: return@LaunchedEffect
        editableGroupName = name
    }

    // Navigate back when group is deleted or user leaves
    LaunchedEffect(selectedGroupId, groupSelected) {
        if (groupSelected && selectedGroupId == null) {
            navController.navigateUp()
        }
    }

    DisposableEffect(Unit) {
        onDispose { groupViewModel.clearSelectedGroup() }
    }

    if (pendingKickMemberId != null) {
        AlertDialog(
            onDismissRequest = { groupViewModel.cancelKick() },
            title = { Text("Eliminar integrante") },
            text = { Text("¿Estás seguro de que querés eliminar a este integrante?") },
            confirmButton = {
                Button(
                    onClick = { groupViewModel.confirmKick() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = {
                TextButton(onClick = { groupViewModel.cancelKick() }) { Text("Cancelar") }
            }
        )
    }

    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Abandonar grupo") },
            text = { Text("¿Estás seguro de que querés abandonar este grupo?") },
            confirmButton = {
                Button(
                    onClick = {
                        showLeaveConfirm = false
                        groupViewModel.leaveGroup()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Abandonar") }
            },
            dismissButton = {
                TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancelar") }
            }
        )
    }

    val context = LocalContext.current
    val latestInvite = invites.firstOrNull()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0),
                title = {
                    Column {
                        Text(
                            selectedGroup?.name ?: "Grupo",
                            fontFamily = FrauncesFamily,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            "${members.size} integrantes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    when (currentUserRole) {
                        MemberRole.ADMIN -> IconButton(onClick = {}) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar grupo")
                        }
                        MemberRole.USER -> IconButton(onClick = { showLeaveConfirm = true }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ExitToApp,
                                contentDescription = "Abandonar grupo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                        null -> Unit
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                SectionCard(title = "Integrantes") {
                    if (members.isEmpty()) {
                        Text(
                            "Sin integrantes",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        members.forEachIndexed { index, member ->
                            MemberRow(
                                member = member,
                                canKick = currentUserRole == MemberRole.ADMIN && member.role != MemberRole.ADMIN,
                                onKick = { groupViewModel.requestKickMember(member.id) }
                            )
                            if (index < members.size - 1) {
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Invitar") {
                    Button(
                        onClick = { mainViewModel.generateInvite() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generar código")
                    }
                    if (latestInvite != null) {
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Código: ${latestInvite.code}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Surface(
                                onClick = {
                                    val clipboard = context.getSystemService(ClipboardManager::class.java)
                                    clipboard?.setPrimaryClip(
                                        ClipData.newPlainText("invite_code", latestInvite.code)
                                    )
                                },
                                shape = RoundedCornerShape(50),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = "Copiar código",
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text("Copiar", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                        TextButton(onClick = { showQr = !showQr }) {
                            Text(if (showQr) "Ocultar QR" else "Mostrar QR")
                        }
                        if (showQr) {
                            val qrBitmap = rememberQrBitmap(latestInvite.link)
                            if (qrBitmap != null) {
                                Image(
                                    bitmap = qrBitmap.asImageBitmap(),
                                    contentDescription = "QR de invitación",
                                    modifier = Modifier.size(160.dp)
                                )
                            }
                        }
                    }
                }
            }

            if (currentUserRole == MemberRole.ADMIN) {
                item {
                    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .height(IntrinsicSize.Min)
                        ) {
                            Box(
                                Modifier
                                    .width(4.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.error)
                            )
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Zona de peligro",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.error,
                                    fontWeight = FontWeight.SemiBold
                                )
                                OutlinedTextField(
                                    value = editableGroupName,
                                    onValueChange = { editableGroupName = it },
                                    label = { Text("Nombre del grupo") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = travelTextFieldColors()
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = { groupViewModel.updateSelectedGroupName(editableGroupName) },
                                        modifier = Modifier.weight(1f),
                                        enabled = editableGroupName.isNotBlank()
                                    ) { Text("Guardar") }
                                    OutlinedButton(
                                        onClick = { groupViewModel.deleteSelectedGroup() },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Eliminar")
                                    }
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
private fun GroupListItem(group: TravelGroup, onClick: () -> Unit) {
    ElevatedCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
        ) {
            Box(
                Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = FrauncesFamily),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(2.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "${group.memberCount} integrantes",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.Default.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MemberRow(
    member: GroupMember,
    canKick: Boolean,
    onKick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Surface(
                color = memberColor(member.id).copy(alpha = 0.18f),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    text = memberInitial(member.name),
                    color = memberColor(member.id),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    fontWeight = FontWeight.Bold
                )
            }
            Column {
                Text(
                    member.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (member.role == MemberRole.ADMIN)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (member.role == MemberRole.ADMIN) "Admin" else "Miembro",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (member.role == MemberRole.ADMIN)
                            MaterialTheme.colorScheme.onPrimaryContainer
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
            }
        }
        if (canKick) {
            IconButton(onClick = onKick) {
                Icon(
                    Icons.Default.PersonRemove,
                    contentDescription = "Eliminar integrante",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun rememberQrBitmap(content: String, size: Int = 512): Bitmap? {
    return remember(content, size) {
        runCatching {
            val matrix: BitMatrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
            val pixels = IntArray(size * size)
            for (y in 0 until size) {
                for (x in 0 until size) {
                    pixels[(y * size) + x] = if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                }
            }
            Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
                setPixels(pixels, 0, size, 0, 0, size, size)
            }
        }.getOrNull()
    }
}
