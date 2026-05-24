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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.decodeToImageBitmap
import xalabus.composeapp.generated.resources.*
import xalabus.composeapp.generated.resources.Res

private enum class AppDestination { AUTH, MAIN, ADMIN_DASHBOARD }
// CU-03: Se agrega FORGOT_PASSWORD al flujo de autenticación
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
    // CU-03: ViewModel dedicado para recuperación de contraseña
    val forgotPasswordViewModel = remember { ForgotPasswordViewModel() }

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
                        onForgotPassword     = { currentAuthScreen = AuthScreen.FORGOT_PASSWORD },
                        onBack               = { destination = AppDestination.MAIN }
                    )
                    AuthScreen.REGISTER -> RegisterScreen(
                        viewModel         = authViewModel,
                        onRegisterSuccess = { currentAuthScreen = AuthScreen.LOGIN },
                        onNavigateToLogin = { currentAuthScreen = AuthScreen.LOGIN }
                    )
                    // CU-03: Pantalla de recuperación de contraseña
                    AuthScreen.FORGOT_PASSWORD -> ForgotPasswordScreen(
                        viewModel = forgotPasswordViewModel,
                        onBack    = { currentAuthScreen = AuthScreen.LOGIN },
                        onSuccess = { currentAuthScreen = AuthScreen.LOGIN }
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
                    reportsViewModel = reportsViewModel
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
    reportsViewModel: com.example.xalabus.ui.viewmodel.ReportsViewModel
) {
    LaunchedEffect(Unit) { viewModel.initializeData() }

    val isLoaded by viewModel.isDataLoaded.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var showOnboarding by remember { mutableStateOf(!OnboardingPreferences.isOnboardingCompleted()) }
    var showGeneralReport by remember { mutableStateOf(false) }

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
                    isDarkMode  = isDarkMode,
                    onBack      = { showMap = false },
                    isAuthenticated = isAuthenticated
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
    // CU-10: estado local del botón de favorito
    var showFavoritosViewModel by remember { mutableStateOf(false) }
    // CU-13: mostrar pantalla de reporte de incidente
    var showReportIncident by remember { mutableStateOf(false) }

    // CU-10: ViewModel de favoritos
    val favoritosViewModel = remember { com.example.xalabus.ui.viewmodel.FavoritosViewModel() }
    val favoritoState by favoritosViewModel.uiState.collectAsState()
    val esFavorito by favoritosViewModel.esFavorito.collectAsState()

    // Cargar estado de favorito al abrir la ruta
    LaunchedEffect(routeId) {
        if (routeId.isNotEmpty()) {
            favoritosViewModel.verificarFavorito(routeId)
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
                // ── Cabecera de la ruta con botón favorito (CU-10) ────────────────
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
                    // CU-10: Botón de favoritos (solo para usuarios autenticados)
                    if (isAuthenticated) {
                        IconButton(
                            onClick = {
                                favoritosViewModel.toggleFavorito(routeId)
                            }
                        ) {
                            Icon(
                                imageVector = if (esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (esFavorito) "Quitar de favoritos" else "Agregar a favoritos",
                                tint = if (esFavorito) Color(0xFFF5C518) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Feedback de estado favorito
                when (favoritoState) {
                    is com.example.xalabus.ui.viewmodel.FavoritosUiState.Error -> {
                        Text(
                            (favoritoState as com.example.xalabus.ui.viewmodel.FavoritosUiState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    is com.example.xalabus.ui.viewmodel.FavoritosUiState.Success -> {
                        Text(
                            if (esFavorito) "⭐ Agregado a favoritos" else "Removido de favoritos",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    else -> Unit
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

                // ── Tarifas ────────────────────────────────────────────────────
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
                InfoItem(
                    icon  = Icons.Default.Timer,
                    label = "Tiempo Estimado de Traslado",
                    value = calcularTiempoEstimado(selectedRoute?.frequency)
                )

                Spacer(Modifier.height(8.dp))

                // Frecuencia
                InfoItem(
                    icon  = Icons.Default.DirectionsBus,
                    label = "Frecuencia de Salida",
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
                    // Botón sugerir parada (existente)
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

                // CU-13: Botón para reportar incidente en el mapa
                if (isAuthenticated) {
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = { showReportIncident = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Warning, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Reportar incidente en el mapa")
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
                latitude = 19.543,
                longitude = -96.927,
                onDismiss = { showStopDialog = false }
            )
        }

        // CU-13: Diálogo de reporte de incidente
        if (showReportIncident) {
            com.example.xalabus.ui.reports.ReportIncidentDialog(
                onDismiss = { showReportIncident = false }
            )
        }
    }
}

/**
 * CU-11: Calcula el tiempo estimado de traslado.
 * Si la frecuencia indica el tiempo de recorrido (ej. "45 min"), lo usa directamente.
 * Si no hay datos, devuelve un valor genérico basado en velocidad promedio urbana de 30 km/h.
 */
private fun calcularTiempoEstimado(frequency: String?): String {
    if (frequency.isNullOrBlank()) return "~30–45 min (estimado)"
    // Si la frecuencia ya contiene "min", puede representar duración del recorrido
    val cleaned = frequency.trim().lowercase()
    if (cleaned.contains("min")) return frequency
    // Formato numérico simple (ej. "15" = frecuencia en minutos, no duración)
    // En ese caso estimamos la duración promedio de ruta urbana en Xalapa
    return "~30–45 min (estimado)"
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
