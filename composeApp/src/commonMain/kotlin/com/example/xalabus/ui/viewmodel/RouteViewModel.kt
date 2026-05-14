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
import kotlinx.coroutines.flow.map

/** Normaliza un texto para comparaciones: sin acentos, sin espacios, minúsculas y elimina palabras genéricas. */
private fun String.normalizeZone(): String {
    var text = this.trim().lowercase()

    // 1. ELIMINAR TEXTO ENTRE PARÉNTESIS (Borra por completo "(economía)")
    text = text.replace(Regex("\\(.*?\\)"), "")

    // 2. Quitar signos de puntuación restantes
    text = text.replace(Regex("[.,;:]"), " ")

    // 3. Reemplazos de acentos y Unicode
    text = text.replace("á", "a").replace("é", "e").replace("í", "i")
        .replace("ó", "o").replace("ú", "u").replace("ü", "u")
        .replace("à", "a").replace("è", "e").replace("ì", "i")
        .replace("ò", "o").replace("ù", "u")
        .replace("ǭ", "a").replace("ǭ", "o")
        .replace("ǘ", "u").replace("ǖ", "u").replace("ǚ", "u").replace("ǜ", "u")
        .replace("ā", "a").replace("ē", "e").replace("ī", "i").replace("ō", "o").replace("ū", "u")
        .replace("ă", "a").replace("ĕ", "e").replace("ĭ", "i").replace("ŏ", "o").replace("ŭ", "u")
        .replace("ã", "a").replace("õ", "o").replace("ñ", "n")

    // 4. Eliminar caracteres no ASCII
    text = text.map { if (it.code > 127) '?' else it }.joinToString("")
        .replace("?", "")

    // 5. LIMPIEZA AGRESIVA (La clave para agrupar)
    // En lugar de estandarizar, ELIMINAMOS las palabras de calle para quedarnos con el nombre puro
    text = text.replace(Regex("\\b(av|avenida|col|colonia|calle|fracc|fraccionamiento|prolongacion|blvd|bulevar)\\b"), "")
    
    // Quitar conectores
    text = text.replace(Regex("\\b(de|del|las|los|la|el)\\b"), "")

    // 6. Limpieza final de espacios extra
    return text.replace("\\s+".toRegex(), " ").trim()
}

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

    // 1. El texto que el usuario escribe en la barra general
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    // NUEVO: Puntos de inicio y fin para filtrar por zonas
    private val _startZoneQuery = MutableStateFlow("")
    val startZoneQuery: StateFlow<String> = _startZoneQuery

    private val _endZoneQuery = MutableStateFlow("")
    val endZoneQuery: StateFlow<String> = _endZoneQuery

    // NUEVO: 2. Estado para saber si el orden alfabético está activo
    private val _isSortedAlphabetically = MutableStateFlow(false)
    val isSortedAlphabetically: StateFlow<Boolean> = _isSortedAlphabetically

    // 3. La lista COMPLETA de rutas que viene de la base de datos
    private val _allRoutes = MutableStateFlow<List<RouteEntity>>(emptyList())

    // Zonas únicas dinámicas extraídas de los nombres de las rutas
    val uniqueZones: StateFlow<List<String>> = _allRoutes.map { routes ->
        val seen = mutableSetOf<String>()
        val unique = mutableListOf<String>()

        val rutaNumPattern = Regex("^ruta\\s+\\d+$", RegexOption.IGNORE_CASE)

        routes.flatMap { route ->
            route.name.split("-").map { it.trim() }
        }.filter { it.isNotBlank() }.forEach { rawZone ->
            
            // 1. LLAVE SECRETA: Usamos la limpieza agresiva SOLO como llave interna para no repetir 
            // (Esto junta "Av Xalapa (economía)" con "Avenida Xalapa" bajo la llave secreta "xalapa")
            val key = rawZone.normalizeZone() 
            if (key.isBlank() || rutaNumPattern.matches(key)) return@forEach
            
            if (seen.add(key)) {
                // 2. NOMBRE VISUAL: Para mostrar en la lista usamos el texto original
                // Primero le quitamos cosas feas como "(economía)"
                var displayName = rawZone.replace(Regex("\\(.*?\\)"), "").trim()
                
                // Si alguien lo guardó en la DB como "Av." o "Av", lo ponemos formal como "Avenida"
                displayName = displayName.replace(Regex("^Av\\.?\\s+", RegexOption.IGNORE_CASE), "Avenida ")
                
                // Capitalizamos cada palabra para que se vea muy profesional
                displayName = displayName.split(" ").joinToString(" ") { word ->
                    word.replaceFirstChar { it.uppercase() }
                }
                
                unique.add(displayName)
            }
        }

        listOf("Mi ubicación") + unique.sorted()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // 4. La lista FILTRADA que la vista realmente observa y muestra
    val filteredRoutes: StateFlow<List<RouteEntity>> = combine(
        _allRoutes,
        combine(_searchQuery, _startZoneQuery, _endZoneQuery) { q, start, end -> Triple(q, start, end) },
        _isSortedAlphabetically
    ) { routes, queries, isSorted ->
        val (query, start, end) = queries
        
        var resultList = routes

        // Filtro por búsqueda general
        if (query.isNotBlank()) {
            val searchTokens = query.trim().split("\\s+".toRegex())
            resultList = resultList.filter { route ->
                searchTokens.all { token ->
                    route.id.contains(token, ignoreCase = true) ||
                            route.name.contains(token, ignoreCase = true)
                }
            }
        }

        // Filtro por zona de inicio (normalizado para ignorar tildes y mayúsculas)
        if (start.isNotBlank() && start != "Mi ubicación") {
            val startNorm = start.normalizeZone()
            resultList = resultList.filter { route ->
                route.name.split("-").any { segment ->
                    segment.normalizeZone().contains(startNorm)
                }
            }
        }

        // Filtro por zona de fin (normalizado para ignorar tildes y mayúsculas)
        if (end.isNotBlank() && end != "Mi ubicación") {
            val endNorm = end.normalizeZone()
            resultList = resultList.filter { route ->
                route.name.split("-").any { segment ->
                    segment.normalizeZone().contains(endNorm)
                }
            }
        }

        // Ordenamiento alfabético
        if (isSorted) {
            resultList.sortedBy { it.name }
        } else {
            resultList 
        }

    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Lazily,
        initialValue = emptyList()
    )

    // Funciones que se llaman cuando el usuario teclea
    fun onSearchQueryChanged(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun onStartZoneQueryChanged(newQuery: String) {
        _startZoneQuery.value = newQuery
    }

    fun onEndZoneQueryChanged(newQuery: String) {
        _endZoneQuery.value = newQuery
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

                val isEmpty = repository.isDatabaseEmpty()
                val isStale = !isEmpty && repository.hasStaleData()

                if (isEmpty || isStale) {
                    if (isStale) {
                        println("XALABUS_DEBUG: Datos stale detectados (nombres 'Ruta XXXXX'). Limpiando BD...")
                        repository.clearAllData()
                    } else {
                        println("XALABUS_DEBUG: Base de datos vacía, cargando desde index.json...")
                    }

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
                            // Si no hay archivo de ruta individual, insertar solo los metadatos del index
                            repository.insertSingleRoute(indexItem)
                            println("XALABUS_DEBUG: Usando datos del index para ruta ${indexItem.id}")
                        }
                    }
                }

                val allRoutes = repository.getAllRoutes()
                println("XALABUS_DEBUG: Rutas en DB: ${allRoutes.size}")

                launch(Dispatchers.Main) {
                    _mapFilePath.value = internalPath
                    _allRoutes.value = allRoutes
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