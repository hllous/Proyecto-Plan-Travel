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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.presentation.auth.AuthState
import com.hllous.plantravel.presentation.auth.AuthViewModel
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.ui.screens.DestinationScreen
import com.hllous.plantravel.ui.screens.ExpenseScreen
import com.hllous.plantravel.ui.screens.GroupDetailScreen
import com.hllous.plantravel.ui.screens.GroupsScreen
import com.hllous.plantravel.ui.screens.HomeScreen
import com.hllous.plantravel.ui.screens.LoginScreen
import com.hllous.plantravel.ui.screens.ProfileScreen
import com.hllous.plantravel.ui.screens.ProfileSetupScreen
import com.hllous.plantravel.ui.screens.QrScannerScreen
import com.hllous.plantravel.ui.screens.RegisterScreen
import com.hllous.plantravel.ui.theme.FrauncesFamily
import com.hllous.plantravel.ui.utils.displayInitials
import com.hllous.plantravel.ui.theme.ProyectoPlanTravelTheme
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    val userEmail by authViewModel.userEmail.collectAsState()
    val pendingInviteCode by authViewModel.pendingInviteCode.collectAsState()
    val navController = rememberNavController()

    val displayName = (authState as? AuthState.Authenticated)?.displayName ?: ""

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
                onPendingInviteConsumed = { authViewModel.clearPendingInviteCode() },
                displayName = displayName,
                userEmail = userEmail,
                authViewModel = authViewModel,
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
    onPendingInviteConsumed: () -> Unit = {},
    displayName: String = "",
    userEmail: String? = null,
    authViewModel: AuthViewModel,
) {
    val viewModel = hiltViewModel<MainViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    val expenseViewModel = hiltViewModel<ExpenseViewModel>()
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

    val groupMessage by groupViewModel.message.collectAsState(initial = null)
    val expenseMessage by expenseViewModel.message.collectAsState(initial = null)

    LaunchedEffect(groupMessage) {
        val text = groupMessage
        if (!text.isNullOrBlank()) {
            snackbarHostState.showSnackbar(text)
            groupViewModel.clearMessage()
        }
    }

    LaunchedEffect(expenseMessage) {
        val text = expenseMessage
        if (!text.isNullOrBlank()) {
            snackbarHostState.showSnackbar(text)
            expenseViewModel.clearMessage()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerContent(
                displayName = displayName,
                userEmail = userEmail,
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
                if (currentRoute != "qr_scanner" && currentRoute != "profile" &&
                    currentRoute != "groups" && !currentRoute.startsWith("group_detail") &&
                    currentRoute != "destinations" && currentRoute != "gastos") {
                    TopAppBar(
                        title = {
                            Text(
                                "Plan Travel",
                                fontFamily = FrauncesFamily,
                                fontWeight = FontWeight.Medium,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menú")
                            }
                        },
                    )
                }
            },
            bottomBar = {
                if (currentRoute != "qr_scanner" && currentRoute != "profile" &&
                    !currentRoute.startsWith("group_detail")
                ) {
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
                    HomeScreen(
                        navController = navController,
                        displayName = displayName,
                        groupViewModel = groupViewModel,
                    )
                }
                composable("groups") {
                    GroupsScreen(
                        groupViewModel = groupViewModel,
                        mainViewModel = viewModel,
                        navController = navController,
                    )
                }
                composable("destinations") {
                    DestinationScreen(viewModel = viewModel, navController = navController)
                }
                composable("gastos") {
                    ExpenseScreen(viewModel = expenseViewModel, navController = navController)
                }
                composable("qr_scanner") {
                    QrScannerScreen(
                        viewModel = viewModel,
                        onDone = { navController.navigate("groups") },
                        onBack = { navController.navigateUp() },
                    )
                }
                composable("profile") {
                    ProfileScreen(authViewModel = authViewModel, onLogout = onLogout)
                }
                composable(
                    route = "group_detail/{groupId}",
                    arguments = listOf(navArgument("groupId") { type = NavType.StringType }),
                ) { backStackEntry ->
                    val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                    GroupDetailScreen(
                        groupId = groupId,
                        groupViewModel = groupViewModel,
                        mainViewModel = viewModel,
                        navController = navController
                    )
                }
            }
        }
    }
}

@Composable
fun DrawerContent(
    displayName: String,
    userEmail: String?,
    isDarkTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onLogout: () -> Unit,
    onNavigate: (String) -> Unit,
) {
    Surface(
        modifier = Modifier
            .width(300.dp)
            .fillMaxHeight(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(modifier = Modifier.fillMaxHeight()) {
            // User header
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.primary)
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val initials = displayInitials(displayName)

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = displayName.ifBlank { "Usuario" },
                        style = MaterialTheme.typography.headlineSmall,
                        fontFamily = FrauncesFamily,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                    if (!userEmail.isNullOrBlank()) {
                        Text(
                            text = userEmail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.78f),
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Person, contentDescription = null) },
                    label = { Text("Perfil") },
                    selected = false,
                    onClick = { onNavigate("profile") },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(horizontal = 12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = null,
                        )
                    },
                    label = { Text(if (isDarkTheme) "Modo claro" else "Modo oscuro") },
                    selected = false,
                    onClick = { onThemeChange(!isDarkTheme) },
                )
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label = { Text("Cerrar sesión") },
                    selected = false,
                    onClick = onLogout,
                )
            }
        }
    }
}

@Composable
fun BottomNavBar(currentRoute: String, navController: NavHostController) {
    NavigationBar {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            selected = currentRoute == "home",
            onClick = { navController.navigate("home") { launchSingleTop = true } },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.People, contentDescription = "Grupos") },
            label = { Text("Grupos") },
            selected = currentRoute == "groups",
            onClick = { navController.navigate("groups") { launchSingleTop = true } },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.LocationOn, contentDescription = "Destinos") },
            label = { Text("Destinos") },
            selected = currentRoute == "destinations",
            onClick = { navController.navigate("destinations") { launchSingleTop = true } },
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Gastos") },
            label = { Text("Gastos") },
            selected = currentRoute == "gastos",
            onClick = { navController.navigate("gastos") { launchSingleTop = true } },
        )
    }
}
