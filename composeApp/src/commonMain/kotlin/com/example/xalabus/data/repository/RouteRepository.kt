package com.example.xalabus.data.repository

import com.example.xalabus.DBD.AppDatabase
import com.example.xalabus.DBD.RouteEntity
import com.example.xalabus.DBD.StopEntity
import com.example.xalabus.data.model.RouteJson
import com.example.xalabus.data.reports.RouteStop
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

        queries.insertRoute(
            id = routeIdStr,
            name = route.desc ?: route.name,
            fare = route.fare ?: "12.00",
            fareStudent = route.fareStudent ?: "7.00",
            fareInapan = route.fareInapan ?: "7.00",
            frequency = route.frequency ?: "15 min"
        )

        route.variants.forEach { variant ->
            queries.insertGeometry(
                routeId = routeIdStr,
                direction = variant.direction,
                polyline = Json.encodeToString(variant.coords)
            )
        }
    }

    suspend fun isDatabaseEmpty(): Boolean = withContext(Dispatchers.IO) {
        queries.selectAllRoutes().executeAsList().isEmpty()
    }

    fun getAllRoutes() = queries.selectAllRoutes().executeAsList()

    fun getGeometryForRoute(routeId: String) = queries.selectGeometryByRoute(routeId).executeAsList()

    // ── CU-09: paradas locales ───────────────────────────────────────────────

    fun getStopsForRoute(routeId: String): List<StopEntity> =
        queries.selectStopsByRoute(routeId).executeAsList()

    fun insertLocalStop(stop: RouteStop) {
        val id = stop.id ?: return
        queries.insertStop(
            id = id,
            routeId = stop.routeId.toString(),
            lat = stop.latitude,
            lng = stop.longitude,
            popularity = stop.popularity.toLong(),
            description = stop.description
        )
    }

    fun incrementLocalStopPopularity(stopId: String) {
        queries.incrementStopPopularity(stopId)
    }

    suspend fun replaceStopsForRoute(routeId: String, stops: List<RouteStop>) = withContext(Dispatchers.IO) {
        queries.deleteStopsByRoute(routeId)
        stops.filter { it.status == "accepted" && it.id != null }.forEach { insertLocalStop(it) }
    }

    // ── CU- Historial ────────────────────────────────────────────────────────

    fun saveRouteToHistory(routeId: String) {
        val currentTime = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        queries.insertHistory(routeId, currentTime)
        queries.deleteOldHistory()
    }

    fun getRouteHistory(): List<RouteEntity> = queries.selectHistory().executeAsList()
}
