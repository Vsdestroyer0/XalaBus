package com.example.xalabus.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.XalaContext
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.core.util.MapFileManager
import com.example.xalabus.ui.auth.AuthUiState
import com.example.xalabus.ui.auth.AuthViewModel
import com.example.xalabus.ui.auth.LoginScreen
import com.example.xalabus.ui.auth.RegisterScreen
import com.example.xalabus.ui.home.HomeScreen
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.ui.map.MapScreen
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import xalabus.composeapp.generated.resources.*
import xalabus.composeapp.generated.resources.Res
import androidx.compose.ui.graphics.ImageBitmap
import org.jetbrains.compose.resources.decodeToImageBitmap

/** Pantalla actual de autenticación */
private enum class AuthScreen { LOGIN, REGISTER }

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cargando XalaBus...")
        }
    }
}

@Composable
fun App(
    driverFactory: DriverFactory,
    fileManager: MapFileManager
) {
    val repository = remember { XalaContext.getRepository(driverFactory) }
    val viewModel = remember { RouteViewModel(repository, fileManager) }
    val authViewModel = remember { AuthViewModel() }

    val systemDark = isSystemInDarkTheme()
    var isDarkMode by remember { mutableStateOf(systemDark) }
    val colorScheme = if (isDarkMode) darkColorScheme() else lightColorScheme()

    // Determinar si el usuario ya tiene sesión activa
    var isAuthenticated by remember { mutableStateOf(authViewModel.isSessionActive()) }
    var currentAuthScreen by remember { mutableStateOf(AuthScreen.LOGIN) }

    MaterialTheme(colorScheme = colorScheme) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {

            if (!isAuthenticated) {
                // ── Flujo de autenticación ──────────────────────────────────
                when (currentAuthScreen) {
                    AuthScreen.LOGIN -> LoginScreen(
                        viewModel = authViewModel,
                        onLoginSuccess = { isAuthenticated = true },
                        onNavigateToRegister = { currentAuthScreen = AuthScreen.REGISTER }
                    )
                    AuthScreen.REGISTER -> RegisterScreen(
                        viewModel = authViewModel,
                        onRegisterSuccess = { /* El registro pide confirmación de email */ },
                        onNavigateToLogin = { currentAuthScreen = AuthScreen.LOGIN }
                    )
                }
            } else {
                // ── App principal ───────────────────────────────────────────
                MainAppContent(
                    driverFactory = driverFactory,
                    fileManager = fileManager,
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onToggleDarkMode = { isDarkMode = !isDarkMode },
                    onSignOut = {
                        authViewModel.signOut()
                        isAuthenticated = false
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
    onToggleDarkMode: () -> Unit,
    onSignOut: () -> Unit
) {
    LaunchedEffect(Unit) {
        viewModel.initializeData()
    }

    val isLoaded by viewModel.isDataLoaded.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    if (!isLoaded) {
        LoadingScreen()
    } else {
        ModalNavigationDrawer(
            drawerState = drawerState,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Configuraciones",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleLarge
                    )
                    HorizontalDivider()

                    NavigationDrawerItem(
                        icon = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null) },
                        label = { Text("Modo Oscuro") },
                        selected = false,
                        onClick = onToggleDarkMode,
                        badge = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { onToggleDarkMode() }
                            )
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Botón Cerrar Sesión
                    NavigationDrawerItem(
                        icon = { Icon(Icons.Default.Logout, null) },
                        label = { Text("Cerrar sesión") },
                        selected = false,
                        onClick = {
                            scope.launch { drawerState.close() }
                            onSignOut()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        ) {
            if (!showMap) {
                HomeScreen(
                    viewModel = viewModel,
                    onOpenDrawer = { scope.launch { drawerState.open() } },
                    onRouteClick = { routeId ->
                        viewModel.selectRoute(routeId)
                        showMap = true
                    }
                )
            } else {
                MapDetailView(
                    fileManager = fileManager,
                    viewModel = viewModel,
                    isDarkMode = isDarkMode,
                    onBack = { showMap = false }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun MapDetailView(
    fileManager: MapFileManager,
    viewModel: RouteViewModel,
    isDarkMode: Boolean,
    onBack: () -> Unit
) {
    val selectedRoute by viewModel.selectedRoute.collectAsState()
    val scaffoldState = rememberBottomSheetScaffoldState()

    val routeId = selectedRoute?.id ?: ""

    BottomSheetScaffold(
        scaffoldState = scaffoldState,
        sheetPeekHeight = 200.dp,
        sheetContainerColor = MaterialTheme.colorScheme.surface,
        sheetContent = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = selectedRoute?.name ?: "Ruta Desconocida",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text("Xalapa, Veracruz", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)

                Spacer(Modifier.height(16.dp))

                val formattedId = routeId.padStart(3, '0')
                val imagePath = "drawable/bus_$formattedId.jpg"

                var imageBitmap by remember(imagePath) { mutableStateOf<ImageBitmap?>(null) }

                LaunchedEffect(imagePath) {
                    if (routeId.isNotEmpty()) {
                        try {
                            val bytes = Res.readBytes(imagePath)
                            imageBitmap = bytes.decodeToImageBitmap()
                        } catch (e: Exception) {
                            imageBitmap = null
                        }
                    } else {
                        imageBitmap = null
                    }
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                    shape = MaterialTheme.shapes.large,
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap!!,
                                contentDescription = "Foto del autobús $formattedId",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            DefaultPlaceholder()
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    InfoItem(
                        icon = Icons.Default.Payments,
                        label = "Tarifa",
                        value = selectedRoute?.fare?.let { if (it.isEmpty()) "N/A" else "$$it" } ?: "Consultando..."
                    )
                    InfoItem(
                        icon = Icons.Default.Timer,
                        label = "Frecuencia",
                        value = selectedRoute?.frequency?.let { if (it.isEmpty()) "N/A" else it } ?: "Consultando..."
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text("Reportar cambios en la ruta", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Escribe aquí si la ruta cambió...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )
                Button(
                    onClick = { /* Lógica reporte */ },
                    modifier = Modifier.align(Alignment.End).padding(top = 8.dp)
                ) {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar Reporte")
                }
                Spacer(Modifier.height(40.dp))
            }
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding).fillMaxSize()) {
            MapScreen(fileManager = fileManager, viewModel = viewModel, isDarkMode = isDarkMode)
            FilledIconButton(
                onClick = onBack,
                modifier = Modifier.padding(16.dp).size(48.dp).align(Alignment.TopStart),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
            }
        }
    }
}

@Composable
fun DefaultPlaceholder() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(Icons.Default.DirectionsBus, null, Modifier.size(48.dp), tint = Color.LightGray)
        Text("Foto no disponible", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
}

@Composable
fun InfoItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
            Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        }
    }
}
