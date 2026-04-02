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
     * Prepopula la base de datos solo con Rutas y sus Geometrías (Polylines).
     * Se eliminó el procesamiento de paradas para optimizar el rendimiento y evitar errores de mapeo.
     */
    suspend fun checkAndPrepopulate(routesJson: String) = withContext(Dispatchers.IO) {
        println("XALABUS_DEBUG: Iniciando carga de rutas...")
        try {
            val routes = jsonConfig.decodeFromString<List<RouteJson>>(routesJson)

            queries.transaction {
                val currentRoutes = queries.selectAllRoutes().executeAsList()

                if (currentRoutes.isEmpty()) {
                    println("XALABUS_DEBUG: DB vacía. Procesando ${routes.size} rutas...")

                    routes.forEach { route ->
                        val routeIdStr = route.id.toString()

                        // 1. Insertar metadatos de la ruta
                        queries.insertRoute(
                            id = routeIdStr,
                            name = route.desc ?: route.name,
                            fare = route.fare ?: "12.00",
                            frequency = route.frequency ?: "15 min"
                        )

                        // 2. Insertar las geometrías (Ida y Vuelta si existen)
                        route.variants.forEach { variant ->
                            queries.insertGeometry(
                                routeId = routeIdStr,
                                direction = variant.direction,
                                polyline = Json.encodeToString(variant.coords)
                            )
                        }
                    }
                }
            }
            println("XALABUS_DEBUG: Carga finalizada correctamente.")
        } catch (e: Exception) {
            println("XALABUS_DEBUG: ERROR EN PREPOPULATE -> ${e.message}")
        }
    }

    // --- Consultas ---

    fun getAllRoutes() = queries.selectAllRoutes().executeAsList()

    fun getGeometryForRoute(routeId: String) = queries.selectGeometryByRoute(routeId).executeAsList()

    /**
     * Mantenemos la función para futura implementación (dio mio)
     */
    // fun getStopsForRoute(routeId: String) = emptyList<com.example.xalabus.DBD.StopEntity>()
}