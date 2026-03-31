package com.example.xalabus.data.repository

import com.example.xalabus.DBD.AppDatabase
import com.example.xalabus.data.model.RouteJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class RouteRepository(private val db: AppDatabase) {
    private val queries = db.appDatabaseQueries

    suspend fun checkAndPrepopulate(jsonString: String) = withContext(Dispatchers.IO) {
        try {
            queries.transaction {
                // Solo insertamos si la base de datos está vacía
                if (queries.selectAllRoutes().executeAsList().isEmpty()) {
                    val routes = Json.decodeFromString<List<RouteJson>>(jsonString)

                    routes.forEach { route ->
                        // LÓGICA DE INTERCAMBIO: Priorizamos 'desc' sobre 'name'
                        val displayName = if (!route.desc.isNullOrBlank()) {
                            route.desc
                        } else {
                            route.name
                        }

                        // Insertamos en la tabla de rutas
                        queries.insertRoute(
                            id = route.id.toString(),
                            name = displayName
                        )

                        // Insertamos las variantes (geometrías)
                        route.variants.forEach { variant ->
                            queries.insertGeometry(
                                routeId = route.id.toString(),
                                direction = variant.direction,
                                polyline = Json.encodeToString(variant.coords)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            println("Error en la transacción SQL: ${e.message}")
            throw e
        }
    }

    // Retorna todas las rutas para la lista del Home
    fun getAllRoutes() = queries.selectAllRoutes().executeAsList()

    // Retorna las geometrías de una ruta específica para el mapa
    fun getGeometryForRoute(routeId: String) =
        queries.selectGeometryByRoute(routeId).executeAsList()
}