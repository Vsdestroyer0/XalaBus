package com.example.xalabus.ui.admin

import androidx.compose.foundation.layout.*
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
private val RejectRed   = Color(0xFFEF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit
) {
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
                    selected = true,
                    onClick  = { scope.launch { drawerState.close() } },
                    colors   = NavigationDrawerItemDefaults.colors(
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
                            text       = "Dashboard",
                            color      = AdminText,
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
            Box(
                modifier            = Modifier.fillMaxSize().padding(padding),
                contentAlignment    = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
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
                        "Selecciona una opción del menú lateral.",
                        color    = AdminMuted,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
