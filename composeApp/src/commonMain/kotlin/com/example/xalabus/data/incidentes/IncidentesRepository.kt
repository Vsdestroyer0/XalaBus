package com.example.xalabus.data.incidentes

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.DateTimePeriod
import kotlinx.datetime.minus

/**
 * Repositorio de incidentes — CU-13.
 * Opera sobre la tabla `reportes` en Supabase.
 */
class IncidentesRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Registra un nuevo reporte de incidente con coordenadas GPS.
     * FA-01: punto inválido → validar lat/lng antes de llamar.
     * FA-02: texto vacío  → validar descripcion antes de llamar.
     * Ex-01: error al guardar → lanza excepción, capturar en ViewModel.
     */
    suspend fun reportarIncidente(incidente: Incidente) {
        client.postgrest["reportes"].insert(incidente)
    }

    /**
     * CU-13: devuelve los incidentes creados en las últimas 4 horas.
     * El filtro se hace en Supabase con gt(created_at, ahora-4h)
     * para que solo sean visibles en el mapa mientras están activos.
     */
    suspend fun getIncidentesActivos(): List<Incidente> {
        val hace4Horas = Clock.System.now()
            .minus(DateTimePeriod(hours = 4), TimeZone.currentSystemDefault())
            .toString() // ISO-8601: "2026-05-27T13:00:00Z"

        return client.postgrest["reportes"]
            .select {
                filter {
                    gt("created_at", hace4Horas)
                }
            }
            .decodeList<Incidente>()
    }
}
