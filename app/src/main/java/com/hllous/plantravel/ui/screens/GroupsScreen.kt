package com.hllous.plantravel.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PersonRemove
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.domain.model.InviteToken
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.domain.model.TravelGroup
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.formatExpiry
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsScreen(
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel
) {
    val currentGroup by groupViewModel.currentGroup.collectAsState()

    if (currentGroup == null) {
        NoGroupContent(groupViewModel = groupViewModel, mainViewModel = mainViewModel)
    } else {
        GroupDetailContent(
            group = currentGroup!!,
            groupViewModel = groupViewModel,
            mainViewModel = mainViewModel
        )
    }
}

// ─── No-group state ────────────────────────────────────────────────────────────

@Composable
private fun NoGroupContent(
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel
) {
    var showCreateForm by rememberSaveable { mutableStateOf(false) }
    var showJoinForm by rememberSaveable { mutableStateOf(false) }
    var newGroupName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }

    Column(Modifier.fillMaxSize()) {
        // Primary header panel
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(bottom = 44.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.06f), 90.dp.toPx(), Offset(size.width + 40.dp.toPx(), -50.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.04f), 60.dp.toPx(), Offset(-20.dp.toPx(), size.height + 30.dp.toPx()))
            }
            Column(
                Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("✈️", style = MaterialTheme.typography.displaySmall)
                Text(
                    "Sin grupo aún",
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Creá o unite a uno para planificar",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                )
            }
        }

        // White content card slides up over header bottom padding
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // CTA: Crear grupo
                CtaCard(
                    label = "Crear grupo",
                    description = "Invitá personas con un código",
                    icon = {
                        Icon(
                            Icons.Default.Add, null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    isExpanded = showCreateForm,
                    onClick = {
                        showCreateForm = !showCreateForm
                        if (showCreateForm) showJoinForm = false
                    }
                )
                AnimatedVisibility(visible = showCreateForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = newGroupName,
                            onValueChange = { newGroupName = it },
                            label = { Text("Nombre del grupo") },
                            placeholder = { Text("Ej: Viaje a Bariloche") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = travelTextFieldColors()
                        )
                        Button(
                            onClick = {
                                groupViewModel.createGroup(newGroupName)
                                newGroupName = ""
                                showCreateForm = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = newGroupName.isNotBlank()
                        ) { Text("Crear grupo") }
                    }
                }

                // Divider
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                    Text(
                        "o",
                        Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                }

                // CTA: Unirme con código
                CtaCard(
                    label = "Unirme con código",
                    description = "Ingresá el código de invitación",
                    icon = {
                        Icon(
                            Icons.Default.Link, null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    },
                    isExpanded = showJoinForm,
                    onClick = {
                        showJoinForm = !showJoinForm
                        if (showJoinForm) showCreateForm = false
                    }
                )
                AnimatedVisibility(visible = showJoinForm) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
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
                                showJoinForm = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = joinCode.isNotBlank()
                        ) { Text("Unirme") }
                    }
                }
            }
        }
    }
}

// ─── Group detail state ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroupDetailContent(
    group: TravelGroup,
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel
) {
    val members by groupViewModel.members.collectAsState()
    val currentUserRole by groupViewModel.currentUserRole.collectAsState()
    val invites by mainViewModel.invites.collectAsState(initial = emptyList())
    val pendingKickMemberId by groupViewModel.pendingKickMemberId.collectAsState()
    val context = LocalContext.current

    var showAdminSheet by rememberSaveable { mutableStateOf(false) }
    var showLeaveConfirm by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirm by rememberSaveable { mutableStateOf(false) }
    var editableGroupName by rememberSaveable { mutableStateOf(group.name) }
    var showQr by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(group.name) { editableGroupName = group.name }

    // Kick confirmation
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
            dismissButton = { TextButton(onClick = { groupViewModel.cancelKick() }) { Text("Cancelar") } }
        )
    }

    // Leave confirmation
    if (showLeaveConfirm) {
        AlertDialog(
            onDismissRequest = { showLeaveConfirm = false },
            title = { Text("Abandonar grupo") },
            text = { Text("¿Estás seguro de que querés abandonar este grupo?") },
            confirmButton = {
                Button(
                    onClick = { showLeaveConfirm = false; groupViewModel.leaveGroup() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Abandonar") }
            },
            dismissButton = { TextButton(onClick = { showLeaveConfirm = false }) { Text("Cancelar") } }
        )
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar grupo") },
            text = { Text("Esta acción es irreversible. ¿Querés eliminar el grupo \"${group.name}\"?") },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; groupViewModel.deleteSelectedGroup() },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") } }
        )
    }

    // Admin bottom sheet
    if (showAdminSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAdminSheet = false },
            sheetState = rememberModalBottomSheetState()
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    "Configuración del grupo",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "NOMBRE DEL GRUPO",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                OutlinedTextField(
                    value = editableGroupName,
                    onValueChange = { editableGroupName = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = travelTextFieldColors()
                )
                Button(
                    onClick = {
                        groupViewModel.updateSelectedGroupName(editableGroupName)
                        showAdminSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = editableGroupName.isNotBlank()
                ) { Text("Guardar cambios") }
                OutlinedButton(
                    onClick = { showAdminSheet = false; showDeleteConfirm = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Eliminar grupo")
                }
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Primary header panel
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(bottom = 36.dp)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(Color.White.copy(alpha = 0.06f), 80.dp.toPx(), Offset(size.width + 30.dp.toPx(), -40.dp.toPx()))
                drawCircle(Color.White.copy(alpha = 0.04f), 50.dp.toPx(), Offset(-20.dp.toPx(), size.height + 20.dp.toPx()))
            }

            // Top row: gear (admin) or exit (member) at end
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp)
                    .height(44.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (currentUserRole) {
                    MemberRole.ADMIN -> IconButton(onClick = { showAdminSheet = true }) {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = "Configurar grupo",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    MemberRole.USER -> IconButton(onClick = { showLeaveConfirm = true }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Salir del grupo",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    null -> Unit
                }
            }

            // Avatar stack + group name + member count
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 44.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MemberAvatarStack(members = members)
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f)
                ) {
                    Text(
                        text = "👥 ${members.size} integrantes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Content card
        Surface(
            modifier = Modifier.fillMaxWidth().weight(1f),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 14.dp, vertical = 18.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Members section
                SectionEyebrow("Integrantes")
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        members.forEachIndexed { index, member ->
                            MemberDetailRow(
                                member = member,
                                canKick = currentUserRole == MemberRole.ADMIN && member.role != MemberRole.ADMIN,
                                onKick = { groupViewModel.requestKickMember(member.id) }
                            )
                            if (index < members.size - 1) {
                                HorizontalDivider(
                                    Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                        if (members.isEmpty()) {
                            Text(
                                "Sin integrantes",
                                Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Invite section
                SectionEyebrow("Invitar")
                InviteSection(
                    invites = invites,
                    onGenerate = { mainViewModel.generateInvite() },
                    context = context,
                    showQr = showQr,
                    onQrToggle = { showQr = !showQr }
                )
            }
        }
    }
}

// ─── Shared sub-composables ────────────────────────────────────────────────────

@Composable
private fun SectionEyebrow(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.2.sp
    )
}

@Composable
private fun CtaCard(
    label: String,
    description: String,
    icon: @Composable () -> Unit,
    isExpanded: Boolean,
    onClick: () -> Unit
) {
    val chevronRotation by animateFloatAsState(if (isExpanded) 90f else 0f, label = "chevron")

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = if (isExpanded) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null,
        elevation = CardDefaults.cardElevation(defaultElevation = if (isExpanded) 2.dp else 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) { icon() }
            }
            Column(Modifier.weight(1f)) {
                Text(
                    label,
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FrauncesFamily,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                modifier = Modifier.graphicsLayer { rotationZ = chevronRotation },
                tint = if (isExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun MemberAvatarStack(members: List<GroupMember>) {
    val displayed = members.take(5)
    val overflow = (members.size - 5).coerceAtLeast(0)

    Row(horizontalArrangement = Arrangement.spacedBy((-10).dp)) {
        displayed.forEach { member ->
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .clip(CircleShape)
                        .background(memberColor(member.id).copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        memberInitial(member.name),
                        color = memberColor(member.id),
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        if (overflow > 0) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(31.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.22f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "+$overflow",
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberDetailRow(
    member: GroupMember,
    canKick: Boolean,
    onKick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(11.dp))
                .background(memberColor(member.id).copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                memberInitial(member.name),
                color = memberColor(member.id),
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.labelMedium
            )
        }
        Text(
            member.name,
            Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = if (member.role == MemberRole.ADMIN)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                if (member.role == MemberRole.ADMIN) "Admin" else "Miembro",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (member.role == MemberRole.ADMIN)
                    MaterialTheme.colorScheme.onPrimaryContainer
                else
                    MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
            )
        }
        if (canKick) {
            IconButton(onClick = onKick, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Default.PersonRemove,
                    contentDescription = "Expulsar",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun InviteSection(
    invites: List<InviteToken>,
    onGenerate: () -> Unit,
    context: Context,
    showQr: Boolean,
    onQrToggle: () -> Unit
) {
    val latestInvite = invites.firstOrNull()

    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (latestInvite == null) {
                Button(onClick = onGenerate, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Add, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generar código")
                }
            } else {
                // Code pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = latestInvite.code,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 13.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        textAlign = TextAlign.Center,
                        letterSpacing = 3.sp
                    )
                }

                // Copy + Share chips
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            val cb = context.getSystemService(ClipboardManager::class.java)
                            cb?.setPrimaryClip(ClipData.newPlainText("invite_link", latestInvite.link))
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("Copiar", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                    Surface(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                putExtra(Intent.EXTRA_TEXT, latestInvite.link)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(intent, "Compartir invitación"))
                        },
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Share, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("Compartir", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Expiry
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Default.AccessTime, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                    Text(
                        formatExpiry(latestInvite.expiresAtMillis),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                // QR toggle
                OutlinedButton(onClick = onQrToggle, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.QrCode, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (showQr) "Ocultar QR" else "Ver QR")
                }

                AnimatedVisibility(visible = showQr) {
                    val qrBitmap = rememberQrBitmap(latestInvite.link)
                    if (qrBitmap != null) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Image(
                                bitmap = qrBitmap.asImageBitmap(),
                                contentDescription = "QR de invitación",
                                modifier = Modifier
                                    .size(160.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
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
