package com.example.xalabus.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ─── Paleta admin (misma que AdminLoginScreen) ────────────────────────────────
private val AdminBg         = Color(0xFF080C14)
private val AdminSurface    = Color(0xFF0F1624)
private val AdminSurface2   = Color(0xFF161E2E)
private val AdminAccent     = Color(0xFF6366F1)
private val AdminAccentSoft = Color(0xFF818CF8)
private val AdminText       = Color(0xFFE2E8F0)
private val AdminMuted      = Color(0xFF64748B)
private val AdminOutline    = Color(0xFF1E2D45)

// Colores para las tarjetas de estadísticas
private val CardGreen  = Color(0xFF10B981)
private val CardOrange = Color(0xFFF59E0B)
private val CardRed    = Color(0xFFEF4444)
private val CardBlue   = Color(0xFF3B82F6)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // ── Header ────────────────────────────────────────────────────────
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(AdminSurface2, AdminBg)
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 24.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(AdminAccent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AdminPanelSettings,
                                    contentDescription = null,
                                    tint = AdminAccent,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    "Panel de Control",
                                    color = AdminText,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "XalaBus · Administrador",
                                    color = AdminAccentSoft,
                                    fontSize = 12.sp
                                )
                            }
                        }
                        IconButton(
                            onClick = {
                                viewModel.signOut()
                                onSignOut()
                            }
                        ) {
                            Icon(
                                Icons.Default.Logout,
                                contentDescription = "Cerrar sesión",
                                tint = AdminMuted
                            )
                        }
                    }
                }
                Divider(color = AdminOutline, thickness = 1.dp)
            }

            // ── Sección: Estadísticas rápidas ─────────────────────────────────
            item {
                Spacer(Modifier.height(24.dp))
                Text(
                    "Resumen del sistema",
                    color = AdminMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DirectionsBus,
                        label = "Rutas",
                        value = "47",
                        color = CardGreen
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Report,
                        label = "Reportes",
                        value = "12",
                        color = CardOrange
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.People,
                        label = "Usuarios",
                        value = "238",
                        color = CardBlue
                    )
                    StatCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.Warning,
                        label = "Alertas",
                        value = "3",
                        color = CardRed
                    )
                }
            }

            // ── Sección: Acciones rápidas ──────────────────────────────────────
            item {
                Spacer(Modifier.height(28.dp))
                Text(
                    "Acciones rápidas",
                    color = AdminMuted,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            item { AdminActionItem(icon = Icons.Default.AddRoad,       label = "Agregar nueva ruta",        subtitle = "Crea y configura una ruta nueva") }
            item { AdminActionItem(icon = Icons.Default.EditRoad,       label = "Editar rutas existentes",   subtitle = "Modifica nombre, tarifa o frecuencia") }
            item { AdminActionItem(icon = Icons.Default.MarkEmailRead,  label = "Revisar reportes",          subtitle = "12 reportes pendientes de revisión", badge = "12") }
            item { AdminActionItem(icon = Icons.Default.ManageAccounts, label = "Gestionar usuarios",        subtitle = "Ver, suspender o eliminar cuentas") }
            item { AdminActionItem(icon = Icons.Default.BarChart,       label = "Ver estadísticas",          subtitle = "Rutas más consultadas y horarios") }
            item { AdminActionItem(icon = Icons.Default.Notifications,  label = "Enviar notificación",       subtitle = "Avisa a los usuarios sobre cambios") }
        }
    }
}

// ─── Tarjeta de estadística ───────────────────────────────────────────────────
@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    color: Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = AdminSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, null, tint = color, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(value, color = AdminText, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(label, color = AdminMuted, fontSize = 12.sp)
        }
    }
}

// ─── Ítem de acción ───────────────────────────────────────────────────────────
@Composable
private fun AdminActionItem(
    icon: ImageVector,
    label: String,
    subtitle: String,
    badge: String? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        color = AdminSurface,
        onClick = { /* TODO: navegar a la sección correspondiente */ }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AdminAccent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = AdminAccentSoft, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(label, color = AdminText, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = AdminMuted, fontSize = 12.sp)
            }
            if (badge != null) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(CardOrange)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            } else {
                Icon(Icons.Default.ChevronRight, null, tint = AdminMuted)
            }
        }
    }
}
