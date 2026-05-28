package com.example.xalabus.ui.ratings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.data.ratings.RouteWithAvgRating
import com.example.xalabus.ui.viewmodel.RatingViewModel
import com.example.xalabus.ui.viewmodel.TopRatedUiState

/**
 * CU-24: Pantalla de rutas mejor calificadas, ordenadas por promedio descendente.
 * Cubre: C24-1 (lista ordenada + puntuación/conteo), C24-2 (estado vacío),
 *        C24-3 (paginación scroll incremental), C24-4 (filtro ciudad),
 *        C24-5 (error conexión + reintento).
 *
 * @param onRateRoute Callback CU-23: abre RatingDialog con (routeId, routeName).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopRatedRoutesScreen(
    viewModel: RatingViewModel,
    onRouteClick: (String) -> Unit,
    onRateRoute: (routeId: String, routeName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState   by viewModel.topRatedState.collectAsState()
    val listState  = rememberLazyListState()

    var showFilterDialog by remember { mutableStateOf(false) }
    var cityInput        by remember { mutableStateOf(viewModel.cityFilter ?: "") }
    val filterActive     = viewModel.cityFilter != null

    // Carga inicial
    LaunchedEffect(Unit) {
        if (viewModel.topRatedState.value is TopRatedUiState.Idle) {
            viewModel.loadTopRated()
        }
    }

    // C24-3: paginación incremental al llegar al final de la lista
    LaunchedEffect(listState.layoutInfo) {
        val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        val total       = listState.layoutInfo.totalItemsCount
        val state = uiState
        if (state is TopRatedUiState.Success && state.hasMore && lastVisible >= total - 3) {
            viewModel.loadTopRated()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Rutas Mejor Calificadas") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                },
                actions = {
                    // C24-4: botón de filtro — badge visual cuando hay filtro activo
                    BadgedBox(
                        badge = {
                            if (filterActive) Badge()
                        }
                    ) {
                        IconButton(onClick = { showFilterDialog = true }) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filtrar por ciudad")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {

                // ── Cargando ─────────────────────────────────────────────
                is TopRatedUiState.Loading -> {
                    Column(
                        modifier            = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text("Cargando rutas...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // ── C24-2: Sin calificaciones ─────────────────────────────
                is TopRatedUiState.Empty -> {
                    Column(
                        modifier            = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint     = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay rutas calificadas aún",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Explora las rutas y sé el primero en calificar.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        OutlinedButton(onClick = onDismiss) { Text("Ver rutas") }
                    }
                }

                // ── C24-5: Error de conexión ──────────────────────────────
                is TopRatedUiState.Error -> {
                    Column(
                        modifier            = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Error al cargar datos",
                            style      = MaterialTheme.typography.titleMedium,
                            color      = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(20.dp))
                        Button(onClick = { viewModel.loadTopRated(refresh = true) }) {
                            Text("Reintentar")
                        }
                    }
                }

                // ── C24-1 + C24-3: Lista de rutas ─────────────────────────
                is TopRatedUiState.Success -> {
                    LazyColumn(
                        state           = listState,
                        contentPadding  = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier        = Modifier.fillMaxSize()
                    ) {
                        // Chip de filtro activo (C24-4)
                        if (filterActive) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick  = {
                                        cityInput = ""
                                        viewModel.applyCityFilter(null)
                                    },
                                    label = { Text("Ciudad: ${viewModel.cityFilter}  \u2715") }
                                )
                            }
                        }

                        // C24-1: tarjetas ordenadas por promedio desc
                        items(state.routes, key = { it.id }) { route ->
                            TopRatedRouteCard(
                                route      = route,
                                position   = state.routes.indexOf(route) + 1,
                                onClick    = { onRouteClick(route.id) },
                                onRateRoute = { onRateRoute(route.id, route.name) }
                            )
                        }

                        // Indicador de carga incremental (C24-3)
                        if (state.hasMore) {
                            item {
                                Box(
                                    modifier         = Modifier.fillMaxWidth().padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier    = Modifier.size(24.dp),
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }
                    }
                }

                else -> Unit
            }
        }
    }

    // ── C24-4: Diálogo de filtro por ciudad ───────────────────────────────
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title  = { Text("Filtrar por ciudad") },
            text   = {
                OutlinedTextField(
                    value         = cityInput,
                    onValueChange = { cityInput = it },
                    label         = { Text("Ciudad") },
                    placeholder   = { Text("Ej: Xalapa") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.applyCityFilter(cityInput.trim().takeIf { it.isNotBlank() })
                    showFilterDialog = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Tarjeta individual (C24-1: muestra puntuación promedio y conteo) ──────────
@Composable
private fun TopRatedRouteCard(
    route: RouteWithAvgRating,
    position: Int,
    onClick: () -> Unit,
    onRateRoute: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Posición en el ranking
            Text(
                "#$position",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.width(36.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    route.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                // C24-1: muestra número de valoraciones
                Text(
                    "${route.ratingCount} valoraciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // C24-1: puntuación promedio + estrella
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint     = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        "%.1f".format(route.avgScore),
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color      = MaterialTheme.colorScheme.primary
                    )
                }
                // CU-23: botón calificar accesible desde CU-24
                TextButton(
                    onClick      = onRateRoute,
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                ) {
                    Text(
                        "Calificar",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
