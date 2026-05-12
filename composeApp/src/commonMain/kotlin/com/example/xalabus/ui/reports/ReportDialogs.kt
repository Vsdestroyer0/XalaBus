package com.example.xalabus.ui.reports

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.ReportUiState
import com.example.xalabus.ui.viewmodel.ReportsViewModel

@Composable
fun GeneralReportDialog(
    viewModel: ReportsViewModel,
    onDismiss: () -> Unit
) {
    var message by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = { 
            if (uiState !is ReportUiState.Loading) {
                viewModel.resetState()
                onDismiss()
            }
        },
        title = { Text("Reporte General") },
        text = {
            Column {
                Text("¿Tienes algún comentario, queja o sugerencia sobre la aplicación?")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = message,
                    onValueChange = { message = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("Escribe tu reporte aquí...") }
                )
                
                if (uiState is ReportUiState.Error) {
                    Text(
                        text = (uiState as ReportUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (uiState is ReportUiState.Success) {
                    Text(
                        text = "¡Reporte enviado con éxito!",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (uiState is ReportUiState.Success) {
                Button(onClick = { 
                    viewModel.resetState()
                    onDismiss() 
                }) {
                    Text("Cerrar")
                }
            } else {
                Button(
                    onClick = { viewModel.submitGeneralReport(message) },
                    enabled = message.isNotBlank() && uiState !is ReportUiState.Loading
                ) {
                    if (uiState is ReportUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Enviar")
                    }
                }
            }
        },
        dismissButton = {
            if (uiState !is ReportUiState.Success && uiState !is ReportUiState.Loading) {
                TextButton(onClick = { 
                    viewModel.resetState()
                    onDismiss() 
                }) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
fun RouteStopDialog(
    viewModel: ReportsViewModel,
    routeId: Int,
    latitude: Double,
    longitude: Double,
    onDismiss: () -> Unit
) {
    var description by remember { mutableStateOf("") }
    val uiState by viewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = { 
            if (uiState !is ReportUiState.Loading) {
                viewModel.resetState()
                onDismiss()
            }
        },
        title = { Text("Sugerir Parada") },
        text = {
            Column {
                Text("Describe brevemente el lugar de la parada (ej. 'Frente al Oxxo', 'Esquina con calle principal').")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("Descripción de la parada...") }
                )
                
                if (uiState is ReportUiState.Error) {
                    Text(
                        text = (uiState as ReportUiState.Error).message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                if (uiState is ReportUiState.Success) {
                    Text(
                        text = "¡Sugerencia enviada! Un administrador la revisará.",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (uiState is ReportUiState.Success) {
                Button(onClick = { 
                    viewModel.resetState()
                    onDismiss() 
                }) {
                    Text("Cerrar")
                }
            } else {
                Button(
                    onClick = { viewModel.submitRouteStop(routeId, description, latitude, longitude) },
                    enabled = description.isNotBlank() && uiState !is ReportUiState.Loading
                ) {
                    if (uiState is ReportUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Enviar")
                    }
                }
            }
        },
        dismissButton = {
            if (uiState !is ReportUiState.Success && uiState !is ReportUiState.Loading) {
                TextButton(onClick = { 
                    viewModel.resetState()
                    onDismiss() 
                }) {
                    Text("Cancelar")
                }
            }
        }
    )
}
