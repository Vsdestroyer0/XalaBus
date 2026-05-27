package com.example.xalabus.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.ReportProblem
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.IncidentUiState
import com.example.xalabus.ui.viewmodel.IncidentViewModel
import com.example.xalabus.ui.viewmodel.isWithinXalapa

// ─── Bounding-box de Xalapa (mismos valores que IncidentViewModel) ────────────
private const val LAT_MIN = 19.48
private const val LAT_MAX = 19.62
private const val LNG_MIN = -97.00
private const val LNG_MAX = -96.85

/**
 * CU-13 — Pantalla para reportar algún inconveniente con la ruta.
 *
 * Flujo normal (F1-F6):
 *  1. Usuario accede y está registrado.
 *  2. Selecciona la opción de reportar → esta pantalla.
 *  3. Toca el mapa para marcar la ubicación del inconveniente.
 *  4. Escribe la descripción en el cuadro de texto.
 *  5. Opcionalmente adjunta foto (placeholder — extensión futura).
 *  6. Pulsa "Subir advertencia" → el sistema guarda el reporte.
 *
 * Flujos alternativos manejados:
 *  - FA-01 (C2): punto fuera del mapa → "Punto inválido"
 *  - FA-02 (C3): cuadro de texto vacío al enviar → validación mostrada
 *
 * Excepción:
 *  - Ex-01 (C4): el sistema no carga los datos → "Error al cargar datos"
 *
 * Post-condición: la advertencia queda registrada y visible para otros usuarios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportIncidentScreen(
    viewModel: IncidentViewModel,
    onDismiss: () -> Unit
) {
    val uiState       by viewModel.uiState.collectAsState()
    val selectedLat   by viewModel.selectedLat.collectAsState()
    val selectedLng   by viewModel.selectedLng.collectAsState()
    val pointSelected by viewModel.pointSelected.collectAsState()

    var descripcion by remember { mutableStateOf("") }
    // Coordenadas del toque en el canvas (para dibujar el pin)
    var tapOffset   by remember { mutableStateOf<Offset?>(null) }

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
                title = { Text("Reportar inconveniente") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.resetState()
                        onDismiss()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
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
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ── Encabezado ────────────────────────────────────────────────────
            Icon(
                Icons.Default.ReportProblem,
                contentDescription = null,
                modifier = Modifier
                    .size(52.dp)
                    .align(Alignment.CenterHorizontally),
                tint = MaterialTheme.colorScheme.error
            )
            Text(
                "¿Qué está pasando en la ruta?",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                modifier   = Modifier.fillMaxWidth()
            )

            // ── Paso 1: Seleccionar punto en el mapa ──────────────────────────
            Text(
                "Paso 1 — Toca el mapa para marcar la ubicación",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            MapPickerCanvas(
                tapOffset     = tapOffset,
                pointSelected = pointSelected,
                uiState       = uiState,
                onTap         = { offset, lat, lng ->
                    tapOffset = offset
                    viewModel.updateLocation(lat, lng)
                }
            )

            // Coordenadas del punto seleccionado
            if (pointSelected) {
                val coordText = "%.5f, %.5f".format(selectedLat, selectedLng)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        null,
                        Modifier.size(16.dp),
                        tint = if (isWithinXalapa(selectedLat, selectedLng))
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        coordText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider()

            // ── Paso 2: Descripción ───────────────────────────────────────────
            Text(
                "Paso 2 — Describe el inconveniente",
                style      = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )

            OutlinedTextField(
                value         = descripcion,
                onValueChange = { descripcion = it },
                label         = { Text("Descripción de la advertencia") },
                placeholder   = { Text("ej. La ruta cambió de dirección, hay tráfico bloqueado, bache peligroso...") },
                modifier      = Modifier
                    .fillMaxWidth()
                    .height(130.dp),
                maxLines = 5,
                // FA-02 (C3): mostrar borde de error si está vacío tras intentar enviar
                isError = uiState is IncidentUiState.Error &&
                    (uiState as IncidentUiState.Error).message.contains("vacío", ignoreCase = true)
            )

            // ── Paso 3: Foto (opcional) ───────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color  = MaterialTheme.colorScheme.surfaceVariant,
                        shape  = MaterialTheme.shapes.small
                    )
                    .padding(12.dp)
            ) {
                Icon(
                    Icons.Default.PhotoCamera,
                    null,
                    Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.width(8.dp))
                Column {
                    Text(
                        "Agregar foto (opcional)",
                        style      = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "La opción de adjuntar foto estará disponible próximamente.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ── Mensaje de error (FA-01, FA-02, Ex-01) ────────────────────────
            if (uiState is IncidentUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.ReportProblem,
                            null,
                            Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            (uiState as IncidentUiState.Error).message,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // ── Botón principal: Subir advertencia ────────────────────────────
            Button(
                onClick  = { viewModel.submitIncidente(descripcion) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                // Habilitado solo cuando hay descripción y no está cargando
                enabled  = descripcion.isNotBlank() &&
                           uiState !is IncidentUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor   = MaterialTheme.colorScheme.onError
                )
            ) {
                when (uiState) {
                    is IncidentUiState.Loading -> {
                        CircularProgressIndicator(
                            modifier    = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                            color       = MaterialTheme.colorScheme.onError
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Subiendo...")
                    }
                    else -> {
                        Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Subir advertencia", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Botón secundario: cancelar
            OutlinedButton(
                onClick   = {
                    viewModel.resetState()
                    onDismiss()
                },
                modifier  = Modifier.fillMaxWidth(),
                enabled   = uiState !is IncidentUiState.Loading
            ) {
                Text("Cancelar")
            }
        }
    }
}

/**
 * Canvas interactivo que simula el selector de punto en el mapa.
 *
 * - Muestra una cuadrícula simplificada con los límites de Xalapa.
 * - Al tocar, convierte las coordenadas del canvas en lat/lng y
 *   llama a [onTap] para que el ViewModel valide y actualice el estado.
 * - Si el punto está dentro del área válida se pinta en verde;
 *   fuera del área se pinta en rojo (FA-01).
 */
@Composable
private fun MapPickerCanvas(
    tapOffset     : Offset?,
    pointSelected : Boolean,
    uiState       : IncidentUiState,
    onTap         : (offset: Offset, lat: Double, lng: Double) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor   = MaterialTheme.colorScheme.error
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline

    // Color del pin dependiendo del estado FA-01
    val isInvalidPoint = uiState is IncidentUiState.Error &&
        (uiState as IncidentUiState.Error).message.contains("inválido", ignoreCase = true)
    val pinColor = if (isInvalidPoint) errorColor else primaryColor

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .background(surfaceColor, shape = MaterialTheme.shapes.medium)
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        // Convertir posición canvas → lat/lng dentro del bounding-box
                        val lat = LAT_MAX - (offset.y / size.height) * (LAT_MAX - LAT_MIN)
                        val lng = LNG_MIN + (offset.x / size.width)  * (LNG_MAX - LNG_MIN)
                        onTap(offset, lat, lng)
                    }
                }
        ) {
            val w = size.width
            val h = size.height

            // Cuadrícula de fondo
            val gridLines = 5
            val gridColor = Color(0x22000000)
            for (i in 0..gridLines) {
                val x = w * i / gridLines
                val y = h * i / gridLines
                drawLine(gridColor, Offset(x, 0f), Offset(x, h), strokeWidth = 1f)
                drawLine(gridColor, Offset(0f, y), Offset(w, y),  strokeWidth = 1f)
            }

            // Cruz central (centro de Xalapa)
            val cx = w * 0.5f
            val cy = h * 0.5f
            drawLine(outlineColor, Offset(cx - 12f, cy), Offset(cx + 12f, cy), strokeWidth = 2f)
            drawLine(outlineColor, Offset(cx, cy - 12f), Offset(cx, cy + 12f), strokeWidth = 2f)

            // Pin del punto seleccionado
            if (tapOffset != null && pointSelected) {
                drawCircle(pinColor.copy(alpha = 0.25f), radius = 22f, center = tapOffset)
                drawCircle(pinColor, radius = 10f, center = tapOffset)
                drawCircle(Color.White, radius = 4f, center = tapOffset)
            }
        }

        // Etiqueta de instrucción cuando no hay punto
        if (!pointSelected) {
            Text(
                "Toca aquí para marcar la ubicación",
                modifier = Modifier.align(Alignment.Center),
                style    = MaterialTheme.typography.labelMedium,
                color    = outlineColor,
                textAlign = TextAlign.Center
            )
        }

        // Etiqueta de referencia (esquina inferior)
        Text(
            "Área: Xalapa, Ver.",
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
            style    = MaterialTheme.typography.labelSmall,
            color    = outlineColor
        )
    }
}
