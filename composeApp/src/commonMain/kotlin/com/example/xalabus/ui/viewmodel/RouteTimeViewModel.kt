package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.example.xalabus.core.util.TravelTimeEstimator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.*

/**
 * CU-11: Estado de UI para el componente de duración total del trayecto.
 *
 * - Idle:   Sin ruta seleccionada.
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
 * No hace llamadas a Supabase ni depende de paradas.
 * El cálculo es: distancia Haversine acumulada / 32 km/h → minutos totales.
 *
 * @param SPEED_KMH  Velocidad promedio del camión: 32 km/h (fija por requerimiento CU-11).
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
     * [RouteViewModel.selectedRoutePoints]: List<List<List<Double>>>
     * donde cada punto es [longitud, latitud] (formato GeoJSON).
     *
     * Si la geometría está vacía o tiene menos de 2 puntos no hace nada.
     */
    fun calculateFromGeometry(routePoints: List<List<List<Double>>>) {
        // Aplanar todas las polylines en una sola secuencia de puntos
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
            totalKm += TravelTimeEstimator.haversineDistanceKm(lat1, lon1, lat2, lon2)
        }

        // tiempo (min) = (km / km/h) * 60
        val totalMinutes = ((totalKm / SPEED_KMH) * 60).roundToInt()

        _uiState.value = RouteTimeUiState.Result(
            distanceKm    = (totalKm * 10).toLong() / 10.0,   // 1 decimal
            totalMinutes  = totalMinutes,
            formattedTime = TravelTimeEstimator.formatMinutes(totalMinutes)
        )
    }

    fun resetState() {
        _uiState.value = RouteTimeUiState.Idle
    }
}

// Extensión para redondear Double a Int (evita importar kotlin.math en el archivo)
private fun Double.roundToInt(): Int = kotlin.math.roundToInt(this)
