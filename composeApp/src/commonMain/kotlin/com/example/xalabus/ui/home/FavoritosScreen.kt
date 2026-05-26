package com.example.xalabus.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.xalabus.data.favoritos.Favorito
import com.example.xalabus.ui.viewmodel.FavoritosUiState
import com.example.xalabus.ui.viewmodel.FavoritosViewModel
import com.example.xalabus.ui.viewmodel.RouteViewModel

/**
 * CU-10 (extensión): Pantalla/hoja modal que muestra todas las rutas
 * marcadas como favoritas por el usuario autenticado.
 *
 * Se abre desde el Drawer lateral cuando el usuario está loggeado.
 * Al tocar una ruta favorita, navega directamente al mapa de esa ruta.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritosScreen(
    favoritosViewModel: FavoritosViewModel,
    routeViewModel: RouteViewModel,
    onNavigateToRoute: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val uiState by favoritosViewModel.uiState.collectAsState()

    // Cargar favoritos al abrir la pantalla
    LaunchedEffect(Unit) {
        favoritosViewModel.loadUserFavorites()
    }

    // Limpiar estado al salir
    DisposableEffect(Unit) {
        onDispose { favoritosViewModel.resetState() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Rutas Favoritas",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                // ── Cargando ──────────────────────────────────────────────────
                is FavoritosUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Cargando tus favoritos...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // ── Error (Ex-01) ─────────────────────────────────────────────
                is FavoritosUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            state.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        OutlinedButton(onClick = { favoritosViewModel.loadUserFavorites() }) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Reintentar")
                        }
                    }
                }

                // ── Lista de favoritos ────────────────────────────────────────
                is FavoritosUiState.Success -> {
                    val favoritos = state.favoritos
                    if (favoritos.isEmpty()) {
                        // Estado vacío
                        EmptyFavoritosState()
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Text(
                                    "${favoritos.size} ruta${if (favoritos.size != 1) "s" else ""} guardada${if (favoritos.size != 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            items(favoritos, key = { it.routeId }) { favorito ->
                                FavoritoCard(
                                    favorito = favorito,
                                    routeViewModel = routeViewModel,
                                    onTap = {
                                        onNavigateToRoute(favorito.routeId)
                                    },
                                    onRemove = {
                                        favoritosViewModel.removeFromFavorites(favorito.routeId)
                                    }
                                )
                            }
                            // Espacio al final para el último item
                            item { Spacer(Modifier.height(16.dp)) }
                        }
                    }
                }

                // ── Idle (estado inicial) ─────────────────────────────────────
                else -> { /* LaunchedEffect ya dispara la carga, no se llega aquí */ }
            }
        }
    }
}

/**
 * Tarjeta individual de ruta favorita.
 * Muestra el nombre de la ruta obtenido del RouteViewModel local.
 * Botón de eliminar (quitar de favoritos) en el lado derecho.
 */
@Composable
private fun FavoritoCard(
    favorito: Favorito,
    routeViewModel: RouteViewModel,
    onTap: () -> Unit,
    onRemove: () -> Unit
) {
    // Obtener nombre de la ruta desde la lista local (sin llamada a red)
    val allRoutes by routeViewModel.filteredRoutes.collectAsState()
    val routeName = remember(favorito.routeId, allRoutes) {
        allRoutes.find { it.id == favorito.routeId }?.name ?: "Ruta ${favorito.routeId}"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Ícono de bus
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = CircleShape,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Spacer(Modifier.width(16.dp))

            // Nombre de la ruta
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = routeName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3
                )
                Text(
                    text = "Ver trazado en el mapa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            // Botón quitar favorito
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Quitar de favoritos",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/** Estado vacío cuando el usuario no tiene rutas guardadas. */
@Composable
private fun EmptyFavoritosState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.StarBorder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "Sin rutas favoritas",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Toca la estrella ☆ en cualquier ruta del mapa para guardarla aquí.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}
