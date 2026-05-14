package com.example.xalabus.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.xalabus.ui.viewmodel.RouteViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: RouteViewModel,
    onOpenDrawer: () -> Unit,
    onRouteClick: (String) -> Unit
) {
    val routes by viewModel.filteredRoutes.collectAsState()
    val searchText by viewModel.searchQuery.collectAsState()
    val startText by viewModel.startZoneQuery.collectAsState()
    val endText by viewModel.endZoneQuery.collectAsState()
    val uniqueZones by viewModel.uniqueZones.collectAsState()
    var showFaq by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "XalaBus",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Abrir menú"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showFaq = true },
                containerColor = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Help,
                    contentDescription = "Preguntas Frecuentes",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { 
            val isSorted by viewModel.isSortedAlphabetically.collectAsState()
            
            // Fila de búsqueda general y botón ordenar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchText,
                    onValueChange = { viewModel.onSearchQueryChanged(it) },
                    modifier = Modifier.weight(1f),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    placeholder = { Text("Buscar ruta...", style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp)) },
                    leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
                    trailingIcon = {
                        if (searchText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                                Icon(Icons.Default.Clear, "Limpiar", Modifier.size(20.dp))
                            }
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))

                FilledIconToggleButton(
                    checked = isSorted,
                    onCheckedChange = { viewModel.toggleSortAlphabetically() },
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = Color.Transparent,
                        checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.SortByAlpha,
                        contentDescription = "Ordenar A-Z"
                    )
                }
            }

            // Fila de búsqueda por zonas (Inicio y Fin)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ZoneDropdown(
                    label = "Zona de inicio...",
                    selectedOption = startText,
                    options = uniqueZones,
                    onOptionSelected = { viewModel.onStartZoneQueryChanged(it) },
                    modifier = Modifier.weight(1f)
                )

                ZoneDropdown(
                    label = "Zona de destino...",
                    selectedOption = endText,
                    options = uniqueZones,
                    onOptionSelected = { viewModel.onEndZoneQueryChanged(it) },
                    modifier = Modifier.weight(1f)
                )
            }

            Text(
                text = if (routes.isEmpty()) {
                    if (endText.isNotEmpty()) "Parámetro inválido"
                    else if (startText.isNotEmpty()) "Parámetro inválido"
                    else if (searchText.isNotEmpty()) "No se encontraron rutas para '$searchText'"
                    else "Rutas disponibles en Xalapa"
                } else "Rutas disponibles en Xalapa",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(routes) { route ->
                    RouteCard(
                        name = route.name,
                        onClick = { onRouteClick(route.id) }
                    )
                }
            }
        }

        if (showFaq) {
            FaqDialog(onDismiss = { showFaq = false })
        }
    }
}

@Composable
fun RouteCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
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

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 3 // Aumentado para que quepa la descripción larga
                )
                Text(
                    text = "Ver trazado en el mapa",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ZoneDropdown(
    label: String,
    selectedOption: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = { 
                onOptionSelected(it)
                expanded = true 
            },
            modifier = Modifier.menuAnchor(),
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
            placeholder = { Text(label, style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp), maxLines = 1) },
            shape = RoundedCornerShape(16.dp),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            ),
            trailingIcon = {
                if (selectedOption.isNotEmpty()) {
                    IconButton(onClick = { 
                        onOptionSelected("")
                        expanded = false
                    }) {
                        Icon(Icons.Default.Clear, "Limpiar", Modifier.size(20.dp))
                    }
                } else {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                }
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            val filteredOptions = options.filter { it.contains(selectedOption, ignoreCase = true) }
            if (filteredOptions.isNotEmpty()) {
                filteredOptions.take(15).forEach { selectionOption ->
                    DropdownMenuItem(
                        text = { Text(selectionOption, style = MaterialTheme.typography.bodyMedium) },
                        onClick = {
                            onOptionSelected(selectionOption)
                            expanded = false
                        }
                    )
                }
            } else {
                DropdownMenuItem(
                    text = { Text("No hay opciones") },
                    onClick = { expanded = false }
                )
            }
        }
    }
}