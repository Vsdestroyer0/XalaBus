package com.example.xalabus.core.util

import kotlin.math.*

/**
 * CU-11: Calcula la distancia total y el tiempo estimado de traslado
 * a partir de una lista de coordenadas [lng, lat] de la geometría de la ruta.
 *
 * Velocidad promedio fija: 32 km/h (velocidad promedio de camión urbano en Xalapa).
 */
object TravelTimeEstimator {

    private const val AVG_SPEED_KMH = 32.0
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calcula la distancia total en kilómetros usando la fórmula de Haversine.
     * @param coordinates lista de puntos [[lng, lat], [lng, lat], ...]
     * @return distancia total en km, redondeada a 2 decimales
     */
    fun calculateDistanceKm(coordinates: List<List<Double>>): Double {
        if (coordinates.size < 2) return 0.0
        var totalKm = 0.0
        for (i in 0 until coordinates.size - 1) {
            totalKm += haversine(
                lat1 = coordinates[i][1],
                lon1 = coordinates[i][0],
                lat2 = coordinates[i + 1][1],
                lon2 = coordinates[i + 1][0]
            )
        }
        return (totalKm * 100).roundToInt() / 100.0
    }

    /**
     * Calcula el tiempo estimado de traslado en minutos.
     * @param distanceKm distancia total de la ruta
     * @return minutos estimados (entero)
     */
    fun calculateMinutes(distanceKm: Double): Int {
        if (distanceKm <= 0) return 0
        return ceil((distanceKm / AVG_SPEED_KMH) * 60).toInt()
    }

    /**
     * Formatea los minutos a una cadena legible.
     * Ejemplos: "45 min", "1 h 10 min", "2 h"
     */
    fun formatTime(minutes: Int): String {
        return when {
            minutes <= 0  -> "N/A"
            minutes < 60  -> "$minutes min"
            minutes % 60 == 0 -> "${minutes / 60} h"
            else -> "${minutes / 60} h ${minutes % 60} min"
        }
    }

    // ── Fórmula de Haversine ────────────────────────────────────────────────
    private fun haversine(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2).pow(2)
        val c = 2 * asin(sqrt(a))
        return EARTH_RADIUS_KM * c
    }
}
