package com.example.xalabus.core.util

/**
 * Límites aproximados del mapa offline de Xalapa (mbtiles local).
 * FA-01: si el usuario está fuera de este rectángulo, no se envía la petición.
 */
object MapBounds {
    const val MIN_LAT = 19.45
    const val MAX_LAT = 19.62
    const val MIN_LNG = -97.05
    const val MAX_LNG = -96.78

    fun contains(lat: Double, lng: Double): Boolean =
        lat in MIN_LAT..MAX_LAT && lng in MIN_LNG..MAX_LNG
}
