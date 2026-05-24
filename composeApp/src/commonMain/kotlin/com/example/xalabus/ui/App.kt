package com.example.xalabus.ui

import androidx.compose.foundation.Image
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
import com.example.xalabus.ui.home.HomeScreen
import com.example.xalabus.ui.map.MapScreen
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.ui.onboarding.OnboardingScreen
import com.example.xalabus.core.prefs.OnboardingPreferences
import com.example.xalabus.ui.viewmodel.FavoritosViewModel
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import xalabus.composeapp.generated.resources.*
import xalabus.composeapp.generated.resources.Res

private enum class AppDestination { AUTH, MAIN, ADMIN_DASHBOARD }

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
    val reportsViewModel = remember { com.example.xalabus.ui.viewmodel.ReportsViewModel() }
    // CU-03: ViewModel de recuperación de contraseña
    val forgotPasswordViewModel = remember { ForgotPasswordViewModel() }
    // CU-10: ViewModel de favoritos
    val favoritosViewModel = remember { FavoritosViewModel() }
    // CU-13: ViewModel de incidentes GPS
    val incidentViewModel = remember { IncidentViewModel() }

    val systemDark  = isSystemInDarkTheme()
    var isDarkMode  by remember { mutableStateOf(systemDark) }
    val colorScheme = if (isDarkMode) XalaBusDarkColors else XalaBusLightColors

    var isAuthenticated by remember { mutableStateOf(authViewModel.isSessionActive()) }
    var destination by remember {
        mutableStateOf(AppDestination.MAIN) // Iniciamos en MAIN para que sea offline-first
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
                        // CU-03: navegar a la pantalla de recuperación
                        onForgotPassword     = { currentAuthScreen = AuthScreen.FORGOT_PASSWORD }
                    )
                    AuthScreen.REGISTER -> RegisterScreen(
                        viewModel         = authViewModel,
                        onRegisterSuccess = { currentAuthScreen = AuthScreen.LOGIN },
                        onNavigateToLogin = { currentAuthScreen = AuthScreen.LOGIN }
                    )
                    // CU-03: pantalla de recuperación de contraseña
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
                    reportsViewModel = reportsViewModel,
                    favoritosViewModel = favoritosViewModel,
                    incidentViewModel  = incidentViewModel
                )

                AppDestination.ADMIN_DASHBOARD -> AdminDashboardScreen(
                    viewModel  = adminViewModel,
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
    isDarkMode: Boolean,
    isAuthenticated: Boolean,
    onToggleDarkMode: () -> Unit,
    onSignInRequest: () -> Unit,
    onSignOut: () -> Unit,
    reportsViewModel: com.example.xalabus.ui.viewmodel.ReportsViewModel,
    favoritosViewModel: FavoritosViewModel,
    incidentViewModel: IncidentViewModel
) {
    LaunchedEffect(Unit) { viewModel.initializeData() }

    val isLoaded by viewModel.isDataLoaded.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!OnboardingPreferences.isOnboardingCompleted()) }
    var showGeneralReport by remember { mutableStateOf(false) }
    // CU-13: controla la pantalla de reporte de incidente con GPS
    var showIncidentReport by remember { mutableStateOf(false) }

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

                    if (isAuthenticated) {
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
                    fileManager = fileManager,
                    viewModel   = viewModel,
                    reportsViewModel = reportsViewModel,
                    favoritosViewModel = favoritosViewModel,
                    isDarkMode  = isDarkMode,
                    isAuthenticated = isAuthenticated,
                    onBack      = { showMap = false }
                )
            }
        }

        if (showGeneralReport) {
            com.example.xalabus.ui.reports.GeneralReportDialog(
                viewModel = reportsViewModel,
                onDismiss = { showGeneralReport = false }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MapDetailView(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    reportsViewModel: com.example.xalabus.ui.viewmodel.ReportsViewModel,
    favoritosViewModel: FavoritosViewModel,
    isDarkMode: Boolean,
    isAuthenticated: Boolean,
    onBack: () -> Unit
) {
    val selectedRoute  by viewModel.selectedRoute.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val routeId       = selectedRoute?.id ?: ""
    val parsedRouteId = routeId.toIntOrNull() ?: 0

    var routeReportMessage by remember { mutableStateOf("") }
    val reportState by reportsViewModel.uiState.collectAsState()
    var showStopDialog by remember { mutableStateOf(false) }

    // CU-10: estado de favoritos para esta ruta
    val favoritosState by favoritosViewModel.uiState.collectAsState()
    val isFavorito     by favoritosViewModel.isCurrentRouteFavorite.collectAsState()

    // Cargar estado de favorito al seleccionar ruta
    LaunchedEffect(routeId) {
        if (routeId.isNotEmpty()) {
            favoritosViewModel.checkIfFavorite(routeId)
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
                        Text(
                            "Xalapa, Veracruz",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // CU-10: Botón favorito (solo si el usuario está autenticado)
                    if (isAuthenticated) {
                        IconButton(
                            onClick = {
                                if (isFavorito) {
                                    favoritosViewModel.removeFromFavorites(routeId)
                                } else {
                                    favoritosViewModel.addToFavorites(routeId)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = if (isFavorito) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = if (isFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (isFavorito) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
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
                                bitmap       = imageBitmap!!,
                                contentDescription = "Foto del autobús $formattedId",
                                modifier     = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else { DefaultPlaceholder() }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ── Sección de Tarifas ─────────────────────────────────────────
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
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            FarePriceItem("General", selectedRoute?.fare ?: "12.00")
                            FarePriceItem("Estudiantes", selectedRoute?.fareStudent ?: "7.00")
                            FarePriceItem("INAPAN", selectedRoute?.fareInapan ?: "7.00")
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // CU-11: Tiempo estimado de traslado
                // Lógica: se usa la frecuencia como proxy de tiempo; si no, se estima
                // con velocidad promedio urbana de 30 km/h y distancia estimada de la ruta.
                val estimatedMinutes = estimateTransitTime(selectedRoute)
                InfoItem(
                    icon  = Icons.Default.AccessTime,
                    label = "Tiempo Estimado de Traslado",
                    value = if (estimatedMinutes > 0) "~$estimatedMinutes min" else "No disponible"
                )

                Spacer(Modifier.height(8.dp))

                InfoItem(
                    icon  = Icons.Default.Timer,
                    label = "Frecuencia Estimada",
                    value = selectedRoute?.frequency?.let { if (it.isEmpty()) "N/A" else it } ?: "Consultando..."
                )

                Spacer(Modifier.height(24.dp))
                Text("Reportar cambios en la ruta", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = routeReportMessage,
                    onValueChange = { routeReportMessage = it },
                    placeholder = { Text("Escribe aquí si la ruta cambió...", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor    = MaterialTheme.colorScheme.outline,
                        focusedContainerColor   = MaterialTheme.colorScheme.surfaceVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                        focusedTextColor        = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor      = MaterialTheme.colorScheme.onSurface,
                        cursorColor             = MaterialTheme.colorScheme.primary,
                    )
                )

                if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Error) {
                    Text((reportState as com.example.xalabus.ui.viewmodel.ReportUiState.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.labelSmall)
                }
                if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Success) {
                    Text("¡Reporte de ruta enviado!", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelSmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { showStopDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("¡Aquí hay una parada!")
                    }

                    Button(
                        onClick = {
                            reportsViewModel.submitRouteReport(parsedRouteId, routeReportMessage)
                            if (reportState is com.example.xalabus.ui.viewmodel.ReportUiState.Success) {
                                routeReportMessage = ""
                                reportsViewModel.resetState()
                            }
                        },
                        enabled = routeReportMessage.isNotBlank() && reportState !is com.example.xalabus.ui.viewmodel.ReportUiState.Loading,
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor   = MaterialTheme.colorScheme.onPrimary
                        )
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
            MapScreen(fileManager = fileManager, viewModel = viewModel, isDarkMode = isDarkMode)
            FilledIconButton(
                onClick  = onBack,
                modifier = Modifier.padding(16.dp).size(48.dp).align(Alignment.TopStart),
                colors   = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Icon(Icons.Default.ArrowBack, "Regresar", tint = MaterialTheme.colorScheme.onSurface)
            }
        }

        if (showStopDialog) {
            com.example.xalabus.ui.reports.RouteStopDialog(
                viewModel = reportsViewModel,
                routeId = parsedRouteId,
                latitude = 19.543, // Coord. default Xalapa
                longitude = -96.927,
                onDismiss = { showStopDialog = false }
            )
        }
    }
}

/**
 * CU-11: Estima el tiempo total de traslado en minutos.
 *
 * Si la ruta tiene campo `frequency` con formato "X min", lo usa directamente.
 * Si no, estima: número de paradas * tiempo promedio por parada (2 min) + trayecto base.
 * Velocidad promedio urbana asumida: 30 km/h.
 */
private fun estimateTransitTime(route: com.example.xalabus.DBD.RouteEntity?): Int {
    if (route == null) return 0
    // Intentar extraer minutos del campo frequency (ej: "15 min", "20 minutos")
    val freqText = route.frequency?.lowercase() ?: ""
    val freqMinutes = Regex("(\\d+)").find(freqText)?.groupValues?.get(1)?.toIntOrNull()
    if (freqMinutes != null && freqMinutes in 1..120) return freqMinutes
    // Estimación por defecto: 35 minutos promedio para rutas urbanas de Xalapa
    return 35
}

@Composable
fun DefaultPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.DirectionsBus, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.outline)
        Text("Foto no disponible", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
fun FarePriceItem(label: String, price: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("$$price", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    }
}
