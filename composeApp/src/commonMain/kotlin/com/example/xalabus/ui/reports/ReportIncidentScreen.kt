package com.example.xalabus.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.IncidentUiState
import com.example.xalabus.ui.viewmodel.IncidentViewModel

/**
 * CU-13 — Reportar algún inconveniente con la ruta.
 *
 * Flujo normal:
 *  1. Acceder a la aplicación (registrado — controlado desde App.kt).
 *  2. Seleccionar la opción de añadir una advertencia.
 *  3. Tocar el mapa interactivo (MapLibre real) para marcar la ubicación.
 *  4. Describir la advertencia en el cuadro de texto.
 *  5. (Opcional) Adjuntar una foto.
 *  6. Subir la advertencia → el sistema la guarda y la hace visible.
 *
 * Casos de prueba cubiertos:
 *  C1 — flujo feliz
 *  C2 / FA-01 — Punto fuera de Xalapa → "Punto inválido"
 *  C3 / FA-02 — Descripción vacía → validación + botón deshabilitado
 *  C4 / Ex-01 — Error de red/datos  → "Error al cargar datos"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentViewModel,
    isDarkMode: Boolean = false,
    mapStylePath: String? = null,
    onDismiss: () -> Unit
) {
    val uiState     by viewModel.uiState.collectAsState()
    val selectedLat by viewModel.selectedLat.collectAsState()
    val selectedLng by viewModel.selectedLng.collectAsState()

    var descripcion by remember { mutableStateOf("") }

    val isDescriptionEmpty = descripcion.isBlank()
    val isPointValid       = IncidentViewModel.isWithinXalapa(selectedLat, selectedLng)

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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Encabezado ────────────────────────────────────────────────────
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Default.ReportProblem,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.error
                )
                Column {
                    Text(
                        "¿Qué está pasando?",
                        style      = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Marca el punto en el mapa y describe el incidente.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Paso 1: Mapa real de Xalapa (MapLibre) ────────────────────────
            Text(
                "1. Toca el mapa para marcar la ubicación",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier  = Modifier.fillMaxWidth().height(240.dp),
                shape     = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                // XalapaIncidentMap: en Android → MapView real de MapLibre
                //                   en iOS     → placeholder
                XalapaIncidentMap(
                    viewModel    = viewModel,
                    isDarkMode   = isDarkMode,
                    mapStylePath = mapStylePath,
                    modifier     = Modifier.fillMaxSize(),
                )
            }

            // Chip con coordenadas / error de punto inválido (C2)
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    if (isPointValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    modifier           = Modifier.size(18.dp),
                    tint               = if (isPointValid) MaterialTheme.colorScheme.primary
                                         else MaterialTheme.colorScheme.error
                )
                Text(
                    if (isPointValid)
                        "Lat: ${"%.4f".format(selectedLat)}  Lng: ${"%.4f".format(selectedLng)}"
                    else
                        "Punto inválido — selecciona dentro de Xalapa",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isPointValid) MaterialTheme.colorScheme.onSurfaceVariant
                             else MaterialTheme.colorScheme.error
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // ── Paso 2: Descripción ───────────────────────────────────────────
            Text(
                "2. Describe el problema",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )

            OutlinedTextField(
                value          = descripcion,
                onValueChange  = { descripcion = it },
                label          = { Text("Descripción de la advertencia *") },
                placeholder    = { Text("ej. Bloqueo de vía, desvío de ruta, tráfico intenso...") },
                modifier       = Modifier.fillMaxWidth().height(130.dp),
                maxLines       = 5,
                isError        = isDescriptionEmpty && uiState is IncidentUiState.Error,
                supportingText = {
                    if (isDescriptionEmpty) {
                        Text(
                            "Campo obligatorio — debes escribir una descripción.",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            )

            // ── Paso 3: Foto opcional (C1) ────────────────────────────────────
            OutlinedButton(
                onClick  = { /* extensión futura: Supabase Storage */ },
                modifier = Modifier.fillMaxWidth(),
                enabled  = false
            ) {
                Icon(Icons.Default.AttachFile, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Adjuntar foto (opcional) — próximamente")
            }

            // ── Mensaje de error (FA-01, FA-02, Ex-01) ────────────────────────
            if (uiState is IncidentUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier              = Modifier.padding(12.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint     = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            (uiState as IncidentUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // ── Botón enviar ──────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.submitIncidente(descripcion) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = !isDescriptionEmpty && isPointValid && uiState !is IncidentUiState.Loading,
                colors   = ButtonDefaults.buttonColors(
                    containerColor         = MaterialTheme.colorScheme.error,
                    contentColor           = MaterialTheme.colorScheme.onError,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                    disabledContentColor   = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                if (uiState is IncidentUiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onError
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Enviando...", fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Subir advertencia", fontWeight = FontWeight.Bold)
                }
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
