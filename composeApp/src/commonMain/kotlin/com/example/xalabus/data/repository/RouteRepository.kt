package com.example.xalabus.data.repository

import com.example.xalabus.DBD.AppDatabase
import com.example.xalabus.data.model.RouteJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class RouteRepository(private val db: AppDatabase) {
    private val queries = db.appDatabaseQueries
    private val jsonConfig = Json { ignoreUnknownKeys = true }

    /**
     * Inserta una ruta individual y sus geometrías.
     */
    fun insertSingleRoute(route: RouteJson) {
        val routeIdStr = route.id.toString()
        
        // 1. Insertar metadatos
        queries.insertRoute(
            id = routeIdStr,
            name = route.desc ?: route.name,
            fare = route.fare ?: "12.00",
            fareStudent = route.fareStudent ?: "7.00",
            fareInapan = route.fareInapan ?: "7.00",
            frequency = route.frequency ?: "15 min"
        )

        // 2. Insertar las geometrías
        route.variants.forEach { variant ->
            queries.insertGeometry(
                routeId = routeIdStr,
                direction = variant.direction,
                polyline = Json.encodeToString(variant.coords)
            )
        }
    }

    /**
     * Verifica si la base de datos está vacía.
     */
    suspend fun isDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        queries.selectAllRoutes().executeAsList().isEmpty()
    }

    // --- Consultas ---

    fun getAllRoutes() = queries.selectAllRoutes().executeAsList()

    fun getGeometryForRoute(routeId: String) = queries.selectGeometryByRoute(routeId).executeAsList()

    /**
     * Mantenemos la función para futura implementación (dio mio)
     */
    // fun getStopsForRoute(routeId: String) = emptyList<com.example.xalabus.DBD.StopEntity>()
}