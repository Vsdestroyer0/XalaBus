package com.example.xalabus.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.IncidentUiState
import com.example.xalabus.ui.viewmodel.IncidentViewModel

/**
 * CU-13 — Reportar algún inconveniente con la ruta.
 *
 * Flujo normal:
 *  1. Acceder a la aplicación (estar registrado — controlado desde App.kt).
 *  2. Seleccionar la opción de añadir una advertencia.
 *  3. Tocar el mapa interactivo para marcar la ubicación del incidente.
 *  4. Describir la advertencia en el cuadro de texto.
 *  5. (Opcional) Adjuntar una foto.
 *  6. Subir la advertencia → el sistema la guarda y la hace visible.
 *
 * Flujos alternativos y excepciones cubiertos según tabla de casos de prueba:
 *  C2 / FA-01 — Punto fuera del mapa → "Punto inválido" (validado en ViewModel)
 *  C3 / FA-02 — Cuadro de texto vacío → validación visual + botón deshabilitado
 *  C4 / Ex-01 — Error de red/datos    → "Error al cargar datos"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentViewModel,
    onDismiss: () -> Unit
) {
    val uiState     by viewModel.uiState.collectAsState()
    val selectedLat by viewModel.selectedLat.collectAsState()
    val selectedLng by viewModel.selectedLng.collectAsState()

    var descripcion by remember { mutableStateOf("") }
    var mapSize     by remember { mutableStateOf(IntSize.Zero) }

    // Límites del mapa de Xalapa (coinciden con IncidentViewModel.Companion)
    val LAT_MIN = 19.48; val LAT_MAX = 19.60
    val LNG_MIN = -97.00; val LNG_MAX = -96.85

    fun latToFraction(lat: Double) = ((LAT_MAX - lat)  / (LAT_MAX - LAT_MIN)).coerceIn(0.0, 1.0)
    fun lngToFraction(lng: Double) = ((lng   - LNG_MIN) / (LNG_MAX - LNG_MIN)).coerceIn(0.0, 1.0)
    fun fractionToLat(f: Float)    = LAT_MAX - f * (LAT_MAX - LAT_MIN)
    fun fractionToLng(f: Float)    = LNG_MIN + f * (LNG_MAX - LNG_MIN)

    val markerFracX = lngToFraction(selectedLng).toFloat()
    val markerFracY = latToFraction(selectedLat).toFloat()

    val isDescriptionEmpty = descripcion.isBlank()
    val isPointValid       = IncidentViewModel.isWithinXalapa(selectedLat, selectedLng)

    // Colores cartográficos fijos — independientes del tema de la app
    // Verde oscuro para zona válida, rojo oscuro para zona inválida
    val mapBgColor  = if (isPointValid) Color(0xFF2D6A4F) else Color(0xFF8B2020)
    val gridColor   = Color.White.copy(alpha = 0.20f)
    val labelColor  = Color.White.copy(alpha = 0.85f)

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
                verticalAlignment = Alignment.CenterVertically,
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

            // ── Paso 1: Seleccionar punto en el mapa ──────────────────────────
            Text(
                "1. Toca el mapa para marcar la ubicación",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.primary
            )

            Card(
                modifier  = Modifier.fillMaxWidth().height(220.dp),
                shape     = RoundedCornerShape(12.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Color cartográfico fijo — no depende del tema Material
                        .background(mapBgColor)
                        .onSizeChanged { mapSize = it }
                        .pointerInput(Unit) {
                            detectTapGestures { offset ->
                                if (mapSize.width > 0 && mapSize.height > 0) {
                                    viewModel.updateLocation(
                                        fractionToLat(offset.y / mapSize.height),
                                        fractionToLng(offset.x / mapSize.width)
                                    )
                                }
                            }
                        }
                ) {
                    // Cuadrícula blanca semitransparente
                    androidx.compose.foundation.Canvas(Modifier.fillMaxSize()) {
                        val cols = 6; val rows = 5
                        for (i in 1 until cols) {
                            val x = size.width * i / cols
                            drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1.5f)
                        }
                        for (j in 1 until rows) {
                            val y = size.height * j / rows
                            drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.5f)
                        }
                    }

                    // Etiqueta Norte
                    Text(
                        "▲ Norte",
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 6.dp)
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = labelColor
                    )

                    // Etiqueta central de referencia
                    Text(
                        "Xalapa · toca para marcar",
                        modifier = Modifier
                            .align(Alignment.Center)
                            .background(Color.Black.copy(alpha = 0.25f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = labelColor
                    )

                    // Marcador: posicionado en píxeles exactos con absoluteOffset
                    if (mapSize.width > 0 && mapSize.height > 0) {
                        val iconSizePx = 28
                        val markerX = (markerFracX * mapSize.width  - iconSizePx / 2).toInt()
                        val markerY = (markerFracY * mapSize.height - iconSizePx).toInt()
                        Icon(
                            Icons.Default.Place,
                            contentDescription = "Ubicación seleccionada",
                            modifier = Modifier
                                .size(iconSizePx.dp)
                                .absoluteOffset { IntOffset(markerX, markerY) },
                            tint = Color.White
                        )
                    }
                }
            }

            // Chip estado del punto
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
                        "Lat: ${"%,.4f".format(selectedLat)}  Lng: ${"%,.4f".format(selectedLng)}"
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
