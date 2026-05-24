package com.example.xalabus.core.util

import kotlin.math.*

/**
 * CU-11: Utilidad para estimar el tiempo total de traslado en una ruta de camión.
 *
 * Lógica: velocidad promedio urbana de 30 km/h para camiones en Xalapa.
 * La distancia se calcula con la fórmula de Haversine entre puntos GPS consecutivos.
 */
object TravelTimeEstimator {

    /** Velocidad promedio urbana del camión en km/h */
    private const val AVERAGE_SPEED_KMH = 30.0

    /** Radio de la Tierra en kilómetros */
    private const val EARTH_RADIUS_KM = 6371.0

    /**
     * Calcula la distancia en kilómetros entre dos puntos GPS usando la fórmula de Haversine.
     *
     * @param lat1 Latitud del punto de origen
     * @param lon1 Longitud del punto de origen
     * @param lat2 Latitud del punto de destino
     * @param lon2 Longitud del punto de destino
     * @return Distancia en kilómetros
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
     * Estima el tiempo de traslado en minutos dado un conjunto de paradas en orden.
     *
     * Recorre la lista calculando la distancia acumulada entre cada par
     * de paradas consecutivas y luego convierte distancia → tiempo.
     *
     * @param stops Lista de pares (latitud, longitud) en orden de la ruta.
     * @return Tiempo estimado en minutos (redondeado), o null si la lista tiene < 2 paradas.
     */
    fun estimateMinutes(stops: List<Pair<Double, Double>>): Int? {
        if (stops.size < 2) return null
        var totalKm = 0.0
        for (i in 0 until stops.size - 1) {
            val (lat1, lon1) = stops[i]
            val (lat2, lon2) = stops[i + 1]
            totalKm += haversineDistanceKm(lat1, lon1, lat2, lon2)
        }
        // tiempo (h) = distancia (km) / velocidad (km/h)  =>  * 60 para minutos
        val minutes = (totalKm / AVERAGE_SPEED_KMH) * 60.0
        return minutes.roundToInt()
    }

    /**
     * Sobrecarga: acepta listas separadas de latitudes y longitudes (para uso desde Supabase).
     */
    fun estimateMinutes(latitudes: List<Double>, longitudes: List<Double>): Int? {
        if (latitudes.size != longitudes.size) return null
        return estimateMinutes(latitudes.zip(longitudes))
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
