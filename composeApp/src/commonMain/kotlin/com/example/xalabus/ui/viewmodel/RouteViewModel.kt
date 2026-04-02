package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.repository.RouteRepository
import com.example.xalabus.DBD.RouteEntity
import com.example.xalabus.core.util.MapFileManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.jetbrains.compose.resources.ExperimentalResourceApi
import xalabus.composeapp.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class RouteViewModel(
    private val repository: RouteRepository,
    private val fileManager: MapFileManager
) : ViewModel() {

    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded

    private val _mapFilePath = MutableStateFlow<String?>(null)
    val mapFilePath: StateFlow<String?> = _mapFilePath



    private val _selectedRoute = MutableStateFlow<RouteEntity?>(null)
    val selectedRoute: StateFlow<RouteEntity?> = _selectedRoute

    private val _selectedRoutePoints = MutableStateFlow<List<List<List<Double>>>>(emptyList())
    val selectedRoutePoints: StateFlow<List<List<List<Double>>>> = _selectedRoutePoints

    // 1. El texto que el usuario escribe en la barra
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // 2. La lista COMPLETA de rutas que viene de la base de datos
    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())

    // 3. La lista FILTRADA que la vista realmente observa y muestra
    val filteredRoutes: StateFlow<List<RouteEntity>> = combine(
        _allRoutes,
        _searchQuery
    ) { routes, query ->
        if (query.isBlank()) {
            routes
        } else {
            // 1. Convertimos la búsqueda en "tokens" (palabras individuales)
            // y quitamos espacios extra.
            val searchTokens = query.trim().split("\\s+".toRegex())

            routes.filter { route ->
                // 2. Verificamos que CADA palabra de la búsqueda esté en el nombre/descripción
                searchTokens.all { token ->
                    route.id.contains(token, ignoreCase = true) ||
                            route.name.contains(token, ignoreCase = true)
                }
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )
    // 4. La función que se llama cada vez que el usuario teclea una letra
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    @OptIn(ExperimentalResourceApi::class)
    fun initializeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val routeBytes = Res.readBytes("files/master_routes_optimized.json")
                val mapBytes = Res.readBytes("files/xalapa.mbtiles")

                // 1. Insertar solo rutas y geometrías
                repository.checkAndPrepopulate(
                    routesJson = routeBytes.decodeToString()
                )

                val internalPath = fileManager.saveMapFile("xalapa.mbtiles", mapBytes)

                // 2. Traer rutas y limpiar nombres para la UI
                val allRoutes = repository.getAllRoutes().map { route ->
                    val finalName = if (route.name.contains("1000")) {
                        route.name.substringAfter("-").trim().ifEmpty { route.name }
                    } else {
                        route.name
                    }
                    route.copy(name = finalName)
                }

                launch(Dispatchers.Main) {
                    _mapFilePath.value = internalPath
                    _allRoutes.value = allRoutes
                    _isDataLoaded.value = true
                }

            } catch (e: Exception) {
                println("Error crítico en ViewModel: ${e.message}")
                launch(Dispatchers.Main) { _isDataLoaded.value = true }
            }
        }
    }

    fun selectRoute(routeId: String) {
        viewModelScope.launch {
            try {
                _selectedRoute.value = _allRoutes.value.find { it.id == routeId }

                // Solo recuperamos la geometría (el dibujo de la línea en el mapa)
                _selectedRoutePoints.value = getRoutePoints(routeId)

            } catch (e: Exception) {
                println("Error al seleccionar ruta $routeId: ${e.message}")
            }
        }
    }

    private fun getRoutePoints(routeId: String): List<List<List<Double>>> {
        val geometries = repository.getGeometryForRoute(routeId)
        return geometries.map { geometryEntity ->
            Json.decodeFromString<List<List<Double>>>(geometryEntity.polyline)
        }
    }
}