package com.hllous.plantravel

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import com.hllous.plantravel.domain.model.ExpenseItem
import com.hllous.plantravel.domain.model.GroupMember
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.ui.theme.ProyectoPlanTravelTheme
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import com.journeyapps.barcodescanner.CompoundBarcodeView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            ProyectoPlanTravelTheme(darkTheme = isDarkTheme) {
                PlanTravelApp(isDarkTheme = isDarkTheme, onThemeChange = { isDarkTheme = it })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlanTravelApp(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val viewModelResult = runCatching { hiltViewModel<MainViewModel>() }
    if (viewModelResult.isFailure) {
        val message = viewModelResult.exceptionOrNull()?.message ?: "Error desconocido"
        MvpErrorScreen(message = message)
        return
    }
    val viewModel = viewModelResult.getOrThrow()

    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(message) {
        val text = message
        if (!text.isNullOrBlank()) {
            snackbarHostState.showSnackbar(text)
            viewModel.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onNavigate = { route ->
                    navController.navigate(route)
                    scope.launch { drawerState.close() }
                }
            )
        }
    ) {
        val currentBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                TopAppBar(
                    title = { Text("Plan Travel") },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { onThemeChange(!isDarkTheme) }) {
                            Icon(
                                if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                                contentDescription = "Toggle theme"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                        actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            },
            bottomBar = {
                if (currentRoute != "qr_scanner") {
                    BottomNavBar(currentRoute = currentRoute, navController = navController)
                }
            },
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                composable("home") {
                    HomeScreen(navController = navController)
                }
                composable("groups") {
                    GroupsScreen(viewModel = viewModel, navController = navController)
                }
                composable("destinations") {
                    DestinationScreen(viewModel = viewModel, navController = navController)
                }
                composable("gastos") {
                    BallroomScreen(viewModel = viewModel, navController = navController)
                }
                composable("qr_scanner") {
                    QrScannerScreen(
                        viewModel = viewModel,
                        onDone = { navController.navigate("groups") },
                        onBack = { navController.navigateUp() }
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawerContent(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Plan Travel",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    "Organiza tus viajes",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = false,
                        onClick = { onNavigate("home") }
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.People, contentDescription = "Grupos") },
                        label = { Text("Grupos") },
                        selected = false,
                        onClick = { onNavigate("groups") }
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.LocationOn, contentDescription = "Destinos") },
                        label = { Text("Destinos") },
                        selected = false,
                        onClick = { onNavigate("destinations") }
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Gastos") },
                        label = { Text("Gastos") },
                        selected = false,
                        onClick = { onNavigate("gastos") }
                    )
                }
                item {
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.QrCode, contentDescription = "Escanear") },
                        label = { Text("Escanear QR") },
                        selected = false,
                        onClick = { onNavigate("qr_scanner") }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NavigationDrawerItem(
                    icon = { Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme") },
                    label = { Text(if (isDarkTheme) "Modo claro" else "Modo oscuro") },
                    selected = false,
                    onClick = { onThemeChange(!isDarkTheme) }
                )
            }
        }
    }
}

@Composable
private fun BottomNavBar(currentRoute: String, navController: NavHostController) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
            label = { Text("Home") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Grupos") },
            label = { Text("Grupos") },
            selected = currentRoute == "groups",
            onClick = { navController.navigate("groups") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Destinos") },
            label = { Text("Destinos") },
            selected = currentRoute == "destinations",
            onClick = { navController.navigate("destinations") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Gastos") },
            label = { Text("Gastos") },
            selected = currentRoute == "gastos",
            onClick = { navController.navigate("gastos") }
        )
    }
}

@Composable
private fun MvpErrorScreen(message: String) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.errorContainer
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Error al abrir MVP", color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))
            Text(message, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Bienvenido a Plan Travel", style = MaterialTheme.typography.headlineSmall)
                    Text("Organiza y coordina tus viajes en grupo de forma simple y eficiente.")
                }
            }
        }

        item {
            Text("Funcionalidades principales", style = MaterialTheme.typography.titleMedium)
        }

        item {
            HomeCard(
                icon = Icons.Default.People,
                title = "Grupos de viaje",
                subtitle = "Crea grupos con roles de admin y usuarios",
                onClick = { navController.navigate("groups") }
            )
        }

        item {
            HomeCard(
                icon = Icons.Default.QrCode,
                title = "Invitaciones",
                subtitle = "Comparte QR, código o link para invitar",
                onClick = { navController.navigate("groups") }
            )
        }

        item {
            HomeCard(
                icon = Icons.Default.LocationOn,
                title = "Destinos",
                subtitle = "Explora recomendaciones por región",
                onClick = { navController.navigate("destinations") }
            )
        }

        item {
            HomeCard(
                icon = Icons.Default.AccountBalanceWallet,
                title = "Gastos",
                subtitle = "Reparte gastos por consumo real",
                onClick = { navController.navigate("gastos") }
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Composable
private fun HomeCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.width(40.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = onClick) {
                Text("Ir")
            }
        }
    }
}

@Composable
fun GroupsScreen(viewModel: MainViewModel, navController: NavHostController) {
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val members by viewModel.members.collectAsState(initial = emptyList())
    val invites by viewModel.invites.collectAsState(initial = emptyList())

    var groupName by rememberSaveable { mutableStateOf("") }
    var adminName by rememberSaveable { mutableStateOf("") }
    var joinName by rememberSaveable { mutableStateOf("") }
    var joinCode by rememberSaveable { mutableStateOf("") }
    var editableGroupName by rememberSaveable { mutableStateOf("") }
    var showCreateGroup by rememberSaveable { mutableStateOf(false) }

    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }

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
        if (!showCreateGroup) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Crear o acceder a un grupo", style = MaterialTheme.typography.headlineSmall)
                        Text("Inicia un nuevo grupo o escanea QR/código para unirte a uno existente")
                    }
                }
            }

            item {
                Button(
                    onClick = { showCreateGroup = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Crear nuevo grupo")
                }
            }

            item {
                Button(
                    onClick = { navController.navigate("qr_scanner") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unirse escaneando QR")
                }
            }

            if (groups.isNotEmpty()) {
                item {
                    SectionCard(title = "Mis grupos") {
                        groups.forEach { group ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(group.name, style = MaterialTheme.typography.titleMedium)
                                        Text("ID ${group.id}", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Button(onClick = { viewModel.selectGroup(group.id) }) {
                                        Text(if (group.id == selectedGroupId) "Activo" else "Entrar")
                                    }
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                        }
                    }
                }
            }
        } else {
            item {
                SectionCard(title = "Crear nuevo grupo", subtitle = "Inicia un grupo de viaje") {
                    OutlinedTextField(value = groupName, onValueChange = { groupName = it }, label = { Text("Nombre del grupo") }, modifier = Modifier.fillMaxWidth(), colors = travelTextFieldColors())
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = adminName, onValueChange = { adminName = it }, label = { Text("Tu nombre (Admin)") }, modifier = Modifier.fillMaxWidth(), colors = travelTextFieldColors())
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            viewModel.createGroup(groupName, adminName)
                            groupName = ""
                            adminName = ""
                            showCreateGroup = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crear grupo")
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { showCreateGroup = false }) {
                        Text("Cancelar")
                    }
                }
            }
        }

        if (selectedGroupId != null) {
            item {
                SectionCard(title = "Configuración del grupo") {
                    OutlinedTextField(
                        value = editableGroupName,
                        onValueChange = { editableGroupName = it },
                        label = { Text("Nombre del grupo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.updateSelectedGroupName(editableGroupName) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Guardar nombre")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { viewModel.deleteSelectedGroup() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Eliminar grupo")
                    }
                }
            }

            item {
                SectionCard(title = "Integrantes e invitaciones") {
                    Text("Ingreso al grupo solo por QR o código", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinName,
                        onValueChange = { joinName = it },
                        label = { Text("Nombre para unirse") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase() },
                        label = { Text("Código de invitación") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = {
                        viewModel.consumeInvite(joinCode, joinName)
                        joinCode = ""
                    }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Unirse por código")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { viewModel.generateInvite() }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generar invitación")
                    }
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { navController.navigate("qr_scanner") }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.QrCode, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Escanear QR")
                    }
                }
            }

            item {
                Text("Integrantes", style = MaterialTheme.typography.titleMedium)
            }
            if (members.isEmpty()) {
                item { Text("Sin integrantes aún") }
            }
            items(members) { member ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = memberColor(member.id).copy(alpha = 0.25f))
                            ) {
                                Text(
                                    text = memberInitial(member.name),
                                    color = memberColor(member.id),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                            Text("${member.name} - ${member.role.name}")
                        }
                        if (member.role.name != "ADMIN") {
                            IconButton(onClick = { viewModel.deleteMember(member.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar integrante", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            item {
                Text("Invitaciones recientes", style = MaterialTheme.typography.titleMedium)
            }
            if (invites.isEmpty()) {
                item { Text("Genera una invitación para compartir") }
            }
            items(invites.take(3)) { invite ->
                InviteCard(
                    inviteCode = invite.code,
                    inviteLink = invite.link,
                    onDelete = { viewModel.deleteInvite(invite.code) }
                )
            }
        }
    }
}

@Composable
private fun InviteCard(inviteCode: String, inviteLink: String, onDelete: () -> Unit) {
    val payload = "PLANTRAVEL|$inviteCode"
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Código: $inviteCode")
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Eliminar invitación", tint = MaterialTheme.colorScheme.error)
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
fun DestinationScreen(viewModel: MainViewModel, navController: NavHostController) {
    val regions by viewModel.regions.collectAsState(initial = emptyList())
    val selectedRegion by viewModel.selectedRegion.collectAsState(initial = null)
    val recommendations by viewModel.recommendations.collectAsState(initial = emptyList())

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionCard(title = "Regiones de Argentina") {
                if (regions.isEmpty()) {
                    Text("Cargando...")
                } else {
                    regions.forEach { region ->
                        FilterChip(
                            selected = region == selectedRegion,
                            onClick = { viewModel.selectRegion(region) },
                            label = { Text(region) },
                            colors = FilterChipDefaults.filterChipColors()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }
            }
        }

        if (recommendations.isEmpty() && selectedRegion != null) {
            item {
                SectionCard(title = "Sin destinos disponibles") {
                    Text("No hay destinos para esta región")
                }
            }
        } else if (recommendations.isNotEmpty()) {
            items(recommendations) { rec ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.width(20.dp))
                            Text(rec.destination, style = MaterialTheme.typography.titleMedium)
                        }
                        Text(rec.recommendation)
                    }
                }
            }
        }
    }
}

@Composable
fun BallroomScreen(viewModel: MainViewModel, navController: NavHostController) {
    val groups by viewModel.groups.collectAsState(initial = emptyList())
    val members by viewModel.members.collectAsState(initial = emptyList())
    val items by viewModel.expenseItems.collectAsState(initial = emptyList())
    val assignments by viewModel.assignments.collectAsState(initial = emptyList())
    val settlements by viewModel.settlements.collectAsState(initial = emptyList())
    val selectedGroupId by viewModel.selectedGroupId.collectAsState(initial = null)
    val currentMemberId by viewModel.currentMemberId.collectAsState(initial = null)

    var itemName by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }

    var groupsExpanded by rememberSaveable { mutableStateOf(true) }
    var itemsExpanded by rememberSaveable { mutableStateOf(true) }
    var assignExpanded by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(members) {
        if (members.isNotEmpty() && currentMemberId == null) {
            viewModel.setCurrentMember(members.first().id)
        }
    }

    LaunchedEffect(selectedGroupId, items, assignments) {
        if (selectedGroupId != null) {
            viewModel.refreshSettlement()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionCard(
                title = "Gastos",
                subtitle = "El sobrante va al administrador"
            ) { }
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
                item { Text("Crea un grupo primero") }
            }
            items(groups) { group ->
                Button(
                    onClick = { viewModel.selectGroup(group.id) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AccountBalanceWallet, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectedGroupId == group.id) "${group.name} (activo)" else group.name)
                }
            }
        }

        if (selectedGroupId != null) {
            item {
                SectionCard(title = "Tu perfil en el grupo") {
                    if (members.isEmpty()) {
                        Text("No hay integrantes cargados todavía.")
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            members.forEach { member ->
                                val chipSelected = currentMemberId == member.id
                                FilterChip(
                                    selected = chipSelected,
                                    onClick = { viewModel.setCurrentMember(member.id) },
                                    label = {
                                        Text("${memberInitial(member.name)} - ${member.name}")
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = memberColor(member.id).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }

            item {
                CollapsibleHeader(
                    title = "Carga de items",
                    expanded = itemsExpanded,
                    onToggle = { itemsExpanded = !itemsExpanded }
                )
            }

            if (itemsExpanded) {
                item {
                    SectionCard(title = "Nuevo item") {
                        OutlinedTextField(value = itemName, onValueChange = { itemName = it }, label = { Text("Nombre del item") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = travelTextFieldColors())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Precio unitario") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = travelTextFieldColors())
                        Spacer(Modifier.height(6.dp))
                        OutlinedTextField(value = quantity, onValueChange = { quantity = it }, label = { Text("Cantidad") }, modifier = Modifier.fillMaxWidth(), singleLine = true, colors = travelTextFieldColors())
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = {
                            viewModel.addExpenseItem(itemName, price, quantity)
                            itemName = ""
                            price = ""
                            quantity = ""
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Agregar item")
                        }
                    }
                }
            }

            item {
                CollapsibleHeader(
                    title = "Items",
                    expanded = assignExpanded,
                    onToggle = { assignExpanded = !assignExpanded }
                )
            }

            if (assignExpanded && items.isEmpty()) {
                item { Text("Sin items cargados") }
            } else if (assignExpanded) {
                item { Text("Usa + o - para asignar o quitar. 0 te desasigna.", style = MaterialTheme.typography.bodySmall) }
                items(items) { item ->
                    val itemAssignments = assignments.filter { it.itemId == item.id }
                    val assignedTotal = itemAssignments.sumOf { it.quantity }
                    val remaining = (item.quantity - assignedTotal).coerceAtLeast(0)
                    val myAssigned = itemAssignments.firstOrNull { it.memberId == currentMemberId }?.quantity ?: 0
                    Card(
                        modifier = Modifier
                            .fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    val unitPrice = if (item.quantity > 0) item.totalPriceCents / item.quantity else item.totalPriceCents
                                    Text("${item.name}", style = MaterialTheme.typography.titleSmall)
                                    Text("Unit: $${"%.2f".format(unitPrice / 100.0)}  •  Total: $${"%.2f".format(item.totalPriceCents / 100.0)}", style = MaterialTheme.typography.bodySmall)
                                    Text("Restante: ${remaining} uds", style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(onClick = { viewModel.deleteExpenseItem(item.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = MaterialTheme.colorScheme.error)
                                }
                            }

                            if (itemAssignments.isNotEmpty()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    itemAssignments.forEach { assignment ->
                                        val member = members.firstOrNull { it.id == assignment.memberId }
                                        if (member != null && assignment.quantity > 0) {
                                            Card(
                                                colors = CardDefaults.cardColors(containerColor = memberColor(member.id).copy(alpha = 0.25f))
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(memberInitial(member.name), color = memberColor(member.id), style = MaterialTheme.typography.labelMedium)
                                                    Text("x${assignment.quantity}", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            if (currentMemberId != null) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Tu cantidad", style = MaterialTheme.typography.bodySmall)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(onClick = {
                                            currentMemberId?.let { memberId ->
                                                val next = (myAssigned - 1).coerceAtLeast(0)
                                                viewModel.assignItem(item.id, memberId, next.toString())
                                            }
                                        }) {
                                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Quitar")
                                        }
                                        Text(
                                            text = myAssigned.toString(),
                                            style = MaterialTheme.typography.titleMedium,
                                            modifier = Modifier.width(28.dp),
                                            textAlign = TextAlign.Center
                                        )
                                        IconButton(onClick = {
                                            currentMemberId?.let { memberId ->
                                                val next = myAssigned + 1
                                                viewModel.assignItem(item.id, memberId, next.toString())
                                            }
                                        }) {
                                            Icon(Icons.Default.ArrowDropUp, contentDescription = "Agregar")
                                        }
                                    }
                                }
                                if (myAssigned > 0) {
                                    TextButton(onClick = {
                                        currentMemberId?.let { memberId ->
                                            viewModel.assignItem(item.id, memberId, "0")
                                        }
                                    }) {
                                        Text("Desasignarme de este item")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (settlements.isNotEmpty()) {
                item { Text("Montos Finales", style = MaterialTheme.typography.titleMedium) }
            }
            items(settlements) { settlement ->
                val amount = settlement.amountCents / 100.0
                val finalColor = memberColor(settlement.memberId)
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = finalColor.copy(alpha = 0.14f))) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Card(colors = CardDefaults.cardColors(containerColor = finalColor.copy(alpha = 0.3f))) {
                                Text(
                                    text = memberInitial(settlement.memberName),
                                    color = finalColor,
                                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp)
                                )
                            }
                            Text(settlement.memberName, color = finalColor)
                        }
                        Text("$${"%.2f".format(amount)}", style = MaterialTheme.typography.titleMedium, color = finalColor)
                    }
                }
            }
        }
    }
}

@Composable
fun QrScannerScreen(viewModel: MainViewModel, onDone: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    var memberName by rememberSaveable { mutableStateOf("") }
    var scannedText by rememberSaveable { mutableStateOf("") }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Escanear QR", style = MaterialTheme.typography.headlineSmall)
            IconButton(onClick = onBack) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar")
            }
        }

        OutlinedTextField(value = memberName, onValueChange = { memberName = it }, label = { Text("Tu nombre") }, modifier = Modifier.fillMaxWidth(), colors = travelTextFieldColors())

        if (!hasCameraPermission) {
            Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }, modifier = Modifier.fillMaxWidth()) {
                Text("Permitir cámara")
            }
        } else {
            QrCameraPreview(onQrDetected = { payload ->
                if (scannedText.isNotBlank()) return@QrCameraPreview
                scannedText = payload
                val code = payload.removePrefix("PLANTRAVEL|").substringAfterLast("/")
                viewModel.consumeInvite(code = code, memberName = memberName)
                onDone()
            })
        }

        if (scannedText.isNotBlank()) {
            SectionCard(title = "QR detectado") {
                Text(scannedText)
            }
        }
    }
}

@Composable
private fun QrCameraPreview(onQrDetected: (String) -> Unit) {
    val context = LocalContext.current
    val barcodeView = remember {
        CompoundBarcodeView(context).apply {
            barcodeView.decoderFactory = com.journeyapps.barcodescanner.DefaultDecoderFactory(listOf(BarcodeFormat.QR_CODE))
        }
    }

    DisposableEffect(Unit) {
        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult?) {
                val text = result?.text ?: return
                onQrDetected(text)
            }
        }
        barcodeView.decodeContinuous(callback)
        barcodeView.resume()
        onDispose {
            barcodeView.pause()
        }
    }

    AndroidView(
        factory = { barcodeView },
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp)
    )
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

@Composable
private fun SectionCard(
    title: String,
    subtitle: String? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (!subtitle.isNullOrBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall)
            }
            content()
        }
    }
}

@Composable
private fun CollapsibleHeader(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle)
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = if (expanded) "Ocultar" else "Mostrar"
            )
        }
    }
}

@Composable
private fun travelTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    cursorColor = MaterialTheme.colorScheme.primary,
    focusedLeadingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedLeadingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedTrailingIconColor = MaterialTheme.colorScheme.primary,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant
)

private fun memberInitial(name: String): String {
    return name.trim().firstOrNull()?.uppercase() ?: "?"
}

private fun memberColor(memberId: Long): Color {
    val palette = listOf(
        Color(0xFF1D4ED8),
        Color(0xFF7C3AED),
        Color(0xFF0D9488),
        Color(0xFFEA580C),
        Color(0xFFBE123C),
        Color(0xFF4338CA),
        Color(0xFF15803D),
        Color(0xFFB45309)
    )
    return palette[(memberId % palette.size).toInt()]
}










