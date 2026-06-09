package com.hllous.plantravel.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.hllous.plantravel.presentation.auth.AuthState
import com.hllous.plantravel.presentation.auth.AuthViewModel
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.profile.ProfileViewModel
import com.hllous.plantravel.ui.components.travelTextFieldColors
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.displayInitials

@Composable
fun ProfileScreen(
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onBack: () -> Unit = {},
    groupViewModel: GroupViewModel? = null,
    profileViewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by authViewModel.state.collectAsState()
    val userEmail by authViewModel.userEmail.collectAsState()
    val authenticatedDisplayName = (state as? AuthState.Authenticated)?.displayName ?: ""
    val currentGroup by groupViewModel?.currentGroup?.collectAsState() ?: remember { mutableStateOf(null) }
    val members by groupViewModel?.members?.collectAsState() ?: remember { mutableStateOf(emptyList()) }
    val editorState by profileViewModel.editorState.collectAsState()
    val profileMessage by profileViewModel.message.collectAsState()
    val profileUpdated by profileViewModel.profileUpdated.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val displayName = editorState.displayName.ifBlank { authenticatedDisplayName }
    val initials = displayInitials(displayName)
    var nameInput by rememberSaveable(editorState.displayName) { mutableStateOf(editorState.displayName) }
    var phoneInput by rememberSaveable(editorState.phone) { mutableStateOf(editorState.phone) }
    var aliasInput by rememberSaveable(editorState.mpAlias) { mutableStateOf(editorState.mpAlias) }

    LaunchedEffect(profileMessage) {
        val msg = profileMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        profileViewModel.clearMessage()
    }

    LaunchedEffect(profileUpdated) {
        if (profileUpdated) {
            authViewModel.refreshDisplayName()
            profileViewModel.clearProfileUpdated()
        }
    }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        // Blue brand panel
        Box(
            Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primary)
                .statusBarsPadding()
                .padding(bottom = 40.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = displayName.ifBlank { "Usuario" },
                    style = MaterialTheme.typography.headlineMedium.copy(fontFamily = FrauncesFamily),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = "✈ Miembro activo",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // Rounded card content
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp)
                    .navigationBarsPadding()
            ) {
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        ProfileInfoRow(
                            icon = { Icon(Icons.Default.Person, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = "Nombre",
                            value = displayName.ifBlank { "—" }
                        )
                        Spacer(Modifier.height(14.dp))
                        ProfileInfoRow(
                            icon = { Icon(Icons.Default.Email, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = "Email",
                            value = userEmail ?: "—"
                        )
                        Spacer(Modifier.height(14.dp))
                        ProfileInfoRow(
                            icon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(20.dp)) },
                            label = "Teléfono",
                            value = editorState.phone.ifBlank { "Sin teléfono" }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Group mini-card with 4dp left primaryContainer accent
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
                                .background(MaterialTheme.colorScheme.primaryContainer)
                        )
                        Column(Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.People,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Mi grupo",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            if (currentGroup != null) {
                                Text(
                                    currentGroup!!.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "${members.size} integrante${if (members.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                Text(
                                    "Sin grupo activo",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Unified profile edit form
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Editar perfil",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = { nameInput = it },
                            label = { Text("Nombre") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = travelTextFieldColors(),
                            enabled = !editorState.isLoading,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            label = { Text("Teléfono") },
                            placeholder = { Text("+54 9 11 0000-0000") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = travelTextFieldColors(),
                            enabled = !editorState.isLoading,
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = aliasInput,
                            onValueChange = { aliasInput = it },
                            label = { Text("Alias de MercadoPago") },
                            placeholder = { Text("Ej: nico.llousas") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            colors = travelTextFieldColors(),
                            enabled = !editorState.isLoading,
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                profileViewModel.saveProfile(
                                    displayName = nameInput,
                                    phone = phoneInput,
                                    mpAlias = aliasInput,
                                )
                            },
                            enabled = !editorState.isLoading && (
                                nameInput.trim() != editorState.displayName ||
                                    phoneInput.trim() != editorState.phone ||
                                    aliasInput.trim() != editorState.mpAlias
                                ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (editorState.isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Text("Guardar cambios")
                            }
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))

                OutlinedButton(
                    onClick = onLogout,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Text("Cerrar sesión")
                }
            }
        }
    }
    SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.align(Alignment.BottomCenter),
    )
    } // end Box
}

@Composable
private fun ProfileInfoRow(
    icon: @Composable () -> Unit,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(12.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
