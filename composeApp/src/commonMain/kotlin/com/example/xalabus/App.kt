package com.example.xalabus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xalabus.XalaContext
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.ui.viewmodel.RouteViewModel

@Composable
fun App(driverFactory: DriverFactory) {
    // Inicializamos dependencias a través del Context
    val repository = remember { XalaContext.getRepository(driverFactory) }
    val viewModel = remember { RouteViewModel(repository) }
    val isLoaded by viewModel.isDataLoaded.collectAsState()

    // Disparamos la carga de datos al iniciar la App
    LaunchedEffect(Unit) {
        viewModel.initializeData()
    }

    // Aquí usamos el tema que ya tienes en tu carpeta ui/theme
    MaterialTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            if (!isLoaded) {
                Box(contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                    Text(
                        text = "Cargando rutas de Xalapa...",
                        modifier = Modifier.padding(top = 80.dp)
                    )
                }
            } else {
                // Aquí es donde llamarás a tu pantalla de Home o Map mas adelante
                Text("¡XalaBus está listo para navegar!")
            }
        }
    }
}