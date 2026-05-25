package com.example.xalabus.core.util

import kotlin.math.*

/**
 * CU-11: Utilidad para estimar el tiempo total de traslado en una ruta de camión.
 *
 * Lógica:
 *   - Velocidad promedio urbana de 30 km/h para camiones en Xalapa.
 *   - La distancia se calcula con la fórmula de Haversine entre paradas consecutivas.
 *   - Por cada parada intermedia que hace el camión se suman 1 minuto adicional.
 *
 * Fórmula:
 *   tiempo = (distancia_km / 30) * 60  +  (paradas_intermedias * 1 min)
 *
 * "Paradas intermedias" = todas las paradas del trayecto EXCEPTO la parada de origen
 * (el usuario ya está ahí, no espera en ella).
 */
object TravelTimeEstimator {

    /** Velocidad promedio urbana del camión en km/h */
    const val AVERAGE_SPEED_KMH = 30.0

    /** Minutos que el camión permanece en cada parada */
    const val STOP_DURATION_MINUTES = 1

    /** Radio de la Tierra en kilómetros */
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calcula la distancia en kilómetros entre dos puntos GPS usando Haversine.
     */
    fun haversineDistanceKm(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) *
                cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }

    /**
     * Estima el tiempo de traslado en minutos para la lista COMPLETA de paradas
     * (de la primera a la última), sumando 1 min por cada parada intermedia.
     *
     * @param stops Lista de pares (latitud, longitud) en orden de la ruta.
     * @return Tiempo estimado en minutos, o null si la lista tiene < 2 paradas.
     */
    fun estimateMinutes(stops: List<Pair<Double, Double>>): Int? {
        if (stops.size < 2) return null
        return estimateMinutesForSegment(stops, 0, stops.size - 1)
    }

    /**
     * Sobrecarga: acepta listas separadas de latitudes y longitudes.
     */
    fun estimateMinutes(latitudes: List<Double>, longitudes: List<Double>): Int? {
        if (latitudes.size != longitudes.size) return null
        return estimateMinutes(latitudes.zip(longitudes))
    }

    /**
     * CU-11 (selector de tramo): Estima el tiempo entre la parada [fromIndex] y
     * la parada [toIndex] dentro de la lista ordenada de paradas de la ruta.
     *
     * Suma:
     *   1. Distancia Haversine acumulada entre paradas consecutivas del tramo.
     *   2. 1 minuto por cada parada intermedia (todas menos la de origen).
     *
     * Ejemplo: paradas [A, B, C, D], de A→D:
     *   - Distancia: A→B + B→C + C→D
     *   - Paradas intermedias: B, C, D = 3 paradas × 1 min = 3 min
     *
     * @param stops       Lista completa de paradas de la ruta, en orden.
     * @param fromIndex   Índice de la parada de origen (0-based).
     * @param toIndex     Índice de la parada de destino (0-based, debe ser > fromIndex).
     * @return Tiempo estimado en minutos, o null si los índices son inválidos.
     */
    fun estimateMinutesForSegment(
        stops: List<Pair<Double, Double>>,
        fromIndex: Int,
        toIndex: Int
    ): Int? {
        if (stops.size < 2) return null
        if (fromIndex < 0 || toIndex >= stops.size) return null
        if (fromIndex >= toIndex) return null

        // 1. Distancia acumulada entre paradas consecutivas del tramo
        var totalKm = 0.0
        for (i in fromIndex until toIndex) {
            val (lat1, lon1) = stops[i]
            val (lat2, lon2) = stops[i + 1]
            totalKm += haversineDistanceKm(lat1, lon1, lat2, lon2)
        }

        // 2. Tiempo de viaje puro (distancia / velocidad → minutos)
        val travelMinutes = (totalKm / AVERAGE_SPEED_KMH) * 60.0

        // 3. Paradas intermedias = todas las del tramo excepto la de origen
        //    (el usuario ya está en fromIndex, no espera ahí)
        val intermediateStops = toIndex - fromIndex  // = número de paradas B, C, ... destino
        val stopMinutes = intermediateStops * STOP_DURATION_MINUTES

        return (travelMinutes + stopMinutes).roundToInt()
    }

    /** Formatea el tiempo estimado para mostrar en la UI (ej. "25 min" o "1 h 10 min") */
    fun formatMinutes(minutes: Int): String {
        return if (minutes < 60) "$minutes min"
        else {
            val h = minutes / 60
            val m = minutes % 60
            if (m == 0) "$h h" else "$h h $m min"
        }
    }

    private fun Double.roundToInt(): Int = kotlin.math.round(this).toInt()
}
