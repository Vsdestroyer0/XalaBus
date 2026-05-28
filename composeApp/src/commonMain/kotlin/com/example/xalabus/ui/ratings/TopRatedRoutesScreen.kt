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
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.ui.viewmodel.RouteWithAvgRating
import com.example.xalabus.ui.viewmodel.RatingViewModel
import com.example.xalabus.ui.viewmodel.TopRatedUiState
import io.github.jan.supabase.auth.auth

/**
 * CU-24: Pantalla pública de rutas mejor calificadas (ordenadas por promedio desc).
 * Accesible sin login. El botón "Calificar" solo aparece si hay sesión activa.
 * Cubre: C24-1 (lista ordenada), C24-2 (vacío), C24-3 (paginación scroll),
 *        C24-4 (filtro ciudad), C24-5 (error conexión).
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

    val isAuthenticated = remember {
        SupabaseClientProvider.client.auth.currentSessionOrNull() != null
    }

    var showFilterDialog by remember { mutableStateOf(false) }
    var cityInput        by remember { mutableStateOf(viewModel.cityFilter ?: "") }

    // Carga inicial
    LaunchedEffect(Unit) {
        if (viewModel.topRatedState.value is TopRatedUiState.Idle) {
            viewModel.loadTopRated()
        }
    }

    // C24-3: paginación incremental al acercarse al final de la lista
    val lastVisibleIndex by remember {
        derivedStateOf { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
    }
    val totalItems by remember {
        derivedStateOf { listState.layoutInfo.totalItemsCount }
    }
    LaunchedEffect(lastVisibleIndex) {
        val state = uiState
        if (state is TopRatedUiState.Success && state.hasMore && totalItems > 0
            && lastVisibleIndex >= totalItems - 3
        ) {
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
                    // C24-4: filtro por ciudad
                    IconButton(onClick = { showFilterDialog = true }) {
                        Icon(Icons.Default.FilterList, contentDescription = "Filtrar por ciudad")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor    = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor     = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {

                // ── Cargando primera página ───────────────────────────────
                is TopRatedUiState.Loading -> {
                    Column(
                        modifier              = Modifier.fillMaxSize(),
                        verticalArrangement   = Arrangement.Center,
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando rutas...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── C24-2: Sin rutas calificadas ──────────────────────────
                is TopRatedUiState.Empty -> {
                    Column(
                        modifier              = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement   = Arrangement.Center,
                        horizontalAlignment   = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = null,
                            modifier           = Modifier.size(64.dp),
                            tint               = MaterialTheme.colorScheme.outline
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No hay rutas calificadas aún",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Explora las rutas disponibles y sé el primero en calificar.",
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
                        modifier              = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement   = Arrangement.Center,
                        horizontalAlignment   = Alignment.CenterHorizontally
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

                // ── C24-1 + C24-3: Lista ordenada ─────────────────────────
                is TopRatedUiState.Success -> {
                    LazyColumn(
                        state               = listState,
                        contentPadding      = PaddingValues(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier            = Modifier.fillMaxSize()
                    ) {
                        // Badge de filtro activo (C24-4)
                        if (viewModel.cityFilter != null) {
                            item {
                                FilterChip(
                                    selected = true,
                                    onClick  = {
                                        cityInput = ""
                                        viewModel.applyCityFilter(null)
                                    },
                                    label    = { Text("Ciudad: ${viewModel.cityFilter}  ✕") },
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                        }

                        // Items ordenados por avg_score desc (C24-1)
                        items(state.routes, key = { it.id }) { route ->
                            TopRatedRouteCard(
                                route        = route,
                                position     = state.routes.indexOf(route) + 1,
                                isAuthenticated = isAuthenticated,
                                onRouteClick = { onRouteClick(route.id) },
                                onRateClick  = { onRateRoute(route.id, route.name) }
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

    // ── C24-4: Diálogo de filtro por ciudad ──────────────────────────────────
    if (showFilterDialog) {
        AlertDialog(
            onDismissRequest = { showFilterDialog = false },
            title   = { Text("Filtrar por ciudad") },
            text    = {
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
                    viewModel.applyCityFilter(cityInput.takeIf { it.isNotBlank() })
                    showFilterDialog = false
                }) { Text("Aplicar") }
            },
            dismissButton = {
                TextButton(onClick = { showFilterDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

// ── Tarjeta individual de ruta ────────────────────────────────────────────────
@Composable
private fun TopRatedRouteCard(
    route: RouteWithAvgRating,
    position: Int,
    isAuthenticated: Boolean,
    onRouteClick: () -> Unit,
    onRateClick: () -> Unit
) {
    Card(
        onClick   = onRouteClick,
        modifier  = Modifier.fillMaxWidth(),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier              = Modifier.padding(16.dp),
            verticalAlignment     = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Posición
            Text(
                "#$position",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color      = MaterialTheme.colorScheme.primary,
                modifier   = Modifier.width(36.dp)
            )

            // Nombre y conteo
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    route.name,
                    style      = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "${route.ratingCount} valoraciones",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Promedio con estrella (C24-1)
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
        }

        // Botón calificar: solo visible para usuarios autenticados (C23-4)
        if (isAuthenticated) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                color    = MaterialTheme.colorScheme.outlineVariant
            )
            TextButton(
                onClick  = onRateClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = null,
                    modifier           = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text("Calificar esta ruta")
            }
        }
    }
}
