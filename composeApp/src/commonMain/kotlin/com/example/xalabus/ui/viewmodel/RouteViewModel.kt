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

class RouteViewModel(
    private val repository: RouteRepository,
    private val fileManager: MapFileManager
) : ViewModel() {

    // Estado para saber si la base de datos y el mapa están listos
    private val _isDataLoaded = MutableStateFlow(false)
    val isDataLoaded: StateFlow<Boolean> = _isDataLoaded

    // Ruta física del archivo .mbtiles en el almacenamiento interno
    private val _mapFilePath = MutableStateFlow<String?>(null)
    val mapFilePath: StateFlow<String?> = _mapFilePath

    // Lista de todas las rutas disponibles (para la HomeScreen)
    private val _routes = MutableStateFlow<List<RouteEntity>>(emptyList())
    val routes: StateFlow<List<RouteEntity>> = _routes

    // Puntos [Lng, Lat] de la ruta que el usuario seleccionó actualmente
    private val _selectedRoutePoints = MutableStateFlow<List<List<List<Double>>>>(emptyList())
    val selectedRoutePoints: StateFlow<List<List<List<Double>>>> = _selectedRoutePoints

    /**
     * Inicialización: Prepopula la DB y extrae el mapa offline.
     */
    @OptIn(ExperimentalResourceApi::class)
    fun initializeData() {
        viewModelScope.launch {
            try {
                // 1. Cargamos y procesamos el JSON de rutas (SQLDelight)
                val routeBytes = Res.readBytes("files/master_routes_optimized.json")
                repository.checkAndPrepopulate(routeBytes.decodeToString())

                // 2. Extraemos el mapa .mbtiles de los recursos al sistema de archivos
                // Esto es necesario porque MapLibre no lee archivos comprimidos dentro del APK
                val mapBytes = Res.readBytes("files/xalapa.mbtiles")
                val internalPath = fileManager.saveMapFile("xalapa.mbtiles", mapBytes)
                _mapFilePath.value = internalPath

                // 3. Actualizamos la lista de rutas para la UI
                _routes.value = repository.getAllRoutes()

                // Marcamos como listo para quitar el Loading de la App
                _isDataLoaded.value = true
            } catch (e: Exception) {
                println("Error crítico en InitializeData: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * Se llama cuando el usuario toca una ruta en la lista.
     * Recupera la geometría y actualiza el estado que el MapScreen observa.
     */
    fun selectRoute(routeId: String) {
        viewModelScope.launch {
            try {
                val points = getRoutePoints(routeId)
                _selectedRoutePoints.value = points
            } catch (e: Exception) {
                println("Error al seleccionar ruta $routeId: ${e.message}")
            }
        }
    }

    /**
     * Lógica interna para decodificar el String de la DB a una estructura de coordenadas.
     */
    private fun getRoutePoints(routeId: String): List<List<List<Double>>> {
        val geometries = repository.getGeometryForRoute(routeId)

        return geometries.map { geometryEntity ->
            // Convertimos el JSON String (la polyline) a una lista real de coordenadas [Lng, Lat]
            Json.decodeFromString<List<List<Double>>>(geometryEntity.polyline)
        }
    }
}