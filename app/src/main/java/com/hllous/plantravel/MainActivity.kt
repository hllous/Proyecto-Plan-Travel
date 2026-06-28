package com.hllous.plantravel

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.core.view.drawToBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.hllous.plantravel.presentation.MainViewModel
import com.hllous.plantravel.presentation.auth.AuthState
import com.hllous.plantravel.presentation.auth.AuthViewModel
import com.hllous.plantravel.presentation.destination.DestinationViewModel
import com.hllous.plantravel.presentation.expense.ExpenseViewModel
import com.hllous.plantravel.presentation.group.GroupViewModel
import com.hllous.plantravel.presentation.itinerary.ItineraryEventDraft
import com.hllous.plantravel.presentation.itinerary.ItineraryViewModel
import com.hllous.plantravel.ui.screens.DestinationScreen
import com.hllous.plantravel.ui.screens.ExpenseScreen
import com.hllous.plantravel.ui.screens.GroupsScreen
import com.hllous.plantravel.ui.screens.HomeScreen
import com.hllous.plantravel.ui.screens.ItineraryScreen
import com.hllous.plantravel.ui.screens.LoginScreen
import com.hllous.plantravel.ui.screens.PollScreen
import com.hllous.plantravel.presentation.poll.PollViewModel
import com.hllous.plantravel.ui.screens.ProfileScreen
import com.hllous.plantravel.ui.screens.ProfileSetupScreen
import com.hllous.plantravel.ui.screens.QrScannerScreen
import com.hllous.plantravel.ui.screens.RegisterScreen
import com.hllous.plantravel.ui.theme.ProyectoPlanTravelTheme
import androidx.navigation.NavType
import androidx.navigation.navArgument
import kotlinx.serialization.json.Json
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.hypot

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var supabase: SupabaseClient
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleDeepLink(intent)
        enableEdgeToEdge()
        val prefs = getSharedPreferences(THEME_PREFS, MODE_PRIVATE)
        val initialDarkTheme = prefs.getBoolean(THEME_PREF_KEY, false)
        setContent {
            val view = LocalView.current
            var isDarkTheme by rememberSaveable { mutableStateOf(initialDarkTheme) }
            var overlayBitmap by remember { mutableStateOf<androidx.compose.ui.graphics.ImageBitmap?>(null) }
            var overlayOrigin by remember { mutableStateOf<Offset?>(null) }
            val revealProgress = remember { androidx.compose.animation.core.Animatable(1f) }
            var revealJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }
            var isThemeTransitionInProgress by remember { mutableStateOf(false) }
            var rootSize by remember { mutableStateOf(IntSize.Zero) }
            val scope = rememberCoroutineScope()

            val handleThemeChange: (Boolean, Offset?) -> Unit = handleThemeChange@{ targetDark, origin ->
                if (isThemeTransitionInProgress) return@handleThemeChange
                if (targetDark == isDarkTheme && overlayBitmap == null) return@handleThemeChange
                revealJob?.cancel()
                isThemeTransitionInProgress = true
                revealJob = scope.launch {
                    val snapshot = captureThemeOverlay(view)
                    if (snapshot == null) {
                        overlayBitmap = null
                        overlayOrigin = null
                        isDarkTheme = targetDark
                        prefs.edit().putBoolean(THEME_PREF_KEY, targetDark).apply()
                        isThemeTransitionInProgress = false
                        return@launch
                    }
                    revealProgress.snapTo(0f)
                    overlayBitmap = snapshot
                    overlayOrigin = origin
                    isDarkTheme = targetDark
                    prefs.edit().putBoolean(THEME_PREF_KEY, targetDark).apply()
                    revealProgress.animateTo(
                        1f,
                        animationSpec = androidx.compose.animation.core.tween(
                            durationMillis = 520,
                            easing = androidx.compose.animation.core.FastOutSlowInEasing
                        )
                    )
                    overlayBitmap = null
                    overlayOrigin = null
                    isThemeTransitionInProgress = false
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { rootSize = it }
            ) {
                ProyectoPlanTravelTheme(darkTheme = isDarkTheme) {
                    PlanTravelApp(
                        isDarkTheme = isDarkTheme,
                        onThemeChange = handleThemeChange
                    )
                }
                val bitmap = overlayBitmap
                if (bitmap != null) {
                    val origin = overlayOrigin ?: Offset(
                        rootSize.width / 2f,
                        rootSize.height / 2f
                    )
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    ) {
                        val maxRadius = maxRevealRadius(origin, size)
                        val radius = revealProgress.value * maxRadius
                        drawImage(
                            image = bitmap,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt())
                        )
                        drawCircle(
                            color = Color.Transparent,
                            radius = radius,
                            center = origin,
                            blendMode = BlendMode.Clear
                        )
                    }
                }
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

private const val THEME_PREFS = "plan_travel_prefs"
private const val THEME_PREF_KEY = "dark_theme"
private const val TAG = "MainActivity"

private fun captureThemeOverlay(view: View) =
    runCatching { view.drawToBitmap().asImageBitmap() }
        .onFailure { error ->
            Log.w(
                TAG,
                "Theme transition snapshot failed; switching theme without reveal animation.",
                error
            )
        }
        .getOrNull()

private fun maxRevealRadius(origin: Offset, size: Size): Float {
    val topLeft = hypot(origin.x, origin.y)
    val topRight = hypot(size.width - origin.x, origin.y)
    val bottomLeft = hypot(origin.x, size.height - origin.y)
    val bottomRight = hypot(size.width - origin.x, size.height - origin.y)
    return maxOf(topLeft, topRight, bottomLeft, bottomRight)
}

@Composable
fun PlanTravelApp(isDarkTheme: Boolean, onThemeChange: (Boolean, Offset?) -> Unit) {
    val authViewModel = hiltViewModel<AuthViewModel>()
    val authState by authViewModel.state.collectAsState()
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
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onNavigateToRegister = { navController.navigate("register") },
            )
        }
        composable("register") {
            RegisterScreen(
                viewModel = authViewModel,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onNavigateToLogin = { navController.navigateUp() },
            )
        }
        composable("profile_setup") {
            ProfileSetupScreen(
                viewModel = authViewModel,
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
            )
        }
        composable("main") {
            MainAppContent(
                isDarkTheme = isDarkTheme,
                onThemeChange = onThemeChange,
                onLogout = { authViewModel.logout() },
                pendingInviteCode = pendingInviteCode,
                onPendingInviteConsumed = { authViewModel.clearPendingInviteCode() },
                displayName = displayName,
                authViewModel = authViewModel,
            )
        }
    }
}

@Composable
fun MainAppContent(
    isDarkTheme: Boolean,
    onThemeChange: (Boolean, Offset?) -> Unit,
    onLogout: () -> Unit,
    pendingInviteCode: String? = null,
    onPendingInviteConsumed: () -> Unit = {},
    displayName: String = "",
    authViewModel: AuthViewModel,
) {
    val viewModel = hiltViewModel<MainViewModel>()
    val groupViewModel = hiltViewModel<GroupViewModel>()
    val expenseViewModel = hiltViewModel<ExpenseViewModel>()
    val destinationViewModel = hiltViewModel<DestinationViewModel>()
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val message by viewModel.message.collectAsState(initial = null)
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

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route ?: "home"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (currentRoute != "qr_scanner" && currentRoute != "profile" && !currentRoute.startsWith("itinerary") && currentRoute != "poll_detail") {
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
                    pollViewModel = hiltViewModel<PollViewModel>(),
                    destinationViewModel = destinationViewModel,
                    isDarkTheme = isDarkTheme,
                    onThemeChange = onThemeChange,
                    onProfileClick = { navController.navigate("profile") },
                )
            }
            composable("groups") {
                GroupsScreen(
                    groupViewModel = groupViewModel,
                    mainViewModel = viewModel,
                    onNavigateToQr = { navController.navigate("qr_scanner") },
                )
            }
            composable("destinations") {
                DestinationScreen(viewModel = destinationViewModel, navController = navController)
            }
            composable("gastos") {
                ExpenseScreen(viewModel = expenseViewModel, navController = navController)
            }
            composable("qr_scanner") {
                QrScannerScreen(
                    viewModel = viewModel,
                    onDone = {
                        navController.navigate("groups") {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    onBack = { navController.navigateUp() },
                )
            }
            composable("profile") {
                ProfileScreen(
                    authViewModel = authViewModel,
                    onLogout = onLogout,
                    onBack = { navController.navigateUp() },
                    groupViewModel = groupViewModel,
                )
            }
            composable(
                route = "itinerary?draft={draft}",
                arguments = listOf(
                    navArgument("draft") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                ),
            ) { backStackEntry ->
                val draftJson = backStackEntry.arguments?.getString("draft")
                val initialDraft = remember(draftJson) {
                    draftJson?.let {
                        runCatching { Json.decodeFromString<ItineraryEventDraft>(it) }.getOrNull()
                    }
                }
                ItineraryScreen(
                    viewModel = hiltViewModel<ItineraryViewModel>(),
                    navController = navController,
                    initialDraft = initialDraft,
                )
            }
            composable("poll_detail") {
                PollScreen(
                    viewModel = hiltViewModel<PollViewModel>(),
                    navController = navController,
                )
            }
            // PROTOTYPE — delete after animation decisions are locked (#59)
            composable("prototype_poll_animation") {
                com.hllous.plantravel.ui.prototype.PollAnimationPrototypeScreen()
            }
        }
    }
}


@Composable
fun BottomNavBar(currentRoute: String, navController: NavHostController) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        listOf(
            Triple("home", Icons.Default.Home, "Inicio"),
            Triple("groups", Icons.Default.People, "Grupos"),
            Triple("destinations", Icons.Default.LocationOn, "Destinos"),
            Triple("gastos", Icons.Default.AccountBalanceWallet, "Gastos"),
        ).forEach { (route, icon, label) ->
            NavigationBarItem(
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label) },
                selected = currentRoute == route,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            )
        }
    }
}
