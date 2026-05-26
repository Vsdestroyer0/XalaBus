package com.example.xalabus.data.paradas

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

/**
 * Repositorio de paradas — CU-12 y gestión admin.
 * Opera sobre la tabla `paradas` en Supabase.
 * Solo el admin puede insertar/modificar; todos pueden leer.
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

    // ───────────────────────────────────────────────────────────────────────────
    // Métodos de gestión admin (requieren campo `estado` en la tabla)
    // ───────────────────────────────────────────────────────────────────────────

    /**
     * Retorna paradas con estado = 'pendiente' para revisión del admin.
     * Requiere que la columna `estado` exista en la tabla `paradas` de Supabase.
     */
    suspend fun getPendingParadas(): List<Parada> {
        return client.postgrest["paradas"]
            .select { filter { eq("estado", "pendiente") } }
            .decodeList<Parada>()
    }

    /**
     * Aprueba una parada sugerida cambiando su estado a 'aprobada'.
     * Postcondición: la parada queda visible para todos los usuarios.
     */
    suspend fun approveParada(parada: Parada) {
        val id = parada.id ?: return
        client.postgrest["paradas"]
            .update({ set("estado", "aprobada") }) {
                filter { eq("id", id) }
            }
    }

    /**
     * Rechaza y elimina una parada sugerida.
     * Postcondición: la parada deja de aparecer en la app.
     */
    suspend fun rejectParada(parada: Parada) {
        val id = parada.id ?: return
        client.postgrest["paradas"]
            .delete { filter { eq("id", id) } }
    }
}
