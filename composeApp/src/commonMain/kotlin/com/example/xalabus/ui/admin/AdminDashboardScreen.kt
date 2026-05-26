package com.example.xalabus.ui.admin

import androidx.compose.foundation.background
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
import kotlinx.coroutines.launch

private val AdminBg     = Color(0xFF080C14)
private val AdminText   = Color(0xFFE2E8F0)
private val AdminAccent = Color(0xFF6366F1)
private val AdminMuted  = Color(0xFF64748B)
private val AdminCardBg = Color(0xFF1E293B)
private val ApproveGreen = Color(0xFF10B981)
private val RejectRed   = Color(0xFFEF4444)

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
                drawerContentColor = AdminText
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Admin XalaBus",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge,
                    color = AdminText,
                    fontWeight = FontWeight.Bold
                )
                HorizontalDivider(color = AdminMuted.copy(alpha = 0.3f))
                
                Spacer(Modifier.height(8.dp))
                
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
                    label = { Text("Inicio") },
                    selected = currentScreen == AdminScreenDestination.DASHBOARD,
                    onClick = {
                        currentScreen = AdminScreenDestination.DASHBOARD
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = AdminAccent.copy(alpha = 0.2f),
                        selectedTextColor = AdminAccent,
                        selectedIconColor = AdminAccent,
                        unselectedTextColor = AdminText,
                        unselectedIconColor = AdminMuted
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.AddLocationAlt, contentDescription = null) },
                    label = { Text("Paradas Sugeridas") },
                    selected = currentScreen == AdminScreenDestination.STOPS,
                    onClick = {
                        currentScreen = AdminScreenDestination.STOPS
                        stopsViewModel.fetchPendingStops()
                        scope.launch { drawerState.close() }
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color.Transparent,
                        selectedContainerColor = AdminAccent.copy(alpha = 0.2f),
                        selectedTextColor = AdminAccent,
                        selectedIconColor = AdminAccent,
                        unselectedTextColor = AdminText,
                        unselectedIconColor = AdminMuted
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = AdminMuted.copy(alpha = 0.3f))
                NavigationDrawerItem(
                    icon = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick = {
                        scope.launch { drawerState.close() }
                        viewModel.signOut()
                        onSignOut()
                    },
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedTextColor = RejectRed,
                        unselectedIconColor = RejectRed
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding).padding(bottom = 16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Text(
                            text = if (currentScreen == AdminScreenDestination.DASHBOARD) "Dashboard" else "Sugerencias de Paradas",
                            color = AdminText,
                            fontWeight = FontWeight.Bold
                        ) 
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = AdminText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
                )
            },
            containerColor = AdminBg
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize().padding(padding)) {
                when (currentScreen) {
                    AdminScreenDestination.DASHBOARD -> AdminMainDashboard(
                        viewModel = viewModel,
                        adminBg = AdminBg,
                        adminText = AdminText,
                        adminAccent = AdminAccent,
                        adminMuted = AdminMuted,
                        adminCardBg = AdminCardBg
                    )
                    AdminScreenDestination.STOPS -> AdminStopsList(
                        viewModel = stopsViewModel,
                        adminBg = AdminBg,
                        adminText = AdminText,
                        adminAccent = AdminAccent,
                        adminMuted = AdminMuted,
                        adminCardBg = AdminCardBg,
                        approveGreen = ApproveGreen,
                        rejectRed = RejectRed
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminMainDashboard(
    viewModel: AdminViewModel,
    adminBg: Color,
    adminText: Color,
    adminAccent: Color,
    adminMuted: Color,
    adminCardBg: Color
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adminBg)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "Panel de Administración",
            color = adminText,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Gestiona el sistema XalaBus",
            color = adminMuted,
            fontSize = 14.sp
        )

        when (val s = uiState) {
            is AdminUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = adminAccent)
                }
            }
            is AdminUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        s.message,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFFF8A80)
                    )
                }
            }
            else -> {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = adminCardBg)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Sistema activo", color = adminText, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text("Usa el menú lateral para navegar entre secciones.", color = adminMuted, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminStopsList(
    viewModel: AdminStopsViewModel,
    adminBg: Color,
    adminText: Color,
    adminAccent: Color,
    adminMuted: Color,
    adminCardBg: Color,
    approveGreen: Color,
    rejectRed: Color
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(adminBg)
            .padding(16.dp)
    ) {
        when (val s = uiState) {
            is AdminStopsUiState.Loading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = adminAccent)
                }
            }
            is AdminStopsUiState.Error -> {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3B0000)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        s.message,
                        modifier = Modifier.padding(12.dp),
                        color = Color(0xFFFF8A80)
                    )
                }
            }
            is AdminStopsUiState.Success -> {
                if (s.stops.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = approveGreen,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("No hay sugerencias pendientes", color = adminMuted)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(s.stops) { stop ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = adminCardBg)
                            ) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(stop.nombre, color = adminText, fontWeight = FontWeight.Bold)
                                    Text(
                                        "Lat: ${stop.latitud}  Lon: ${stop.longitud}",
                                        color = adminMuted,
                                        fontSize = 12.sp
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Button(
                                            onClick = { viewModel.approveStop(stop) },
                                            colors = ButtonDefaults.buttonColors(containerColor = approveGreen),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Aprobar", fontSize = 12.sp) }
                                        OutlinedButton(
                                            onClick = { viewModel.rejectStop(stop) },
                                            border = androidx.compose.foundation.BorderStroke(1.dp, rejectRed),
                                            modifier = Modifier.weight(1f)
                                        ) { Text("Rechazar", color = rejectRed, fontSize = 12.sp) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            else -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Cargando sugerencias...", color = adminMuted)
                }
            }
        }
    }
}
