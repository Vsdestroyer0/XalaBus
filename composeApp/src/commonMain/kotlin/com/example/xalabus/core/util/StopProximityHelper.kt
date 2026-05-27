package com.example.xalabus.core.util

import com.example.xalabus.DBD.StopEntity
import com.example.xalabus.data.reports.RouteStop
import kotlin.math.*

/**
 * CU-09: utilidades de distancia para detectar paradas cercanas.
 */
object StopProximityHelper {

    const val NEARBY_THRESHOLD_METERS = 50.0
    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun distanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
            sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    fun findNearbyLocalStop(
        stops: List<StopEntity>,
        lat: Double,
        lng: Double,
        thresholdMeters: Double = NEARBY_THRESHOLD_METERS
    ): StopEntity? = stops.minByOrNull { distanceMeters(lat, lng, it.lat, it.lng) }
        ?.takeIf { distanceMeters(lat, lng, it.lat, it.lng) <= thresholdMeters }

    fun findNearbyRemoteStop(
        stops: List<RouteStop>,
        lat: Double,
        lng: Double,
        thresholdMeters: Double = NEARBY_THRESHOLD_METERS
    ): RouteStop? = stops.minByOrNull { distanceMeters(lat, lng, it.latitude, it.longitude) }
        ?.takeIf { distanceMeters(lat, lng, it.latitude, it.longitude) <= thresholdMeters }
}
