package com.example.xalabus.data.reports

import com.example.xalabus.core.util.StopProximityHelper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns

class ReportsRepository {
    private val client = SupabaseClientProvider.client

    suspend fun sendGeneralReport(report: GeneralReport) {
        client.postgrest["general_reports"].insert(report)
    }

    suspend fun sendRouteReport(report: RouteReport) {
        client.postgrest["route_reports"].insert(report)
    }

    suspend fun suggestRouteStop(stop: RouteStop) {
        client.postgrest["route_stops"].insert(stop)
    }

    /**
     * Paradas de una ruta (pendientes y aprobadas) para proximidad y sincronización.
     */
    suspend fun getStopsForRoute(routeId: Int): List<RouteStop> {
        return client.postgrest["route_stops"]
            .select(Columns.ALL) {
                filter { eq("route_id", routeId) }
            }
            .decodeList<RouteStop>()
    }

    /** Paradas aceptadas por el administrador (status = 'accepted' en Supabase). */
    suspend fun getAcceptedStopsForRoute(routeId: Int): List<RouteStop> {
        return client.postgrest["route_stops"]
            .select(Columns.ALL) {
                filter {
                    eq("route_id", routeId)
                    eq("status", "accepted")
                }
            }
            .decodeList<RouteStop>()
    }

    suspend fun findNearbyStop(routeId: Int, lat: Double, lng: Double): RouteStop? {
        val stops = getStopsForRoute(routeId)
        return StopProximityHelper.findNearbyRemoteStop(stops, lat, lng)
    }

    suspend fun incrementPopularity(stopId: String, currentPopularity: Int) {
        client.postgrest["route_stops"]
            .update(PopularityUpdate(currentPopularity + 1)) {
                filter { eq("id", stopId) }
            }
    }

    suspend fun getPendingStops(): List<RouteStop> {
        return client.postgrest["route_stops"]
            .select(Columns.ALL) {
                filter { eq("status", "pending") }
            }
            .decodeList<RouteStop>()
    }

    suspend fun updateStopStatus(stopId: String, newStatus: String, newDescription: String) {
        @kotlinx.serialization.Serializable
        data class StopUpdate(val status: String, val description: String)

        client.postgrest["route_stops"]
            .update(StopUpdate(newStatus, newDescription)) {
                filter { eq("id", stopId) }
            }
    }
}
