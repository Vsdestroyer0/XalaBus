package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.core.util.TravelTimeEstimator
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** Modelo parcial de parada para el cálculo de tiempo (CU-11) */
@Serializable
private data class ParadaGps(
    val latitud: Double,
    val longitud: Double
)

/** Estados de UI para el tiempo estimado de traslado (CU-11) */
sealed class RouteTimeUiState {
    object Idle    : RouteTimeUiState()
    object Loading : RouteTimeUiState()
    /** [formattedTime] ej. "25 min" | [totalMinutes] para uso programático */
    data class Success(
        val totalMinutes: Int,
        val formattedTime: String
    ) : RouteTimeUiState()
    /** Ex-01: datos no disponibles */
    data class Error(val message: String) : RouteTimeUiState()
}

/**
 * CU-11: ViewModel que obtiene las paradas de una ruta y calcula el tiempo estimado
 * de traslado usando [TravelTimeEstimator] (velocidad promedio 30 km/h).
 *
 * Si la tabla `paradas` de Supabase ya tiene los datos, los usa directamente.
 * Si no hay datos disponibles, emite [RouteTimeUiState.Error] (Ex-01).
 */
class RouteTimeViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<RouteTimeUiState>(RouteTimeUiState.Idle)
    val uiState: StateFlow<RouteTimeUiState> = _uiState.asStateFlow()

    /**
     * Carga las paradas de [routeId] desde Supabase y calcula el tiempo estimado.
     * Postcondición: [uiState] emite Success con el tiempo formateado.
     */
    fun loadEstimatedTime(routeId: String) {
        _uiState.value = RouteTimeUiState.Loading
        viewModelScope.launch {
            try {
                val paradas = supabase.postgrest["paradas"]
                    .select {
                        filter { eq("ruta_id", routeId) }
                    }
                    .decodeList<ParadaGps>()

                if (paradas.size < 2) {
                    // Ex-01: datos insuficientes para calcular
                    _uiState.value = RouteTimeUiState.Error(
                        "No hay suficientes paradas para estimar el tiempo de traslado."
                    )
                    return@launch
                }

                val latitudes  = paradas.map { it.latitud }
                val longitudes = paradas.map { it.longitud }
                val minutes    = TravelTimeEstimator.estimateMinutes(latitudes, longitudes)

                if (minutes == null) {
                    _uiState.value = RouteTimeUiState.Error(
                        "No se pudo calcular el tiempo de traslado."
                    )
                } else {
                    _uiState.value = RouteTimeUiState.Success(
                        totalMinutes  = minutes,
                        formattedTime = TravelTimeEstimator.formatMinutes(minutes)
                    )
                }
            } catch (e: Exception) {
                // Ex-01: traducir con ErrorMapper en lugar de exponer e.message
                _uiState.value = RouteTimeUiState.Error(
                    ErrorMapper.toUserMessage(e, "al obtener datos de la ruta")
                )
            }
        }
    }

    fun resetState() { _uiState.value = RouteTimeUiState.Idle }
}
