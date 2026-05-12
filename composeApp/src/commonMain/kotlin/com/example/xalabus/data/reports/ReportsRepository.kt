package com.example.xalabus.data.reports

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

class ReportsRepository {
    private val client = SupabaseClientProvider.client

    // --- Funciones para Usuarios ---

    suspend fun sendGeneralReport(report: GeneralReport) {
        client.postgrest["general_reports"]
            .insert(report)
    }

    suspend fun sendRouteReport(report: RouteReport) {
        client.postgrest["route_reports"]
            .insert(report)
    }

    suspend fun suggestRouteStop(stop: RouteStop) {
        client.postgrest["route_stops"]
            .insert(stop)
    }

    // --- Funciones para el Administrador ---

    /**
     * Obtiene todas las paradas que están pendientes de revisión.
     */
    suspend fun getPendingStops(): List<RouteStop> {
        return client.postgrest["route_stops"]
            .select {
                filter {
                    eq("status", "pending")
                }
            }
            .decodeList<RouteStop>()
    }

    /**
     * Actualiza el estado y la descripción de una parada (Aprobar/Rechazar).
     */
    suspend fun updateStopStatus(stopId: String, newStatus: String, newDescription: String) {
        // En Supabase/Postgrest necesitamos crear una clase auxiliar o usar map para el update
        @kotlinx.serialization.Serializable
        data class StopUpdate(val status: String, val description: String)
        
        client.postgrest["route_stops"]
            .update(StopUpdate(newStatus, newDescription)) {
                filter {
                    eq("id", stopId)
                }
            }
    }
}
