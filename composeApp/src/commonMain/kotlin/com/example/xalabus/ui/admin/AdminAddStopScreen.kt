package com.example.xalabus.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.xalabus.data.paradas.Parada

/**
 * CU-12 — Pantalla para registrar una nueva parada de camión.
 * SOLO accesible para usuarios con rol admin/developer.
 *
 * Flujo:
 *  1. El admin ingresa nombre, latitud, longitud y ruta_id.
 *  2. Si hay paradas cercanas → diálogo de advertencia (FA-02).
 *  3. Si datos incompletos → error inline (FA-01).
 *  4. Éxito → parada visible para todos los usuarios.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStopScreen(
    viewModel: AdminStopViewModel,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var nombre   by remember { mutableStateOf("") }
    var latitud  by remember { mutableStateOf("") }
    var longitud by remember { mutableStateOf("") }
    var rutaId   by remember { mutableStateOf("") }

    LaunchedEffect(uiState) {
        when (uiState) {
            is AdminStopUiState.Success -> {
                nombre   = ""
                latitud  = ""
                longitud = ""
                rutaId   = ""
            }
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar parada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
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
                Icons.Default.AddLocation,
                contentDescription = null,
                modifier = Modifier.size(48.dp).align(Alignment.CenterHorizontally),
                tint     = MaterialTheme.colorScheme.primary
            )

            Text(
                "Nueva parada de camión",
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.align(Alignment.CenterHorizontally)
            )

            // ── Campo: Nombre de la parada ────────────────────────────────────
            OutlinedTextField(
                value         = nombre,
                onValueChange = { nombre = it },
                label         = { Text("Nombre de la parada") },
                placeholder   = { Text("ej. Parque Juárez, Terminal CAXA...") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true
            )

            // ── Campos: Coordenadas GPS ───────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value         = latitud,
                    onValueChange = { latitud = it },
                    label         = { Text("Latitud") },
                    placeholder   = { Text("19.5438") },
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction    = ImeAction.Next
                    ),
                    singleLine = true
                )
                OutlinedTextField(
                    value         = longitud,
                    onValueChange = { longitud = it },
                    label         = { Text("Longitud") },
                    placeholder   = { Text("-96.9269") },
                    modifier      = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction    = ImeAction.Next
                    ),
                    singleLine = true
                )
            }

            // ── Campo: ID de la ruta ──────────────────────────────────────────
            OutlinedTextField(
                value         = rutaId,
                onValueChange = { rutaId = it },
                label         = { Text("ID de la ruta") },
                placeholder   = { Text("ej. 1, 2, 15...") },
                modifier      = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction    = ImeAction.Done
                ),
                singleLine = true
            )

            // ── Mensajes de estado ────────────────────────────────────────────
            when (val s = uiState) {
                is AdminStopUiState.Error ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            s.message,
                            modifier = Modifier.padding(12.dp),
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodySmall
                        )
                    }
                is AdminStopUiState.Success ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Text(
                            "✓ Parada guardada exitosamente.",
                            modifier = Modifier.padding(12.dp),
                            color    = MaterialTheme.colorScheme.onPrimaryContainer,
                            style    = MaterialTheme.typography.bodySmall
                        )
                    }
                else -> Unit
            }

            Spacer(Modifier.weight(1f))

            // ── Botón guardar ─────────────────────────────────────────────────
            Button(
                onClick  = { viewModel.saveParada(nombre, latitud, longitud, rutaId) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                enabled  = uiState !is AdminStopUiState.Loading
            ) {
                if (uiState is AdminStopUiState.Loading) {
                    CircularProgressIndicator(
                        modifier    = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color       = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.Default.AddLocation, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Guardar parada", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
