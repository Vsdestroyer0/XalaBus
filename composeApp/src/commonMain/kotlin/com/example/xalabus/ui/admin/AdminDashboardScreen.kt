package com.example.xalabus.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AdminBg     = Color(0xFF080C14)
private val AdminText   = Color(0xFFE2E8F0)
private val AdminAccent = Color(0xFF6366F1)
private val AdminMuted  = Color(0xFF64748B)

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onSignOut: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AdminBg),
        contentAlignment = Alignment.Center
    ) {
        // Botón cerrar sesión arriba a la derecha
        IconButton(
            onClick = {
                viewModel.signOut()
                onSignOut()
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "Cerrar sesión", tint = AdminMuted)
        }

        // Contenido central
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                Icons.Default.AdminPanelSettings,
                contentDescription = null,
                tint = AdminAccent,
                modifier = Modifier.size(64.dp)
            )
            Text(
                "Página Administrador",
                color = AdminText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "XalaBus",
                color = AdminAccent,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
