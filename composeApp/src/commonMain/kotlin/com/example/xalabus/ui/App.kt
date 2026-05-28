package com.example.xalabus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.XalaContext
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.core.util.MapFileManager
import com.example.xalabus.ui.admin.AdminDashboardScreen
import com.example.xalabus.ui.admin.AdminLoginScreen
import com.example.xalabus.ui.admin.AdminViewModel
import com.example.xalabus.ui.auth.AuthViewModel
import com.example.xalabus.ui.auth.ForgotPasswordViewModel
import com.example.xalabus.ui.auth.ForgotPasswordScreen
import com.example.xalabus.ui.auth.LoginScreen
import com.example.xalabus.ui.auth.RegisterScreen
import com.example.xalabus.ui.home.FavoritosScreen
import com.example.xalabus.ui.home.HomeScreen
import com.example.xalabus.ui.map.MapScreen
import com.example.xalabus.ui.ratings.RatingDialog
import com.example.xalabus.ui.ratings.TopRatedRoutesScreen
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.ui.onboarding.OnboardingScreen
import com.example.xalabus.core.prefs.OnboardingPreferences
import com.example.xalabus.ui.viewmodel.FavoritosViewModel
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import com.example.xalabus.ui.viewmodel.RatingViewModel
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel
import com.example.xalabus.ui.viewmodel.RouteTimeUiState
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import xalabus.composeapp.generated.resources.*
import xalabus.composeapp.generated.resources.Res

private enum class AppDestination { AUTH, MAIN, ADMIN_LOGIN, ADMIN_DASHBOARD }

// CU-03: agregado FORGOT_PASSWORD al enum de pantallas de auth
private enum class AuthScreen { LOGIN, REGISTER, FORGOT_PASSWORD }


@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Text("Cargando XalaBus...", color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun App(
    driverFactory: DriverFactory,
    fileManager: MapFileManager
) {
    val repository     = remember { XalaContext.getRepository(driverFactory) }
    val viewModel      = remember { RouteViewModel(repository, fileManager) }
    val authViewModel  = remember { AuthViewModel() }
    val adminViewModel = remember { AdminViewModel() }
    val reportsViewModel = remember(repository) {
        com.example.xalabus.ui.viewmodel.ReportsViewModel(routeRepository = repository)
    }
    // CU-03: ViewModel de recuperación de contraseña
    val forgotPasswordViewModel = remember { ForgotPasswordViewModel() }
    // CU-10: ViewModel de favoritos
    val favoritosViewModel = remember { FavoritosViewModel() }
    // CU-11: ViewModel de tiempo estimado de traslado (cálculo fijo desde geometría)
    val routeTimeViewModel = remember { RouteTimeViewModel() }
    // CU-13: ViewModel de incidentes GPS
    val incidentViewModel = remember { IncidentViewModel() }
    // CU-23 / CU-24: ViewModel de calificaciones de rutas
    val ratingViewModel = remember { RatingViewModel() }

    val systemDark  = isSystemInDarkTheme()
    var isDarkMode  by remember { mutableStateOf(systemDark) }
    val colorScheme = if (isDarkMode) XalaBusDarkColors else XalaBusLightColors

    var isAuthenticated by remember { mutableStateOf(authViewModel.isSessionActive()) }
    var destination by remember {
        mutableStateOf(AppDestination.MAIN)
    }
    var currentAuthScreen by remember { mutableStateOf(AuthScreen.LOGIN) }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (destination) {
                AppDestination.AUTH -> when (currentAuthScreen) {
                    AuthScreen.LOGIN -> LoginScreen(
                        viewModel            = authViewModel,
                        onLoginSuccess       = {
                            isAuthenticated = true
                            destination = AppDestination.MAIN
                        },
                        onNavigateToRegister = { currentAuthScreen = AuthScreen.REGISTER },
                        onBack               = { destination = AppDestination.MAIN },
                        onForgotPassword     = { currentAuthScreen = AuthScreen.FORGOT_PASSWORD }
                    )
                    AuthScreen.REGISTER -> RegisterScreen(
                        viewModel         = authViewModel,
                        onRegisterSuccess = { currentAuthScreen = AuthScreen.LOGIN },
                        onNavigateToLogin = { currentAuthScreen = AuthScreen.LOGIN }
                    )
                    // CU-03
                    AuthScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
                        viewModel = forgotPasswordViewModel,
                        onSuccess = { currentAuthScreen = AuthScreen.LOGIN },
                        onBack    = { currentAuthScreen = AuthScreen.LOGIN }
                    )
                }

                AppDestination.MAIN -> MainAppContent(
                    driverFactory    = driverFactory,
                    fileManager      = fileManager,
                    viewModel        = viewModel,
                    authViewModel    = authViewModel,
                    isDarkMode       = isDarkMode,
                    isAuthenticated  = isAuthenticated,
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    onSignInRequest  = {
                        destination = AppDestination.AUTH
                        currentAuthScreen = AuthScreen.LOGIN
                    },
                    onSignOut        = {
                        authViewModel.signOut()
                        isAuthenticated = false
                    },
                    onAdminRequest   = { destination = AppDestination.ADMIN_LOGIN },
                    reportsViewModel   = reportsViewModel,
                    favoritosViewModel = favoritosViewModel,
                    incidentViewModel  = incidentViewModel,
                    routeTimeViewModel = routeTimeViewModel,
                    ratingViewModel    = ratingViewModel
                )

                AppDestination.ADMIN_LOGIN -> AdminLoginScreen(
                    viewModel = adminViewModel,
                    onLoginSuccess = { destination = AppDestination.ADMIN_DASHBOARD },
                    onBack = { destination = AppDestination.MAIN }
                )

                AppDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(
                    viewModel  = adminViewModel,
                    reportsRepository = com.example.xalabus.data.reports.ReportsRepository(),
                    routeRepository = repository,
                    onSignOut  = {
                        adminViewModel.signOut()
                        destination = AppDestination.MAIN
                    }
                )
            }
        }
    }
}

@Composable
private fun MainAppContent(
    driverFactory: DriverFactory,
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    authViewModel: AuthViewModel,
    isDarkMode: Boolean,
    isAuthenticated: Boolean,
    onToggleDarkMode: () -> Unit,
    onSignInRequest: () -> Unit,
    onSignOut: () -> Unit,
    onAdminRequest: () -> Unit,
    reportsViewModel: com.example.xalabus.ui.viewmodel.ReportsViewModel,
    favoritosViewModel: FavoritosViewModel,
    incidentViewModel: IncidentViewModel,
    routeTimeViewModel: RouteTimeViewModel,
    ratingViewModel: RatingViewModel
) {
    LaunchedEffect(Unit) { viewModel.initializeData() }

    val isLoaded by viewModel.isDataLoaded.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!OnboardingPreferences.isOnboardingCompleted()) }
    var showGeneralReport by remember { mutableStateOf(false) }
    // CU-13: controla la pantalla de reporte de incidente con GPS
    var showIncidentReport by remember { mutableStateOf(false) }
    // CU-10 (extensión): controla la pantalla de rutas favoritas del drawer
    var showFavoritos by remember { mutableStateOf(false) }
    // CU-24: controla la pantalla de rutas mejor calificadas
    var showTopRated by remember { mutableStateOf(false) }
    // CU-23: controla el diálogo de calificar ruta (activado desde TopRated o MapDetail)
    var ratingRouteId   by remember { mutableStateOf("") }
    var ratingRouteName by remember { mutableStateOf("") }
    var showRatingDialog by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope       = rememberCoroutineScope()

    if (!isLoaded) {
        LoadingScreen()
    } else if (showOnboarding) {
        OnboardingScreen(
            onFinish = {
                OnboardingPreferences.markOnboardingCompleted()
                showOnboarding = false
            }
        )
    } else if (showIncidentReport) {
        // CU-13: pantalla completa de reporte de incidente con GPS
        com.example.xalabus.ui.reports.ReportIncidentScreen(
            viewModel = incidentViewModel,
            onDismiss = { showIncidentReport = false }
        )
    } else if (showFavoritos) {
        // CU-10 (extensión): pantalla completa de rutas favoritas del usuario
        FavoritosScreen(
            favoritosViewModel = favoritosViewModel,
            routeViewModel     = viewModel,
            onNavigateToRoute  = { routeId ->
                viewModel.selectRoute(routeId)
                showFavoritos = false
                showMap = true
            },
            onDismiss = { showFavoritos = false }
        )
    } else if (showTopRated) {
        // CU-24: pantalla de rutas mejor calificadas (acceso público)
        TopRatedRoutesScreen(
            viewModel    = ratingViewModel,
            onRouteClick = { routeId ->
                viewModel.selectRoute(routeId)
                showTopRated = false
                showMap = true
            },
            onRateRoute  = { routeId, routeName ->
                // CU-23: el botón calificar de la tarjeta abre el diálogo
                ratingRouteId   = routeId
                ratingRouteName = routeName
                showRatingDialog = true
            },
            onDismiss = { showTopRated = false }
        )
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet(
                    drawerContainerColor = MaterialTheme.colorScheme.surface,
                    drawerContentColor   = MaterialTheme.colorScheme.onSurface
                ) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Configuraciones",
                        modifier = Modifier.padding(16.dp),
                        style    = MaterialTheme.typography.titleLarge,
                        color    = MaterialTheme.colorScheme.onSurface
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)

                    NavigationDrawerItem(
                        icon   = {
                            Icon(
                                if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode,
                                contentDescription = null
                            )
                        },
                        label  = { Text("Modo Oscuro") },
                        selected = false,
                        onClick  = onToggleDarkMode,
                        badge  = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onToggleDarkMode() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = MaterialTheme.colorScheme.primary
                                )
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color    = MaterialTheme.colorScheme.outline
                    )

                    // CU-24: Rutas mejor calificadas — visible SIEMPRE (sin requerir login)
                    NavigationDrawerItem(
                        icon     = { Icon(Icons.Default.Star, contentDescription = null) },
                        label    = { Text("Rutas Mejor Calificadas") },
                        selected = false,
                        onClick  = {
                            scope.launch { drawerState.close() }
                            showTopRated = true
                        },
                        colors = NavigationDrawerItemDefaults.colors(
                            unselectedTextColor = MaterialTheme.colorScheme.primary,
                            unselectedIconColor = MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color    = MaterialTheme.colorScheme.outline
                    )

                    if (isAuthenticated) {

                        // CU-10 (extensión): ver rutas favoritas guardadas
                        NavigationDrawerItem(
                            icon     = { Icon(Icons.Default.Favorite, contentDescription = null) },
                            label    = { Text("Mis Rutas Favoritas") },
                            selected = false,
                            onClick  = {
                                scope.launch { drawerState.close() }
                                showFavoritos = true
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        // CU-13: opción de reportar incidente con GPS
                        NavigationDrawerItem(
                            icon   = { Icon(Icons.Default.ReportProblem, contentDescription = null) },
                            label  = { Text("Reportar Incidente") },
                            selected = false,
                            onClick  = {
                                scope.launch { drawerState.close() }
                                showIncidentReport = true
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )

                        NavigationDrawerItem(
                            icon   = { Icon(Icons.Default.Feedback, contentDescription = null) },
                            label  = { Text("Reporte General") },
                            selected = false,
                            onClick  = {
                                scope.launch { drawerState.close() }
                                showGeneralReport = true
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }

                    // CU-09: panel de administración para revisar paradas
                    NavigationDrawerItem(
                        icon   = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                        label  = { Text("Administración") },
                        selected = false,
                        onClick  = {
                            scope.launch { drawerState.close() }
                            onAdminRequest()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color    = MaterialTheme.colorScheme.outline
                    )

                    if (isAuthenticated) {
                        NavigationDrawerItem(
                            icon   = { Icon(Icons.Default.Logout, contentDescription = null) },
                            label  = { Text("Cerrar sesión") },
                            selected = false,
                            onClick  = {
                                scope.launch { drawerState.close() }
                                onSignOut()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = MaterialTheme.colorScheme.error,
                                unselectedIconColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    } else {
                        NavigationDrawerItem(
                            icon   = { Icon(Icons.Default.Login, contentDescription = null) },
                            label  = { Text("Iniciar sesión") },
                            selected = false,
                            onClick  = {
                                scope.launch { drawerState.close() }
                                onSignInRequest()
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                unselectedTextColor = MaterialTheme.colorScheme.primary,
                                unselectedIconColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        ) {
            if (!showMap) {
                HomeScreen(
                    viewModel    = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onRouteClick = { routeId ->
                        viewModel.selectRoute(routeId)
                        showMap = true
                    }
                )
            } else {
                MapDetailView(
                    driverFactory      = driverFactory,
                    fileManager        = fileManager,
                    viewModel          = viewModel,
                    authViewModel      = authViewModel,
                    reportsViewModel   = reportsViewModel,
                    favoritosViewModel = favoritosViewModel,
                    routeTimeViewModel = routeTimeViewModel,
                    incidentViewModel  = incidentViewModel,
                    ratingViewModel    = ratingViewModel,
                    isDarkMode         = isDarkMode,
                    isAuthenticated    = isAuthenticated,
                    onBack             = {
                        routeTimeViewModel.resetState()
                        showMap = false
                    }
                )
            }
        }

        if (showGeneralReport) {
            com.example.xalabus.ui.reports.GeneralReportDialog(
                viewModel = reportsViewModel,
                onDismiss = { showGeneralReport = false }
            )
        }

        // CU-23: diálogo de calificar ruta (puede abrirse desde TopRated o MapDetail)
        if (showRatingDialog && ratingRouteId.isNotEmpty()) {
            RatingDialog(
                routeName = ratingRouteName,
                routeId   = ratingRouteId,
                userId    = authViewModel.currentUserId(),
                viewModel = ratingViewModel,
                onDismiss = {
                    showRatingDialog = false
                    ratingRouteId    = ""
                    ratingRouteName  = ""
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MapDetailView(
    driverFactory: com.example.xalabus.db.DriverFactory,
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    authViewModel: AuthViewModel,
    reportsViewModel: com.example.xalabus.ui.viewmodel.ReportsViewModel,
    favoritosViewModel: FavoritosViewModel,
    routeTimeViewModel: RouteTimeViewModel,
    incidentViewModel: IncidentViewModel,
    ratingViewModel: RatingViewModel,
    isDarkMode: Boolean,
    isAuthenticated: Boolean,
    onBack: () -> Unit
) {
    val selectedRoute  by viewModel.selectedRoute.collectAsState()
    val routePoints    by viewModel.selectedRoutePoints.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val routeId       = selectedRoute?.id ?: ""
    val parsedRouteId = routeId.toIntOrNull() ?: 0

    var routeReportMessage by remember { mutableStateOf("") }
    val reportState by reportsViewModel.uiState.collectAsState()
    val stopState by reportsViewModel.stopUiState.collectAsState()
    val userLocation by viewModel.userLocation.collectAsState()

    // CU-10: estado de favoritos para esta ruta
    val isFavorito by favoritosViewModel.isCurrentRouteFavorite.collectAsState()

    // CU-11: estado del estimador de tiempo de traslado
    val routeTimeState by routeTimeViewModel.uiState.collectAsState()

    // CU-23: controla la visibilidad del diálogo de calificación desde MapDetail
    var showRatingDialog by remember { mutableStateOf(false) }

    // Cargar estado de favorito al seleccionar ruta
    LaunchedEffect(routeId) {
        if (routeId.isNotEmpty()) {
            favoritosViewModel.checkIfFavorite(routeId)
        }
    }

    // CU-11: calcular duración en cuanto llegan los puntos de la ruta
    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) {
            routeTimeViewModel.calculateFromGeometry(routePoints)
        }
    }

    // CU-09: sincronizar paradas aprobadas al abrir la ruta (si hay internet)
    LaunchedEffect(parsedRouteId) {
        if (parsedRouteId > 0) {
            viewModel.loadLocalStops(routeId)
            reportsViewModel.syncAcceptedStopsToLocal(parsedRouteId) {
                viewModel.loadLocalStops(routeId)
            }
        }
    }

    BottomSheetScaffold(
        scaffoldState       = scaffoldState,
        sheetPeekHeight     = 200.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                // ── Encabezado con nombre de ruta y botón de favorito (CU-10) ──
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            selectedRoute?.name ?: "Ruta Desconocida",
                            style       = MaterialTheme.typography.headlineSmall,
                            fontWeight  = FontWeight.Bold,
                            color       = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // CU-10: icono de favorito
                    if (isAuthenticated) {
                        IconButton(
                            onClick = {
                                if (isFavorito) favoritosViewModel.removeFromFavorites(routeId)
                                else favoritosViewModel.addToFavorites(routeId)
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorito) Icons.Default.Favorite
                                              else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorito) "Quitar de favoritos"
                                                     else "Agregar a favoritos",
                                tint = if (isFavorito) MaterialTheme.colorScheme.error
                                       else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))

                // CU-11: chip de tiempo estimado
                when (val ts = routeTimeState) {
                    is RouteTimeUiState.Result -> {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    "~${ts.totalMinutes} min",
                                    style = MaterialTheme.typography.labelMedium
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Schedule,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                    else -> Unit
                }

                // CU-23: botón calificar (solo para usuarios autenticados)
                if (isAuthenticated && routeId.isNotEmpty()) {
                    OutlinedButton(
                        onClick  = { showRatingDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Calificar esta ruta")
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    ) {
        // contenido principal del scaffold (mapa)
        MapScreen(
            fileManager = fileManager,
            viewModel = viewModel,
            isDarkMode = isDarkMode,
            routeTimeViewModel = routeTimeViewModel,
            incidentViewModel = incidentViewModel,
            onUserLocationChanged = { lat, lng ->
                viewModel.updateUserLocation(lat, lng)
            }
        )
    }

    // CU-23: diálogo de calificación desde MapDetail
    if (showRatingDialog) {
        RatingDialog(
            routeName = selectedRoute?.name ?: "",
            routeId   = routeId,
            userId    = authViewModel.currentUserId(),
            viewModel = ratingViewModel,
            onDismiss = { showRatingDialog = false }
        )
    }
}
