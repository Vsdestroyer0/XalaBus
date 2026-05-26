package com.example.xalabus.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.IncidentUiState
import com.example.xalabus.ui.viewmodel.IncidentViewModel

/**
 * CU-13 — Pantalla de reporte de incidente en una ruta.
 *
 * Flujo:
 *  1. Usuario ve las coordenadas actuales (o las ajusta manualmente).
 *  2. Ingresa una descripción del problema.
 *  3. Toca "Enviar" → reporte guardado en Supabase tabla `reportes`.
 *
 * FA-01: coordenadas fuera del área de Xalapa → error en ViewModel.
 * FA-02: descripción vacía → error en ViewModel.
 * Ex-01: error al subir → error en ViewModel.
 *
 * Nota: La foto adjunta (campo foto_url) se deja como null en esta versión.
 * Se puede extender con Supabase Storage en una iteración futura.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentViewModel,
    onDismiss: () -> Unit
) {
    val uiState    by viewModel.uiState.collectAsState()
    val selectedLat by viewModel.selectedLat.collectAsState()
    val selectedLng by viewModel.selectedLng.collectAsState()

    var descripcion  by remember { mutableStateOf("") }
    // Campos para edición manual de coordenadas
    var latText      by remember(selectedLat) { mutableStateOf("%.6f".format(selectedLat)) }
    var lngText      by remember(selectedLng) { mutableStateOf("%.6f".format(selectedLng)) }

    // Navegar de vuelta al completar exitosamente
    LaunchedEffect(uiState) {
        if (uiState is IncidentUiState.Success) {
            viewModel.resetState()
            onDismiss()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reportar incidente") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.ArrowBack, "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            Icon(
                Icons.Default.ReportProblem,
                contentDescription = null,
                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                tint     = MaterialTheme.colorScheme.error
            )

            Text(
                "¿Qué está pasando?",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.CenterHorizontally)
            )

            // ── Sección de ubicación GPS ──────────────────────────────────────
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.MyLocation,
                            null,
                            Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Ubicación del incidente",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value         = latText,
                            onValueChange = {
                                latText = it
                                it.toDoubleOrNull()?.let { d -> viewModel.updateLocation(d, selectedLng) }
                            },
                            label    = { Text("Latitud") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value         = lngText,
                            onValueChange = {
                                lngText = it
                                it.toDoubleOrNull()?.let { d -> viewModel.updateLocation(selectedLat, d) }
                            },
                            label    = { Text("Longitud") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Centro de Xalapa: 19.5438, -96.9269",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Descripción del problema ──────────────────────────────────────
            OutlinedTextField(
                value         = descripcion,
                onValueChange = { descripcion = it },
                label         = { Text("Describe el problema") },
                placeholder   = { Text("ej. La ruta cambió de dirección, hay tráfico bloqueado...") },
                modifier      = Modifier.fillMaxWidth().height(140.dp),
                maxLines      = 5
            )

            // ── Nota sobre foto (futura extensión) ───────────────────────────
            Text(
                "📎 La opción de adjuntar foto estará disponible próximamente.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // ── Mensaje de error ──────────────────────────────────────────────
            if (uiState is IncidentUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Text(
                        (uiState as IncidentUiState.Error).message,
                        modifier = Modifier.padding(12.dp),
                        color    = MaterialTheme.colorScheme.onErrorContainer,
                        style    = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            // ── Botón enviar ──────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.submitIncidente(descripcion) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = descripcion.isNotBlank() && uiState !is IncidentUiState.Loading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                )
            ) {
                if (uiState is IncidentUiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onError
                    )
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Enviar reporte", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
