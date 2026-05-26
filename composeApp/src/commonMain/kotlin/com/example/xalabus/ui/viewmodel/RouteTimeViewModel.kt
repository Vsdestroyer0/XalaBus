package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * CU-11: Estado de UI para el componente de duración total del trayecto.
 *
 * - Idle:   Sin ruta seleccionada o geometría vacía.
 * - Result: Cálculo listo con distancia total y tiempo estimado.
 */
sealed class RouteTimeUiState {
    object Idle : RouteTimeUiState()

    data class Result(
        val distanceKm: Double,
        val totalMinutes: Int,
        val formattedTime: String
    ) : RouteTimeUiState()
}

/**
 * CU-11: ViewModel que calcula el tiempo total del trayecto de una ruta
 * a partir de su geometría (lista de coordenadas ya cargadas en memoria).
 *
 * No hace llamadas a red ni depende de paradas.
 * Fórmula: distancia Haversine acumulada / 32 km/h → minutos totales.
 *
 * Velocidad promedio fija del camión: 32 km/h (requerimiento CU-11).
 */
class RouteTimeViewModel : ViewModel() {

    companion object {
        private const val SPEED_KMH = 32.0
    }

    private val _uiState = MutableStateFlow<RouteTimeUiState>(RouteTimeUiState.Idle)
    val uiState: StateFlow<RouteTimeUiState> = _uiState.asStateFlow()

    /**
     * Calcula el tiempo total del trayecto desde la geometría de la ruta.
     *
     * [routePoints] es la lista de polylines tal como la expone
     * RouteViewModel.selectedRoutePoints: List<List<List<Double>>>
     * donde cada punto es [longitud, latitud] (formato GeoJSON).
     */
    fun calculateFromGeometry(routePoints: List<List<List<Double>>>) {
        val points = routePoints.flatten()
        if (points.size < 2) {
            _uiState.value = RouteTimeUiState.Idle
            return
        }

        // Acumular distancia Haversine entre puntos consecutivos
        // Formato GeoJSON: cada punto = [lon, lat]
        var totalKm = 0.0
        for (i in 0 until points.size - 1) {
            val (lon1, lat1) = points[i]
            val (lon2, lat2) = points[i + 1]
            totalKm += haversineKm(lat1, lon1, lat2, lon2)
        }

        val totalMinutes = ((totalKm / SPEED_KMH) * 60).toInt()
        val distanceRounded = (totalKm * 10).toLong() / 10.0

        _uiState.value = RouteTimeUiState.Result(
            distanceKm    = distanceRounded,
            totalMinutes  = totalMinutes,
            formattedTime = formatMinutes(totalMinutes)
        )
    }

    fun resetState() {
        _uiState.value = RouteTimeUiState.Idle
    }

    // ── Haversine ─────────────────────────────────────────────────────────────

    private fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val r = 6371.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return r * 2 * asin(sqrt(a))
    }

    // ── Formato de tiempo ─────────────────────────────────────────────────────

    private fun formatMinutes(minutes: Int): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h > 0 && m > 0 -> "${h}h ${m}min"
            h > 0           -> "${h}h"
            else            -> "${m}min"
        }
    }
}
