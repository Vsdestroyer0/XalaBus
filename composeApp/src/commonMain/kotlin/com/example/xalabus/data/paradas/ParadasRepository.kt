package com.example.xalabus.data.paradas

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repositorio de paradas — CU-12 y gestión admin.
 * Opera sobre la tabla `paradas` en Supabase.
 */
class ParadasRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Agrega una nueva parada de camión con coordenadas GPS.
     * FA-01: datos incompletos → validar antes de llamar este método.
     * FA-02: parada duplicada por coordenadas cercanas → se verifica con getNearbyParadas.
     */
    suspend fun addParada(parada: Parada) {
        client.postgrest["paradas"].insert(parada)
    }

    /** Obtiene todas las paradas de una ruta específica. */
    suspend fun getParadasByRuta(rutaId: String): List<Parada> {
        return client.postgrest["paradas"]
            .select { filter { eq("ruta_id", rutaId) } }
            .decodeList<Parada>()
    }

    /**
     * Busca paradas cercanas a unas coordenadas dadas (radio ~100 metros).
     * Se usa para detectar duplicados antes de insertar (FA-02).
     */
    suspend fun getNearbyParadas(lat: Double, lng: Double, deltaGrados: Double = 0.001): List<Parada> {
        return client.postgrest["paradas"]
            .select {
                filter {
                    gte("latitud", lat - deltaGrados)
                    lte("latitud", lat + deltaGrados)
                    gte("longitud", lng - deltaGrados)
                    lte("longitud", lng + deltaGrados)
                }
            }
            .decodeList<Parada>()
    }

    // ── Métodos de gestión admin ──────────────────────────────────────────────

    /**
     * Retorna todas las paradas registradas para que el admin las gestione.
     * (Sin filtro de estado — la tabla no tiene esa columna.)
     */
    suspend fun getPendingParadas(): List<Parada> {
        return client.postgrest["paradas"]
            .select()
            .decodeList<Parada>()
    }

    /**
     * "Aprobar" una parada no requiere cambio de estado en este schema.
     * Solo recarga la lista (no-op en BD, el ViewModel llama fetchPendingStops tras esto).
     */
    suspend fun approveParada(parada: Parada) {
        // No-op: sin columna estado no hay nada que actualizar.
        // El ViewModel refrescará la lista automáticamente.
    }

    /**
     * Elimina una parada de la BD.
     */
    suspend fun rejectParada(parada: Parada) {
        val id = parada.id ?: return
        client.postgrest["paradas"]
            .delete { filter { eq("id", id) } }
    }
}
