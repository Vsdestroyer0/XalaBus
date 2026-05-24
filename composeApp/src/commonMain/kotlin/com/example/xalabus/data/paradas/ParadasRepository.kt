package com.example.xalabus.data.paradas

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlin.math.abs

/**
 * CU-12: Repositorio para operaciones sobre la tabla `paradas` en Supabase.
 *
 * Incluye detección de paradas duplicadas por proximidad (FA-02):
 * dos paradas se consideran duplicadas si están a menos de [DUPLICATE_THRESHOLD_DEGREES].
 */
class ParadasRepository {
    private val client = SupabaseClientProvider.client

    /**
     * Umbral en grados decimales para considerar dos paradas como "misma ubicación".
     * ~0.0001° ≈ 11 metros (suficiente para camión urbano)
     */
    private val DUPLICATE_THRESHOLD_DEGREES = 0.0002

    /**
     * Inserta una nueva parada en Supabase.
     * Asume que la validación de duplicados ya se realizó con [findNearbyParada].
     */
    suspend fun insertParada(parada: Parada) {
        client.postgrest["paradas"].insert(parada)
    }

    /**
     * Busca si ya existe una parada con coordenadas muy cercanas a las dadas (FA-02).
     * @return La parada duplicada encontrada, o null si no hay duplicados.
     */
    suspend fun findNearbyParada(latitud: Double, longitud: Double): Parada? {
        // Rango de búsqueda en la BD (ampliar el rango para traer candidatos)
        val latMin = latitud  - DUPLICATE_THRESHOLD_DEGREES * 5
        val latMax = latitud  + DUPLICATE_THRESHOLD_DEGREES * 5
        val lonMin = longitud - DUPLICATE_THRESHOLD_DEGREES * 5
        val lonMax = longitud + DUPLICATE_THRESHOLD_DEGREES * 5

        val candidates = client.postgrest["paradas"]
            .select {
                filter {
                    gte("latitud",  latMin)
                    lte("latitud",  latMax)
                    gte("longitud", lonMin)
                    lte("longitud", lonMax)
                }
            }
            .decodeList<Parada>()

        // Filtra con el umbral preciso usando distancia Euclidiana en grados
        return candidates.firstOrNull { p ->
            abs(p.latitud  - latitud)  < DUPLICATE_THRESHOLD_DEGREES &&
            abs(p.longitud - longitud) < DUPLICATE_THRESHOLD_DEGREES
        }
    }

    /** Obtiene todas las paradas de una ruta */
    suspend fun getParadasByRoute(rutaId: String): List<Parada> {
        return client.postgrest["paradas"]
            .select {
                filter { eq("ruta_id", rutaId) }
            }
            .decodeList<Parada>()
    }
}
