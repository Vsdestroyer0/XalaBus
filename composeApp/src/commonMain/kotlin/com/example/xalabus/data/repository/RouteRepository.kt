package com.example.xalabus.data.repository

import com.example.xalabus.database.AppDatabase
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

class RouteRepository(private val db: AppDatabase) {
    private val queries = db.appDatabaseQueries

    suspend fun checkAndPrepopulate(jsonString: String) {
        // Solo insertamos si la base de datos está vacía
        if (queries.selectAllRoutes().executeAsList().isEmpty()) {
            val routes = Json.decodeFromString<List<RouteJson>>(jsonString)

            queries.transaction {
                routes.forEach { route ->
                    queries.insertRoute(route.id, route.name)

                    route.variants.forEach { variant ->
                        queries.insertGeometry(
                            routeId = route.id,
                            direction = variant.direction,
                            polyline = Json.encodeToString(variant.coords)
                        )
                    }
                }
            }
            println("XalaBus: Base de datos poblada con ${routes.size} rutas.")
        }
    }

    fun getAllRoutes() = queries.selectAllRoutes().executeAsList()
}