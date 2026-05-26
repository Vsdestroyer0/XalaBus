package com.example.xalabus.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xalabus.data.paradas.Parada
import kotlinx.coroutines.launch

private val AdminBg      = Color(0xFF080C14)
private val AdminText    = Color(0xFFE2E8F0)
private val AdminAccent  = Color(0xFF6366F1)
private val AdminMuted   = Color(0xFF64748B)
private val AdminCardBg  = Color(0xFF1E293B)
private val ApproveGreen = Color(0xFF10B981)
private val RejectRed    = Color(0xFFEF4444)

private enum class AdminScreenDestination { DASHBOARD, STOPS }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit
) {
    val stopsViewModel = remember { AdminStopsViewModel() }
    var currentScreen by remember { mutableStateOf(AdminScreenDestination.DASHBOARD) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = AdminCardBg,
                drawerContentColor   = AdminText
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Admin XalaBus",
                    modifier   = Modifier.padding(16.dp),
                    style      = MaterialTheme.typography.titleLarge,
                    color      = AdminText,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = AdminMuted.copy(alpha = 0.3f))
                Spacer(Modifier.height(8.dp))

                NavigationDrawerItem(
                    icon     = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label    = { Text("Inicio") },
                    selected = currentScreen == AdminScreenDestination.DASHBOARD,
                    onClick  = {
                        currentScreen = AdminScreenDestination.DASHBOARD
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor   = AdminAccent.copy(alpha = 0.2f),
                        selectedTextColor        = AdminAccent,
                        selectedIconColor        = AdminAccent,
                        unselectedTextColor      = AdminText,
                        unselectedIconColor      = AdminMuted
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon     = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                    label    = { Text("Paradas Sugeridas") },
                    selected = currentScreen == AdminScreenDestination.STOPS,
                    onClick  = {
                        currentScreen = AdminScreenDestination.STOPS
                        stopsViewModel.fetchPendingStops()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor   = AdminAccent.copy(alpha = 0.2f),
                        selectedTextColor        = AdminAccent,
                        selectedIconColor        = AdminAccent,
                        unselectedTextColor      = AdminText,
                        unselectedIconColor      = AdminMuted
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = AdminMuted.copy(alpha = 0.3f))

                NavigationDrawerItem(
                    icon     = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label    = { Text("Cerrar Sesi\u00f3n") },
                    selected = false,
                    onClick  = {
                        scope.launch { drawerState.close() }
                        viewModel.signOut()
                        onSignOut()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = RejectRed,
                        unselectedIconColor = RejectRed
                    ),
                    modifier = Modifier
                        .padding(NavigationDrawerItemDefaults.ItemPadding)
                        .padding(bottom = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = if (currentScreen == AdminScreenDestination.DASHBOARD)
                                "Dashboard" else "Sugerencias de Paradas",
                            color      = AdminText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Men\u00fa", tint = AdminText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
                )
            },
            containerColor = AdminBg
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (currentScreen) {
                    AdminScreenDestination.DASHBOARD -> DashboardContent()
                    AdminScreenDestination.STOPS     -> AdminStopsContent(stopsViewModel)
                }
            }
        }
    }
}

@Composable
fun DashboardContent() {
    Column(
        modifier              = Modifier.fillMaxSize(),
        horizontalAlignment   = Alignment.CenterHorizontally,
        verticalArrangement   = Arrangement.Center
    ) {
        Icon(
            Icons.Default.AdminPanelSettings,
            contentDescription = null,
            tint     = AdminAccent,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Bienvenido al Panel de Control",
            color      = AdminText,
            fontSize   = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Selecciona una opci\u00f3n del men\u00fa lateral.",
            color    = AdminMuted,
            fontSize = 14.sp
        )
    }
}

@Composable
fun AdminStopsContent(viewModel: AdminStopsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val s = uiState) {
        is AdminStopsUiState.Loading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AdminAccent)
            }
        }
        is AdminStopsUiState.Error -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text     = s.message,
                    color    = RejectRed,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
        is AdminStopsUiState.Success -> {
            val stops = s.stops
            if (stops.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No hay paradas pendientes por revisar.", color = AdminMuted)
                }
            } else {
                LazyColumn(
                    contentPadding      = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(stops) { stop ->
                        StopSuggestionCard(
                            stop       = stop,
                            onApprove  = { viewModel.approveStop(stop) },
                            onReject   = { viewModel.rejectStop(stop) }
                        )
                    }
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun StopSuggestionCard(
    stop: Parada,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors    = CardDefaults.cardColors(containerColor = AdminCardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        modifier  = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                stop.nombre,
                color      = AdminText,
                fontWeight = FontWeight.Bold,
                fontSize   = 16.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Ruta: ${stop.rutaId}",
                color    = AdminAccent,
                fontSize = 13.sp
            )
            Text(
                "Coordenadas: ${stop.latitud}, ${stop.longitud}",
                color    = AdminMuted,
                fontSize = 12.sp
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onReject,
                    colors  = ButtonDefaults.textButtonColors(contentColor = RejectRed)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rechazar")
                }

                Spacer(Modifier.width(8.dp))

                Button(
                    onClick = onApprove,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = ApproveGreen,
                        contentColor   = Color.White
                    )
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aprobar")
                }
            }
        }
    }
}
