package com.hllous.plantravel

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.presentation.auth.AuthState
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.auth.AuthViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import com.hllous.plantravel.ui.screens.BallroomScreen
import com.hllous.plantravel.ui.screens.DestinationScreen
import com.hllous.plantravel.ui.screens.GroupsScreen
import com.hllous.plantravel.ui.screens.HomeScreen
import com.hllous.plantravel.ui.screens.LoginScreen
import com.hllous.plantravel.ui.screens.ProfileSetupScreen
import com.hllous.plantravel.ui.screens.QrScannerScreen
import com.hllous.plantravel.ui.screens.RegisterScreen
import com.hllous.plantravel.ui.theme.ProyectoPlanTravelTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabase: SupabaseClient
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        enableEdgeToEdge()
        setContent {
            var isDarkTheme by rememberSaveable { mutableStateOf(false) }
            ProyectoPlanTravelTheme(darkTheme = isDarkTheme) {
                PlanTravelApp(isDarkTheme = isDarkTheme, onThemeChange = { isDarkTheme = it })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent) {
        val uri = intent.data ?: return
        if (uri.scheme != "plantravel") return
        when (uri.host) {
            "auth" -> supabase.handleDeeplinks(intent)
            "invite" -> {
                val code = uri.lastPathSegment ?: return
                authViewModel.setPendingInviteCode(code)
            }
        }
    }
}

@Composable
fun PlanTravelApp(isDarkTheme: Boolean, onThemeChange: (Boolean) -> Unit) {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.state.collectAsState()
    val pendingInviteCode by authViewModel.pendingInviteCode.collectAsState()
    val navController = rememberNavController()

    LaunchedEffect(authState) {
        when (authState) {
            is AuthState.Unauthenticated, is AuthState.Error ->
                navController.navigate("login") { popUpTo(0) { inclusive = true } }
            is AuthState.NeedsProfileSetup ->
                navController.navigate("profile_setup") { popUpTo(0) { inclusive = true } }
            is AuthState.Authenticated ->
                navController.navigate("main") { popUpTo(0) { inclusive = true } }
            is AuthState.Loading -> Unit
        }
    }

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        composable("login") {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateToRegister = { navController.navigate("register") }
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                onNavigateToLogin = { navController.navigateUp() }
            )
        }
        composable("profile_setup") {
            ProfileSetupScreen(viewModel = authViewModel)
        }
        composable("main") {
            MainAppContent(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onLogout = { authViewModel.logout() },
                pendingInviteCode = pendingInviteCode,
                onPendingInviteConsumed = { authViewModel.clearPendingInviteCode() }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContent(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    pendingInviteCode: String? = null,
    onPendingInviteConsumed: () -> Unit = {}
) {
    val viewModel = hiltViewModel<MainViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState(initial = null)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    LaunchedEffect(pendingInviteCode) {
        val code = pendingInviteCode ?: return@LaunchedEffect
        viewModel.consumeInvite(code)
        onPendingInviteConsumed()
    }

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
                onLogout = onLogout,
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
                composable("home") { HomeScreen(navController = navController) }
                composable("groups") { GroupsScreen(groupViewModel = groupViewModel, mainViewModel = viewModel, navController = navController) }
                composable("destinations") { DestinationScreen(viewModel = viewModel, navController = navController) }
                composable("gastos") { BallroomScreen(viewModel = viewModel, navController = navController) }
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
fun DrawerContent(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
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
                Text("Plan Travel", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimary)
                Text("Organiza tus viajes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
            }
            LazyColumn(
                modifier = Modifier.weight(1f, fill = true).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { NavigationDrawerItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") }, selected = false, onClick = { onNavigate("home") }) }
                item { NavigationDrawerItem(icon = { Icon(Icons.Default.People, contentDescription = "Grupos") }, label = { Text("Grupos") }, selected = false, onClick = { onNavigate("groups") }) }
                item { NavigationDrawerItem(icon = { Icon(Icons.Default.LocationOn, contentDescription = "Destinos") }, label = { Text("Destinos") }, selected = false, onClick = { onNavigate("destinations") }) }
                item { NavigationDrawerItem(icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Gastos") }, label = { Text("Gastos") }, selected = false, onClick = { onNavigate("gastos") }) }
                item { NavigationDrawerItem(icon = { Icon(Icons.Default.QrCode, contentDescription = "Escanear") }, label = { Text("Escanear QR") }, selected = false, onClick = { onNavigate("qr_scanner") }) }
            }
            Column(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NavigationDrawerItem(
                    icon = { Icon(if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode, contentDescription = "Theme") },
                    label = { Text(if (isDarkTheme) "Modo claro" else "Modo oscuro") },
                    selected = false,
                    onClick = { onThemeChange(!isDarkTheme) }
                )
                NavigationDrawerItem(
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = onLogout
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String, navController: NavHostController) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary) {
        NavigationBarItem(icon = { Icon(Icons.Default.Home, contentDescription = "Home") }, label = { Text("Home") }, selected = currentRoute == "home", onClick = { navController.navigate("home") })
        NavigationBarItem(icon = { Icon(Icons.Default.People, contentDescription = "Grupos") }, label = { Text("Grupos") }, selected = currentRoute == "groups", onClick = { navController.navigate("groups") })
        NavigationBarItem(icon = { Icon(Icons.Default.LocationOn, contentDescription = "Destinos") }, label = { Text("Destinos") }, selected = currentRoute == "destinations", onClick = { navController.navigate("destinations") })
        NavigationBarItem(icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Gastos") }, label = { Text("Gastos") }, selected = currentRoute == "gastos", onClick = { navController.navigate("gastos") })
    }
}
