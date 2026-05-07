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

    // NUEVO: 2. Estado para saber si el orden alfabético está activo
    private val _isSortedAlphabetically = MutableStateFlow(false)
    val isSortedAlphabetically: StateFlow<Boolean> = _isSortedAlphabetically

    // 3. La lista COMPLETA de rutas que viene de la base de datos
    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())

    // 4. La lista FILTRADA que la vista realmente observa y muestra
    // NUEVO: Agregamos _isSortedAlphabetically al combine
    val filteredRoutes: StateFlow<List<RouteEntity>> = combine(
        _allRoutes,
        _searchQuery,
        _isSortedAlphabetically
    ) { routes, query, isSorted ->

        // Primero filtramos por la búsqueda de texto
        val resultList = if (query.isBlank()) {
            routes
        } else {
            val searchTokens = query.trim().split("\\s+".toRegex())
            routes.filter { route ->
                searchTokens.all { token ->
                    route.id.contains(token, ignoreCase = true) ||
                            route.name.contains(token, ignoreCase = true)
                }
            }
        }

        // Segundo filtramos por el ordenamiento alfabético
        if (isSorted) {
            resultList.sortedBy { it.name }
        } else {
            resultList // Si no está activo el botón, se queda en el orden normal
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Función que se llama cada vez que el usuario teclea una letra
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    // NUEVO: Función para encender/apagar el filtro A-Z
    fun toggleSortAlphabetically() {
        _isSortedAlphabetically.value = !_isSortedAlphabetically.value
    }

    @OptIn(ExperimentalResourceApi::class)
    private val jsonConfig = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @OptIn(ExperimentalResourceApi::class)
    fun initializeData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                println("XALABUS_DEBUG: Iniciando initializeData...")
                val mapBytes = Res.readBytes("files/xalapa.mbtiles")
                val internalPath = fileManager.saveMapFile("xalapa.mbtiles", mapBytes)

                if (repository.isDatabaseEmpty()) {
                    println("XALABUS_DEBUG: Base de datos vacía, cargando desde index.json...")

                    val indexBytes = Res.readBytes("files/routes/Xalapa/index.json")
                    val indexJson = indexBytes.decodeToString()
                    val index = jsonConfig.decodeFromString<List<com.example.xalabus.data.model.RouteJson>>(indexJson)

                    println("XALABUS_DEBUG: Índice cargado con ${index.size} rutas.")

                    index.forEach { indexItem ->
                        try {
                            val routePath = "files/routes/Xalapa/route_${indexItem.id}.json"
                            val routeBytes = Res.readBytes(routePath)
                            val routeData = jsonConfig.decodeFromString<com.example.xalabus.data.model.RouteJson>(routeBytes.decodeToString())
                            repository.insertSingleRoute(routeData)
                        } catch (e: Exception) {
                            println("XALABUS_DEBUG: Error cargando ruta ${indexItem.id}: ${e.message}")
                        }
                    }
                }

                val allRoutes = repository.getAllRoutes()
                println("XALABUS_DEBUG: Rutas en DB: ${allRoutes.size}")

                val processedRoutes = allRoutes.map { route ->
                    val finalName = if (route.name.contains("1000")) {
                        route.name.substringAfter("-").trim().ifEmpty { route.name }
                    } else {
                        route.name
                    }
                    route.copy(name = finalName)
                }

                launch(Dispatchers.Main) {
                    _mapFilePath.value = internalPath
                    _allRoutes.value = processedRoutes
                    _isDataLoaded.value = true
                }

            } catch (e: Exception) {
                println("XALABUS_DEBUG: Error crítico: ${e.message}")
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