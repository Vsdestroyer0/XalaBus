package com.example.xalabus.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xalabus.XalaContext
import com.example.xalabus.db.DriverFactory
import com.example.xalabus.core.util.MapFileManager // 1. Importa la interfaz
import com.example.xalabus.ui.home.HomeScreen
import com.example.xalabus.ui.viewmodel.RouteViewModel
import com.example.xalabus.ui.map.MapScreen

@Composable
fun LoadingScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Cargando XalaBus...")
        }
    }
}

@Composable
fun App(
    driverFactory: DriverFactory,
    fileManager: MapFileManager
) {
    val repository = remember { XalaContext.getRepository(driverFactory) }
    val viewModel = remember { RouteViewModel(repository, fileManager) }

    val isLoaded by viewModel.isDataLoaded.collectAsState()

    // Estado para controlar la navegación
    var showMap by remember { mutableStateOf(false) }

    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            if (!isLoaded) {
                // Pantalla de carga
                LoadingScreen()
            } else {
                // Navegación simple entre Lista y Mapa
                if (!showMap) {
                    HomeScreen(
                        viewModel = viewModel,
                        onRouteClick = { routeId ->
                            viewModel.selectRoute(routeId)
                            showMap = true // Al picar, mandamos al mapa
                        }
                    )
                } else {
                    // Contenedor del mapa con botón para regresar
                    Box(Modifier.fillMaxSize()) {
                        MapScreen(viewModel = viewModel)

                        // Botón flotante para volver a la lista
                        Button(
                            onClick = { showMap = false },
                            modifier = Modifier
                                .padding(16.dp)
                                .align(Alignment.TopStart)
                        ) {
                            Text("Volver")
                        }
                    }
                }
            }
        }
    }
}