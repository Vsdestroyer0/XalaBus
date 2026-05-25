package com.example.xalabus.ui.map

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.RouteTimeUiState
import com.example.xalabus.ui.viewmodel.RouteTimeViewModel

/**
 * CU-11: Panel deslizable que muestra el tiempo estimado de traslado
 * para la ruta seleccionada actualmente.
 *
 * Se muestra en la parte inferior de la pantalla del mapa cuando hay
 * una ruta activa. Permite al usuario:
 *   1. Ver el tiempo estimado del recorrido completo.
 *   2. Seleccionar una parada de origen y destino para un tramo específico.
 *   3. Ver distancia aproximada y número de paradas del tramo.
 *
 * Estados manejados (Ex-01):
 *   - Loading → muestra indicador de progreso.
 *   - Ready   → muestra selectores de tramo.
 *   - Result  → muestra tiempo estimado y detalle.
 *   - Error   → muestra mensaje de error amigable.
 *   - Idle    → el panel no se muestra.
 *
 * @param routeId            ID de la ruta actualmente seleccionada.
 * @param routeTimeViewModel ViewModel que gestiona la lógica de CU-11.
 * @param onDismiss          Callback para cerrar el panel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteTimePanel(
    routeId: String,
    routeTimeViewModel: RouteTimeViewModel,
    onDismiss: () -> Unit
) {
    val uiState by routeTimeViewModel.uiState.collectAsState()

    // Cargar paradas al abrir el panel para esta ruta
    LaunchedEffect(routeId) {
        routeTimeViewModel.loadStops(routeId)
    }

    // El panel solo se muestra si hay estado activo (no Idle)
    val visible = uiState !is RouteTimeUiState.Idle

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit  = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {

                // ── Encabezado del panel ────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Tiempo estimado",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(
                        onClick = {
                            routeTimeViewModel.resetState()
                            onDismiss()
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Cerrar panel de tiempo",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // ── Contenido según estado ──────────────────────────────────
                when (val state = uiState) {

                    // Estado de carga
                    is RouteTimeUiState.Loading -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Calculando tiempo de traslado...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Resultado del cálculo — vista principal
                    is RouteTimeUiState.Result -> {
                        ResultContent(
                            state = state,
                            onSegmentSelected = { from, to ->
                                routeTimeViewModel.onSegmentSelected(from, to)
                            }
                        )
                    }

                    // Paradas listas pero sin resultado aún (transición)
                    is RouteTimeUiState.Ready -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Procesando paradas...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Ex-01: error al cargar datos
                    is RouteTimeUiState.Error -> {
                        ErrorContent(message = state.message)
                    }

                    // Idle — no debería llegar aquí por el AnimatedVisibility
                    else -> Unit
                }
            }
        }
    }
}

/**
 * Contenido del panel cuando el cálculo está listo.
 * Muestra el tiempo estimado y dos Dropdowns para seleccionar tramo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResultContent(
    state: RouteTimeUiState.Result,
    onSegmentSelected: (Int, Int) -> Unit
) {
    val paradas = state.paradas
    var fromIndex by remember(state.fromIndex) { mutableStateOf(state.fromIndex) }
    var toIndex   by remember(state.toIndex)   { mutableStateOf(state.toIndex) }

    // ── Chip de tiempo estimado ─────────────────────────────────────────────
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Default.DirectionsBus,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = state.formattedTime,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "${state.stopCount} paradas",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                text = "~${state.distanceKm} km",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    // ── Selector de tramo ───────────────────────────────────────────────────
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Route,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.secondary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = "Seleccionar tramo",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.secondary
        )
    }

    Spacer(Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Dropdown origen
        StopDropdown(
            label = "Desde",
            paradas = paradas.map { it.nombre },
            selectedIndex = fromIndex,
            modifier = Modifier.weight(1f),
            onSelected = { idx ->
                fromIndex = idx
                // Recalcular solo si el tramo es válido
                if (idx < toIndex) onSegmentSelected(idx, toIndex)
            }
        )
        // Dropdown destino
        StopDropdown(
            label = "Hasta",
            paradas = paradas.map { it.nombre },
            selectedIndex = toIndex,
            modifier = Modifier.weight(1f),
            onSelected = { idx ->
                toIndex = idx
                if (fromIndex < idx) onSegmentSelected(fromIndex, idx)
            }
        )
    }
}

/**
 * Dropdown reutilizable para seleccionar una parada de la lista.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopDropdown(
    label: String,
    paradas: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = paradas.getOrElse(selectedIndex) { "-" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label, style = MaterialTheme.typography.labelSmall) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            singleLine = true,
            textStyle = MaterialTheme.typography.bodySmall,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            paradas.forEachIndexed { idx, nombre ->
                DropdownMenuItem(
                    text = {
                        Text(
                            nombre,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (idx == selectedIndex) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    onClick = {
                        onSelected(idx)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Contenido de error para Ex-01 (datos no disponibles).
 */
@Composable
private fun ErrorContent(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.AccessTime,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error
        )
    }
}
