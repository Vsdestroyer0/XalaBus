package com.example.xalabus.ui.home

import androidx.compose.foundation.clickable // Faltaba este
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons // Faltaba este
import androidx.compose.material.icons.filled.ArrowForward // Faltaba este
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // Faltaba este
import androidx.compose.runtime.getValue // Faltaba este
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.RouteViewModel

@Composable
fun HomeScreen(
    viewModel: RouteViewModel,
    onRouteClick: (String) -> Unit
) {
    // La extensión .value o by collectAsState() necesita el import de arriba
    val routes by viewModel.routes.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "Rutas de XalaBus",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(routes) { route ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRouteClick(route.id.toString()) },
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = route.name, style = MaterialTheme.typography.titleMedium)
                            // Cambié .description por .desc si es que así se llama en tu Entity
                            Text(text = "Ver detalles de ruta", style = MaterialTheme.typography.bodySmall)
                        }
                        Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
                    }
                }
            }
        }
    }
}