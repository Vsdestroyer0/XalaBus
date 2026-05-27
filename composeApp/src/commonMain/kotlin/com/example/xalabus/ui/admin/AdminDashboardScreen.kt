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
import com.example.xalabus.data.reports.ReportsRepository
import com.example.xalabus.data.reports.RouteStop
import com.example.xalabus.data.repository.RouteRepository
import kotlinx.coroutines.launch

private val AdminBg     = Color(0xFF080C14)
private val AdminText   = Color(0xFFE2E8F0)
private val AdminAccent = Color(0xFF6366F1)
private val AdminMuted  = Color(0xFF64748B)
private val AdminCardBg = Color(0xFF1E293B)
private val RejectRed   = Color(0xFFEF4444)
private val ApproveGreen = Color(0xFF22C55E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    reportsRepository: ReportsRepository,
    routeRepository: RouteRepository,
    onSignOut: () -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    var pendingStops by remember { mutableStateOf<List<RouteStop>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var actionMessage by remember { mutableStateOf<String?>(null) }

    fun loadPending() {
        scope.launch {
            isLoading = true
            try {
                pendingStops = reportsRepository.getPendingStops()
            } catch (e: Exception) {
                actionMessage = "No se pudieron cargar las paradas: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadPending() }

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
                    icon     = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                    label    = { Text("Paradas pendientes") },
                    selected = true,
                    onClick  = { scope.launch { drawerState.close() } },
                    colors   = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = AdminAccent.copy(alpha = 0.2f),
                        selectedTextColor      = AdminAccent,
                        selectedIconColor      = AdminAccent,
                        unselectedTextColor    = AdminText,
                        unselectedIconColor    = AdminMuted
                    ),
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                Spacer(Modifier.weight(1f))
                HorizontalDivider(color = AdminMuted.copy(alpha = 0.3f))

                NavigationDrawerItem(
                    icon     = { Icon(Icons.Default.Logout, contentDescription = null) },
                    label    = { Text("Cerrar Sesión") },
                    selected = false,
                    onClick  = {
                        scope.launch { drawerState.close() }
                        viewModel.signOut()
                        onSignOut()
                    },
                    colors   = NavigationDrawerItemDefaults.colors(
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
                            text       = "Paradas pendientes",
                            color      = AdminText,
                            fontWeight = FontWeight.Bold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menú", tint = AdminText)
                        }
                    },
                    actions = {
                        IconButton(onClick = { loadPending() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = AdminText)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = AdminBg)
                )
            },
            containerColor = AdminBg
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                actionMessage?.let { msg ->
                    Text(msg, color = AdminMuted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }

                if (isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AdminAccent)
                    }
                } else if (pendingStops.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No hay paradas pendientes de revisión.", color = AdminMuted)
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(pendingStops, key = { it.id ?: it.hashCode().toString() }) { stop ->
                            PendingStopCard(
                                stop = stop,
                                onApprove = {
                                    val id = stop.id ?: return@PendingStopCard
                                    scope.launch {
                                        try {
                                            reportsRepository.updateStopStatus(
                                                id, "accepted", stop.description
                                            )
                                            routeRepository.insertLocalStop(
                                                stop.copy(status = "accepted")
                                            )
                                            actionMessage = "Parada aprobada y guardada localmente."
                                            loadPending()
                                        } catch (e: Exception) {
                                            actionMessage = "Error al aprobar: ${e.message}"
                                        }
                                    }
                                },
                                onReject = {
                                    val id = stop.id ?: return@PendingStopCard
                                    scope.launch {
                                        try {
                                            reportsRepository.updateStopStatus(
                                                id, "rejected", stop.description
                                            )
                                            actionMessage = "Parada rechazada."
                                            loadPending()
                                        } catch (e: Exception) {
                                            actionMessage = "Error al rechazar: ${e.message}"
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PendingStopCard(
    stop: RouteStop,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AdminCardBg),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Ruta ${stop.routeId}",
                color = AdminAccent,
                fontWeight = FontWeight.Bold
            )
            Text(stop.description, color = AdminText, modifier = Modifier.padding(vertical = 4.dp))
            Text(
                "Lat: ${"%.5f".format(stop.latitude)}, Lng: ${"%.5f".format(stop.longitude)}",
                color = AdminMuted,
                fontSize = 12.sp
            )
            Text("Popularidad: ${stop.popularity}", color = AdminMuted, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onApprove,
                    colors = ButtonDefaults.buttonColors(containerColor = ApproveGreen),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Check, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Aprobar")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = RejectRed)
                ) {
                    Icon(Icons.Default.Close, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Rechazar")
                }
            }
        }
    }
}
