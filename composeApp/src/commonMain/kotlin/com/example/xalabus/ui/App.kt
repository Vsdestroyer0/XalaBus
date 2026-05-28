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
import com.example.xalabus.ui.auth.AuthUiState
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
    val forgotPasswordViewModel = remember { ForgotPasswordViewModel() }
    val favoritosViewModel = remember { FavoritosViewModel() }
    val routeTimeViewModel = remember { RouteTimeViewModel() }
    val incidentViewModel = remember { IncidentViewModel() }
    val ratingViewModel = remember { RatingViewModel() }

    val systemDark  = isSystemInDarkTheme()
    var isDarkMode  by remember { mutableStateOf(systemDark) }
    val colorScheme = if (isDarkMode) XalaBusDarkColors else XalaBusLightColors

    // isAuthenticated se deriva reactivamente: sesion activa al inicio
    // O cuando authViewModel.uiState cambia a Success (login exitoso)
    var isAuthenticated by remember { mutableStateOf(authViewModel.isSessionActive()) }
    val authUiState by authViewModel.uiState.collectAsState()

    // Reacciona a cada cambio de authUiState para mantener isAuthenticated sincronizado
    LaunchedEffect(authUiState) {
        when (authUiState) {
            is AuthUiState.Success -> {
                isAuthenticated = true
            }
            else -> {
                // Solo actualiza a false si Supabase confirma que no hay sesion activa
                // (no lo ponemos en false en Error/Loading para no cerrar sesion por error)
                isAuthenticated = authViewModel.isSessionActive()
            }
        }
    }

    var destination by remember { mutableStateOf(AppDestination.MAIN) }
    var currentAuthScreen by remember { mutableStateOf(AuthScreen.LOGIN) }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when (destination) {
                AppDestination.AUTH -> when (currentAuthScreen) {
                    AuthScreen.LOGIN -> LoginScreen(
                        viewModel            = authViewModel,
                        onLoginSuccess       = {
                            // destination vuelve a MAIN para re-mostrar MainAppContent
                            // isAuthenticated ya se actualizo via LaunchedEffect(authUiState)
                            destination = AppDestination.AUTH  // fuerza recompose cambiando a AUTH...
                            destination = AppDestination.MAIN  // ...y luego a MAIN
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
    var showIncidentReport by remember { mutableStateOf(false) }
    var showFavoritos by remember { mutableStateOf(false) }
    var showTopRated by remember { mutableStateOf(false) }
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
        com.example.xalabus.ui.reports.ReportIncidentScreen(
            viewModel = incidentViewModel,
            onDismiss = { showIncidentReport = false }
        )
    } else if (showFavoritos) {
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
        TopRatedRoutesScreen(
            viewModel    = ratingViewModel,
            onRouteClick = { routeId ->
                viewModel.selectRoute(routeId)
                showTopRated = false
                showMap = true
            },
            onRateRoute  = { routeId, routeName ->
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

                    NavigationDrawerItem(
                        icon   = { Icon(Icons.Default.AdminPanelSettings, contentDescription = null) },
                        label  = { Text("Administracion") },
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
                            label  = { Text("Cerrar sesion") },
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
                            label  = { Text("Iniciar sesion") },
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

    val isFavorito by favoritosViewModel.isCurrentRouteFavorite.collectAsState()
    val routeTimeState by routeTimeViewModel.uiState.collectAsState()
    var showRatingDialog by remember { mutableStateOf(false) }

    LaunchedEffect(routeId) {
        if (routeId.isNotEmpty()) favoritosViewModel.checkIfFavorite(routeId)
    }

    LaunchedEffect(routePoints) {
        if (routePoints.isNotEmpty()) routeTimeViewModel.calculateFromGeometry(routePoints)
    }

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
                        Text(
                            "Xalapa, Veracruz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (isAuthenticated) {
                        IconButton(
                            onClick = {
                                if (isFavorito) favoritosViewModel.removeFromFavorites(routeId)
                                else favoritosViewModel.addToFavorites(routeId)
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (isFavorito) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                val formattedId = routeId.padStart(3, '0')
                val imagePath   = "files/images/routes/Xalapa/bus_$formattedId.jpg"
                var imageBitmap by remember(imagePath) { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(imagePath) {
                    if (routeId.isNotEmpty()) {
                        try {
                            val bytes = Res.readBytes(imagePath)
                            imageBitmap = bytes.decodeToImageBitmap()
                        } catch (_: Exception) { imageBitmap = null }
                    } else { imageBitmap = null }
                }

                Card(
                    modifier  = Modifier.fillMaxWidth().height(160.dp),
                    shape     = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = "Foto del autobus $formattedId",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                    Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(40.dp))
                                    Spacer(Modifier.height(4.dp))
                                    Text("Ruta $formattedId", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Costos de Tarifa", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            FarePriceItem("General",     selectedRoute?.fare        ?: "12.00")
                            FarePriceItem("Estudiantes", selectedRoute?.fareStudent ?: "7.00")
                            FarePriceItem("INAPAN",      selectedRoute?.fareInapan  ?: "7.00")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                RouteTravelTimeCard(state = routeTimeState)
                Spacer(Modifier.height(8.dp))

                InfoItem(
                    icon  = Icons.Default.Timer,
                    label = "Frecuencia Estimada",
                    value = selectedRoute?.frequency?.let { if (it.isEmpty()) "N/A" else it } ?: "Consultando..."
                )

                Spacer(Modifier.height(16.dp))

                if (isAuthenticated && routeId.isNotEmpty()) {
                    OutlinedButton(onClick = { showRatingDialog = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Calificar esta ruta")
                    }
                    Spacer(Modifier.height(8.dp))
                }

                OutlinedButton(
                    onClick = { userLocation?.let { (lat, lng) -> reportsViewModel.submitStopHere(parsedRouteId, lat, lng) } },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = stopState !is com.example.xalabus.ui.viewmodel.StopUiState.Loading && userLocation != null && parsedRouteId > 0 && isAuthenticated
                ) {
                    if (stopState is com.example.xalabus.ui.viewmodel.StopUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.AddLocation, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Aqui hay una parada")
                }

                when {
                    !isAuthenticated -> Text("Inicia sesion para reportar una parada.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    userLocation == null -> Text("Activa el GPS para reportar una parada en tu ubicacion.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                    stopState is com.example.xalabus.ui.viewmodel.StopUiState.Error -> Text((stopState as com.example.xalabus.ui.viewmodel.StopUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                    stopState is com.example.xalabus.ui.viewmodel.StopUiState.Success -> Text((stopState as com.example.xalabus.ui.viewmodel.StopUiState.Success).message, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }

                Spacer(Modifier.height(24.dp))
                Text("Reportar cambios en la ruta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = routeReportMessage,
                    onValueChange = { routeReportMessage = it },
                    placeholder = { Text("Escribe aqui si la ruta cambio...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        cursorColor = MaterialTheme.colorScheme.primary,
                    )
                )

                if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Error) Text((reportState as com.example.xalabus.ui.viewmodel.ReportUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Success) Text("Reporte de ruta enviado!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)

                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            reportsViewModel.submitRouteReport(parsedRouteId, routeReportMessage)
                            if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Success) {
                                routeReportMessage = ""
                                reportsViewModel.resetState()
                            }
                        },
                        enabled = routeReportMessage.isNotBlank() && reportState !is com.example.xalabus.ui.viewmodel.ReportUiState.Loading,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Enviar")
                        }
                    }
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            MapScreen(
                fileManager        = fileManager,
                viewModel          = viewModel,
                isDarkMode         = isDarkMode,
                routeTimeViewModel = routeTimeViewModel,
                incidentViewModel  = incidentViewModel,
                onUserLocationChanged = { lat, lng -> viewModel.updateUserLocation(lat, lng) }
            )
            FilledIconButton(
                onClick  = onBack,
                modifier = Modifier.padding(16.dp).size(48.dp).align(Alignment.TopStart),
                colors   = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            ) {
                Icon(Icons.Default.ArrowBack, "Regresar", tint = MaterialTheme.colorScheme.onSurface)
            }
        }
    }

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

@Composable
fun RouteTravelTimeCard(state: RouteTimeUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DirectionsBus, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Duracion del trayecto", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Recorrido completo de la ruta", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
            }
            when (state) {
                is RouteTimeUiState.Idle -> CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.primary)
                is RouteTimeUiState.Result -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("~${state.formattedTime}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("${state.distanceKm} km · 32 km/h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
fun FarePriceItem(label: String, price: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "$$price", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoItem(icon: ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
    }
}
