package com.hllous.plantravel
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
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
import com.hllous.plantravel.domain.model.ItemAssignment
import com.hllous.plantravel.domain.model.MemberSettlement
import com.hllous.plantravel.domain.model.SettlementWarning
import com.hllous.plantravel.domain.model.TravelGroup
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
    val viewModel = hiltViewModel<MainViewModel>()
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
// ====== HELPER FUNCTIONS ======
fun memberInitial(name: String): String {
    return name.trim().firstOrNull()?.uppercase() ?: "?"
}
fun memberColor(memberId: Long): Color {
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
@Composable
fun travelTextFieldColors() = OutlinedTextFieldDefaults.colors(
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
@Composable
fun CollapsibleHeader(
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
fun SectionCard(
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
fun rememberQrBitmap(content: String, size: Int = 512): Bitmap? {
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
fun QrCameraPreview(onQrDetected: (String) -> Unit) {
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
                Text("Permitir camara")
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
fun DrawerContent(
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
fun BottomNavBar(currentRoute: String, navController: NavHostController) {
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
                subtitle = "Comparte QR, codigo o link para invitar",
                onClick = { navController.navigate("groups") }
            )
        }
        item {
            HomeCard(
                icon = Icons.Default.LocationOn,
                title = "Destinos",
                subtitle = "Explora recomendaciones por region",
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
fun HomeCard(
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
        if (selectedGroupId == null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
                                OutlinedButton(onClick = { viewModel.selectGroup(availableGroup.id) }) {
                                    Text("Grupos")
                                    Icon(Icons.Default.ArrowForward, contentDescription = null)
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
                        OutlinedTextField(
                            value = adminName,
                            onValueChange = { adminName = it },
                            label = { Text("Tu nombre") },
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
                                    viewModel.createGroup(groupName, adminName)
                                    groupName = ""
                                    adminName = ""
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
                        value = joinName,
                        onValueChange = { joinName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    OutlinedTextField(
                        value = joinCode,
                        onValueChange = { joinCode = it.uppercase() },
                        label = { Text("Codigo") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = travelTextFieldColors()
                    )
                    Button(
                        onClick = {
                            viewModel.consumeInvite(joinCode, joinName)
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
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewModel.leaveSelectedGroupForDebug() }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Volver")
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
                            onClick = { viewModel.updateSelectedGroupName(editableGroupName) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Guardar")
                        }
                        Button(
                            onClick = { viewModel.deleteSelectedGroup() },
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
                            onClick = { viewModel.generateInvite() },
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
                                if (member.role.name != "ADMIN") {
                                    IconButton(onClick = { viewModel.deleteMember(member.id) }) {
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
                                onDelete = { viewModel.deleteInvite(invite.code) }
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
fun InviteCard(inviteCode: String, inviteLink: String, onDelete: () -> Unit) {
    val context = LocalContext.current
    val payload = "PLANTRAVEL|$inviteCode"
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
@Suppress("UNUSED_PARAMETER")
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
                    Text("No hay destinos para esta region")
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

private fun formatCurrency(cents: Long): String = "$${"%.2f".format(cents / 100.0)}"

private fun calculatePendingCents(
    items: List<ExpenseItem>,
    assignments: List<ItemAssignment>
): Long {
    return items.sumOf { item ->
        val assignedQuantity = assignments
            .filter { it.itemId == item.id }
            .sumOf { it.quantity }
        val pendingQuantity = (item.quantity - assignedQuantity).coerceAtLeast(0)
        if (item.quantity <= 0 || pendingQuantity == 0) 0L
        else (item.totalPriceCents * pendingQuantity) / item.quantity
    }
}

private fun calculatePendingCents(warnings: List<SettlementWarning>): Long {
    return warnings.sumOf { it.unassignedAmountCents }
}

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
                    item {
                        SummaryPill(
                            title = "Items",
                            value = itemsCount.toString(),
                            onColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    item {
                        SummaryPill(
                            title = "Total cargado",
                            value = formatCurrency(totalExpenseCents),
                            onColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    item {
                        SummaryPill(
                            title = "Pendiente",
                            value = formatCurrency(pendingCents),
                            onColor = MaterialTheme.colorScheme.onPrimary
                        )
                    }
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
            Surface(
                color = accent.copy(alpha = 0.22f),
                modifier = Modifier.size(34.dp)
            ) {
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
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
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
                            Surface(
                                color = accent.copy(alpha = 0.14f),
                                shape = MaterialTheme.shapes.large
                            ) {
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
                            IconButton(
                                enabled = remaining > 0,
                                onClick = { onAssignQuantity(myAssigned + 1) }
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

@Composable
private fun SettlementWarningCard(warning: SettlementWarning) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
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
                Text(
                    "Carga un gasto sin salir de la vista principal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Cerrar formulario")
            }
        }
        OutlinedTextField(
            value = itemName,
            onValueChange = onItemNameChange,
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = travelTextFieldColors()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = price,
                onValueChange = onPriceChange,
                label = { Text("Precio unitario") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = travelTextFieldColors()
            )
            OutlinedTextField(
                value = quantity,
                onValueChange = onQuantityChange,
                label = { Text("Cantidad") },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = travelTextFieldColors()
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancelar")
            }
            Button(onClick = onConfirm, modifier = Modifier.weight(1f)) {
                Text("Agregar")
            }
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
        modifier = Modifier
            .fillMaxWidth()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .animateContentSize()
                .background(
                    brush = Brush.verticalGradient(
                        listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f)
                        )
                    )
                )
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (member == null) {
                    Text("Selecciona tu perfil para ver tu monto final.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(color = accent.copy(alpha = 0.18f), modifier = Modifier.size(38.dp)) {
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
                            Text(member.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Text(
                                formatCurrency(amountCents),
                                style = MaterialTheme.typography.headlineSmall,
                                color = accent,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    IconButton(onClick = onToggleSettlements) {
                        Icon(
                            imageVector = if (settlementsExpanded) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp,
                            contentDescription = if (settlementsExpanded) "Ocultar montos" else "Mostrar montos"
                        )
                    }
                    FloatingActionButton(
                        onClick = onToggleAddItem,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Agregar item")
                    }
                }
            }

            if (addItemExpanded) {
                Spacer(Modifier.height(14.dp))
                AddExpenseItemPanel(
                    itemName = itemName,
                    price = price,
                    quantity = quantity,
                    onItemNameChange = onItemNameChange,
                    onPriceChange = onPriceChange,
                    onQuantityChange = onQuantityChange,
                    onDismiss = onDismissAddItem,
                    onConfirm = onConfirmAddItem
                )
            }

            if (settlementsExpanded) {
                Spacer(Modifier.height(14.dp))
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    settlements.forEach { settlement ->
                        SettlementCard(settlement = settlement)
                    }
                }
            }
        }
    }
}
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
    val currentMemberId by viewModel.currentMemberId.collectAsState(initial = null)
    var itemName by rememberSaveable { mutableStateOf("") }
    var price by rememberSaveable { mutableStateOf("") }
    var quantity by rememberSaveable { mutableStateOf("") }
    var groupsExpanded by rememberSaveable { mutableStateOf(true) }
    var settlementsExpanded by rememberSaveable { mutableStateOf(false) }
    var addItemExpanded by rememberSaveable { mutableStateOf(false) }

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

    val density = LocalDensity.current
    var bottomPanelHeightPx by remember { mutableStateOf(0) }
    val bottomPanelHeightDp = with(density) { bottomPanelHeightPx.toDp() }
    val selectedGroup = groups.firstOrNull { it.id == selectedGroupId }
    val totalExpenseCents = items.sumOf { it.totalPriceCents }
    val pendingCents = calculatePendingCents(settlementWarnings)
    val mySettlementCents = settlements.firstOrNull { it.memberId == currentMemberId }?.amountCents ?: 0L
    val currentMember = members.firstOrNull { it.id == currentMemberId }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
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
                                        onClick = { viewModel.setCurrentMember(member.id) }
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
                    viewModel.addExpenseItem(itemName, price, quantity)
                    if (itemName.isNotBlank() && price.isNotBlank() && quantity.isNotBlank()) {
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
