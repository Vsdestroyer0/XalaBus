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

    private val _selectedRouteId = MutableStateFlow("")
    val selectedRouteId: StateFlow<String> = _selectedRouteId

    private val _selectedRoutePoints = MutableStateFlow<List<List<List<Double>>>>(emptyList())
    val selectedRoutePoints: StateFlow<List<List<List<Double>>>> = _selectedRoutePoints

    // 1. El texto que el usuario escribe en la barra
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // NUEVO: 2. Estado para saber si el orden alfabético está activo
    private val _isSortedAlphabetically = MutableStateFlow(false)
    val isSortedAlphabetically: StateFlow<Boolean> = _isSortedAlphabetically

    // NUEVO: 3. Estado para el filtro de zona
    private val _selectedZone = MutableStateFlow<String?>(null)
    val selectedZone: StateFlow<String?> = _selectedZone

    val availableZones = listOf(
        "20 de Noviembre", "Araucarias", "Arco Sur", "Articulo 3ro", "Av. Xalapa", "Avila Camacho",
        "Berros", "Buena Vista", "Bugambilias", "Calvario", "Campo de Tiro", "Carolino Anaya",
        "Casa Blanca", "Caxa", "Central de Abastos", "Centro", "Clavijero", "Coapexpan", "Estación",
        "Fovissste", "Hernandez Castillo", "IMSS", "Ignacio de la Llave", "Instituto Tecnológico",
        "Jardines", "Lázaro Cárdenas", "Lomas Verdes", "Luz del Barrio", "Mercado", "Murillo Vidal",
        "Ojo de agua", "Pastoresa", "Pipila", "Plaza Crystal", "Rebsamen", "Revolución", "Ruiz Cortines",
        "SEFIPLAN", "San Andrés Tlalnehuayocan", "San Antonio", "Sauces", "Sayago", "Sumidero",
        "Tecnica 72", "Torre Ánimas", "Trancas", "Villa Hermosa", "Xalapa", "Xalapa 2000",
        "Xocotla", "Zona UV"
    )

    // 3. La lista COMPLETA de rutas que viene de la base de datos
    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())

    // 4. La lista FILTRADA que la vista realmente observa y muestra
    // NUEVO: Agregamos _isSortedAlphabetically y _selectedZone al combine
    val filteredRoutes: StateFlow<List<RouteEntity>> = combine(
        _allRoutes,
        _searchQuery,
        _isSortedAlphabetically,
        _selectedZone
    ) { routes, query, isSorted, zone ->

        // Primero filtramos por zona (usando la descripción o el nombre)
        val zoneFiltered = if (zone.isNullOrBlank()) {
            routes
        } else {
            routes.filter { route ->
                // Las propiedades name o id pueden estar en la clase. Para la descripción usamos name por ahora o si tiene desc (la entidad tiene description? No lo vimos. Veamos la entidad, wait).
                // Ah, RouteEntity solo tiene id, name, fare, fareStudent, fareInapan, frequency. No se guarda la descripción en la base de datos!
                // Wait! RouteEntity no tiene descripción! Let me check this.
                route.name.contains(zone, ignoreCase = true)
            }
        }

        // Segundo filtramos por la búsqueda de texto
        val resultList = if (query.isBlank()) {
            zoneFiltered
        } else {
            val searchTokens = query.trim().split("\\s+".toRegex())
            zoneFiltered.filter { route ->
                searchTokens.all { token ->
                    route.id.contains(token, ignoreCase = true) ||
                            route.name.contains(token, ignoreCase = true)
                }
            }
        }

        // Tercero filtramos por el ordenamiento alfabético
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

    // NUEVO: Función para seleccionar una zona
    fun selectZone(zone: String?) {
        _selectedZone.value = zone
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
                _selectedRouteId.value = routeId

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

    // --- NUEVO: Funcionalidad para GPS y Mejor Ruta ---
    private val _userLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val userLocation: StateFlow<Pair<Double, Double>?> = _userLocation

    private val _destinationLocation = MutableStateFlow<Pair<Double, Double>?>(null)
    val destinationLocation: StateFlow<Pair<Double, Double>?> = _destinationLocation

    fun setUserLocation(lat: Double, lng: Double) {
        _userLocation.value = Pair(lat, lng)
    }

    fun setDestinationLocation(lat: Double, lng: Double?) {
        if (lat == null || lng == null) {
            _destinationLocation.value = null
        } else {
            _destinationLocation.value = Pair(lat, lng)
        }
    }

    private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0 // Radio de la Tierra en km
        val dLat = (lat2 - lat1) * kotlin.math.PI / 180.0
        val dLon = (lon2 - lon1) * kotlin.math.PI / 180.0
        val a = kotlin.math.sin(dLat / 2) * kotlin.math.sin(dLat / 2) +
                kotlin.math.cos(lat1 * kotlin.math.PI / 180.0) * kotlin.math.cos(lat2 * kotlin.math.PI / 180.0) *
                kotlin.math.sin(dLon / 2) * kotlin.math.sin(dLon / 2)
        val c = 2 * kotlin.math.atan2(kotlin.math.sqrt(a), kotlin.math.sqrt(1 - a))
        return r * c
    }

    fun calculateBestRoute() {
        val userLoc = _userLocation.value ?: return
        val destLoc = _destinationLocation.value ?: return

        viewModelScope.launch(Dispatchers.IO) {
            var bestRouteId: String? = null
            var minScore = Double.MAX_VALUE

            _allRoutes.value.forEach { route ->
                val points = getRoutePoints(route.id)
                if (points.isNotEmpty()) {
                    val flatPoints = points.flatten()
                    
                    var minUserDist = Double.MAX_VALUE
                    var minDestDist = Double.MAX_VALUE

                    flatPoints.forEach { point ->
                        if (point.size >= 2) {
                            val lng = point[0]
                            val lat = point[1]
                            
                            val userDist = calculateDistance(userLoc.first, userLoc.second, lat, lng)
                            if (userDist < minUserDist) minUserDist = userDist

                            val destDist = calculateDistance(destLoc.first, destLoc.second, lat, lng)
                            if (destDist < minDestDist) minDestDist = destDist
                        }
                    }

                    // Se suma la distancia desde el usuario a la ruta + distancia de la ruta al destino
                    val score = minUserDist + minDestDist
                    if (score < minScore) {
                        minScore = score
                        bestRouteId = route.id
                    }
                }
            }

            bestRouteId?.let { 
                launch(Dispatchers.Main) {
                    selectRoute(it)
                }
            }
        }
    }
}