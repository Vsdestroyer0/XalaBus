package com.example.xalabus.data.incidentes

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repositorio de incidentes — CU-13.
 * Opera sobre la tabla `reportes` en Supabase.
 */
class IncidentesRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Registra un nuevo reporte de incidente con coordenadas GPS.
     * FA-01: punto inválido → validar lat/lng antes de llamar.
     * FA-02: texto vacío → validar descripcion antes de llamar.
     * Ex-01: error al guardar → lanza excepción, capturar en ViewModel.
     */
    suspend fun reportarIncidente(incidente: Incidente) {
        client.postgrest["reportes"].insert(incidente)
    }

    /** Obtiene todos los reportes con estado 'pendiente' (para mostrar en mapa). */
    suspend fun getIncidentesPendientes(): List<Incidente> {
        return client.postgrest["reportes"]
            .select { filter { eq("estado", "pendiente") } }
            .decodeList<Incidente>()
    }
}
