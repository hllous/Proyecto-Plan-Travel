package com.hllous.plantravel.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.domain.model.MemberRole
import com.hllous.plantravel.presentation.UiState
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.ui.components.ErrorCard
import com.hllous.plantravel.ui.components.SectionCard
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.utils.memberColor
import com.hllous.plantravel.ui.utils.memberInitial

@Composable
fun GroupsScreen(
    groupViewModel: GroupViewModel,
    mainViewModel: MainViewModel,
    navController: NavHostController
) {
    val groupsUiState by groupViewModel.groupsUiState.collectAsState()
    val groups by groupViewModel.groups.collectAsState(initial = emptyList())
    val selectedGroupId by groupViewModel.selectedGroupId.collectAsState(initial = null)
    val members by groupViewModel.members.collectAsState(initial = emptyList())
    val invites by mainViewModel.invites.collectAsState(initial = emptyList())
    var groupName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var editableGroupName by rememberSaveable { mutableStateOf("") }
    var showCreateGroup by rememberSaveable { mutableStateOf(false) }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val availableGroup = groups.firstOrNull()

    LaunchedEffect(selectedGroupId, selectedGroup?.name) {
        if (selectedGroup != null) {
            editableGroupName = selectedGroup.name
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (groupsUiState is UiState.Error) {
            item {
                ErrorCard(
                    message = (groupsUiState as UiState.Error).message,
                    onRetry = { groupViewModel.reloadGroups() }
                )
            }
        }
        if (selectedGroupId == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Grupos", style = MaterialTheme.typography.headlineSmall)
                            if (availableGroup != null) {
                                OutlinedButton(onClick = { groupViewModel.selectGroup(availableGroup.id) }) {
                                    Text("Grupos")
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                                }
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Surface(
                                onClick = { showCreateGroup = !showCreateGroup },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = null)
                                    Text(if (showCreateGroup) "Cerrar" else "Crear")
                                }
                            }
                            Surface(
                                onClick = { navController.navigate("qr_scanner") },
                                shape = MaterialTheme.shapes.large,
                                color = MaterialTheme.colorScheme.surface,
                                modifier = Modifier.weight(1f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null)
                                    Text("Escanear")
                                }
                            }
                        }
                    }
                }
            }
            if (showCreateGroup) {
                item {
                    SectionCard(title = "Nuevo grupo") {
                        OutlinedTextField(
                            value = groupName,
                            onValueChange = { groupName = it },
                            label = { Text("Grupo") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = travelTextFieldColors()
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedButton(
                                onClick = { showCreateGroup = false },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Cancelar")
                            }
                            Button(
                                onClick = {
                                    groupViewModel.createGroup(groupName)
                                    groupName = ""
                                    showCreateGroup = false
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Crear")
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Unirse") {
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase() },
                        label = { Text("Codigo de invitacion") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Button(
                        onClick = {
                            mainViewModel.consumeInvite(joinCode)
                            joinCode = ""
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Ingresar")
                    }
                }
            }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { groupViewModel.clearSelectedGroup() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(selectedGroup?.name ?: "Grupo", style = MaterialTheme.typography.headlineSmall)
                            Text("Activo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Grupo activo") {
                    OutlinedTextField(
                        value = editableGroupName,
                        onValueChange = { editableGroupName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { groupViewModel.updateSelectedGroupName(editableGroupName) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                        Button(
                            onClick = { groupViewModel.deleteSelectedGroup() },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Eliminar")
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Invitaciones") {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { mainViewModel.generateInvite() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Generar")
                        }
                        OutlinedButton(
                            onClick = { navController.navigate("qr_scanner") },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.QrCode, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("QR")
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Integrantes") {
                    if (members.isEmpty()) {
                        Text("Sin integrantes")
                    } else {
                        members.forEach { member ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(color = memberColor(member.id).copy(alpha = 0.18f), shape = MaterialTheme.shapes.large) {
                                        Text(
                                            text = memberInitial(member.name),
                                            color = memberColor(member.id),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Column {
                                        Text(member.name, fontWeight = FontWeight.Medium)
                                        Text(member.role.name, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (member.role != MemberRole.ADMIN) {
                                    IconButton(onClick = { groupViewModel.deleteMember(member.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar integrante", tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item {
                SectionCard(title = "Recientes") {
                    if (invites.isEmpty()) {
                        Text("Sin invitaciones")
                    } else {
                        invites.take(3).forEach { invite ->
                            InviteCard(
                                inviteCode = invite.code,
                                inviteLink = invite.link,
                                onDelete = { mainViewModel.deleteInvite(invite.code) }
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InviteCard(inviteCode: String, inviteLink: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    val payload = inviteLink
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Codigo: $inviteCode")
                Row {
                    IconButton(onClick = {
                        val clipboard = context.getSystemService(ClipboardManager::class.java)
                        clipboard?.setPrimaryClip(ClipData.newPlainText("invite_code", inviteCode))
                    }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copiar codigo")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar invitacion", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text("Link: $inviteLink", style = MaterialTheme.typography.bodySmall)
            val qrBitmap = rememberQrBitmap(payload)
            if (qrBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR",
                    modifier = Modifier
                        .width(140.dp)
                        .height(140.dp)
                )
            }
        }
    }
}

@Composable
private fun rememberQrBitmap(content: String, size: Int = 512): Bitmap? {
    return androidx.compose.runtime.remember(content, size) {
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
