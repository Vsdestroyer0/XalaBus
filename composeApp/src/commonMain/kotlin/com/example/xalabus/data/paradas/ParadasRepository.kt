package com.example.xalabus.data.paradas

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest

/**
 * Repositorio de paradas — CU-12.
 * Opera sobre la tabla `paradas` en Supabase.
 * Solo el admin puede insertar; todos pueden leer.
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
}
