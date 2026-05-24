package com.example.xalabus.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * CU-12: Pantalla de registro de nuevas paradas de camión.
 *
 * IMPORTANTE: Esta pantalla SOLO debe ser navegable si el usuario
 * tiene rol "developer" o "admin" (validación en el NavGraph o en la pantalla llamante).
 *
 * Flujos:
 * - FA-01: datos incompletos → mensaje de error inline
 * - FA-02: parada duplicada  → mensaje de error con nombre de la parada cercana
 * - Ex-01: error de red     → mensaje de error con detalle
 *
 * @param onBack Navega de regreso al panel de administrador
 * @param onSuccess Navega o muestra confirmación tras guardado exitoso
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAddStopScreen(
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: AdminStopViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var nombre    by remember { mutableStateOf("") }
    var latitud   by remember { mutableStateOf("") }
    var longitud  by remember { mutableStateOf("") }
    var rutaId    by remember { mutableStateOf("") }

    // Postcondición: navegar tras éxito
    LaunchedEffect(uiState) {
        if (uiState is AdminStopUiState.Success) {
            onSuccess()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Agregar parada") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // Encabezado
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AddLocation,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Text(
                    text = "Nueva parada de camión",
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Text(
                text = "Solo visible para administradores. La parada quedará visible para todos los usuarios.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Campo: Nombre de la parada
            OutlinedTextField(
                value         = nombre,
                onValueChange = { nombre = it },
                label         = { Text("Nombre de la parada") },
                placeholder   = { Text("Ej. Parque Juárez, esquina 20 de Noviembre") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // Campo: ID de ruta
            OutlinedTextField(
                value         = rutaId,
                onValueChange = { rutaId = it },
                label         = { Text("ID de ruta") },
                placeholder   = { Text("Ej. ruta_01") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )

            // Campos: Coordenadas GPS
            Text(
                text  = "Coordenadas GPS",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value         = latitud,
                    onValueChange = { latitud = it },
                    label         = { Text("Latitud") },
                    placeholder   = { Text("19.5438") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value         = longitud,
                    onValueChange = { longitud = it },
                    label         = { Text("Longitud") },
                    placeholder   = { Text("-96.9270") },
                    singleLine    = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier      = Modifier.weight(1f)
                )
            }

            // Mensaje de error (FA-01, FA-02, Ex-01)
            if (uiState is AdminStopUiState.Error) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as AdminStopUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Botón de guardar
            Button(
                onClick  = { viewModel.saveParada(nombre, latitud, longitud, rutaId) },
                enabled  = uiState !is AdminStopUiState.Loading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (uiState is AdminStopUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardando...")
                } else {
                    Text("Guardar parada")
                }
            }
        }
    }
}
