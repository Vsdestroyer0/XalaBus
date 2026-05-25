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
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de parada que llega desde la tabla `paradas` de Supabase.
 * Sólo se mapean los campos necesarios para el cálculo de CU-11.
 */
@Serializable
data class ParadaItem(
    val id: String = "",
    val nombre: String = "",
    val latitud: Double = 0.0,
    val longitud: Double = 0.0,
    @SerialName("ruta_id") val rutaId: String = ""
)

/**
 * Estados de UI para CU-11 (selector de tramo).
 *
 * - Idle:    Estado inicial / reiniciado.
 * - Loading: Cargando paradas desde Supabase.
 * - Ready:   Paradas cargadas; usuario puede seleccionar origen y destino.
 * - Result:  Cálculo completado; muestra tiempo estimado.
 * - Error:   Ex-01, datos no disponibles.
 */
sealed class RouteTimeUiState {
    object Idle    : RouteTimeUiState()
    object Loading : RouteTimeUiState()

    /** Paradas listas para seleccionar tramo */
    data class Ready(
        val paradas: List<ParadaItem>,
        val fromIndex: Int = 0,
        val toIndex: Int = if (paradas.size > 1) paradas.size - 1 else 0
    ) : RouteTimeUiState()

    /** Resultado del cálculo de tiempo */
    data class Result(
        val paradas: List<ParadaItem>,
        val fromIndex: Int,
        val toIndex: Int,
        val totalMinutes: Int,
        val formattedTime: String,
        /** Detalle legible: paradas intermedias y km del tramo */
        val stopCount: Int,
        val distanceKm: Double
    ) : RouteTimeUiState()

    /** Ex-01: datos no disponibles */
    data class Error(val message: String) : RouteTimeUiState()
}

/**
 * CU-11: ViewModel que:
 *  1. Carga todas las paradas de una ruta desde la tabla `paradas` de Supabase.
 *  2. Permite al usuario seleccionar parada origen y parada destino.
 *  3. Calcula el tiempo estimado usando [TravelTimeEstimator.estimateMinutesForSegment]:
 *       tiempo = (distancia Haversine km / 30 km/h * 60) + (paradas intermedias * 1 min)
 *
 * Fórmula de paradas intermedias:
 *   intermedias = toIndex - fromIndex  (ej. A→D tiene 3 paradas: B, C, D)
 *   (la parada de origen NO se cuenta porque el usuario ya está ahí)
 */
class RouteTimeViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<RouteTimeUiState>(RouteTimeUiState.Idle)
    val uiState: StateFlow<RouteTimeUiState> = _uiState.asStateFlow()

    // -----------------------------------------------------------------------
    // Carga de paradas
    // -----------------------------------------------------------------------

    /**
     * Carga las paradas de [routeId] desde Supabase en orden.
     * Al terminar emite [RouteTimeUiState.Ready] con la lista lista para seleccionar.
     */
    fun loadStops(routeId: String) {
        _uiState.value = RouteTimeUiState.Loading
        viewModelScope.launch {
            try {
                val paradas = supabase.postgrest["paradas"]
                    .select { filter { eq("ruta_id", routeId) } }
                    .decodeList<ParadaItem>()

                if (paradas.size < 2) {
                    _uiState.value = RouteTimeUiState.Error(
                        "Esta ruta no tiene suficientes paradas registradas para calcular el tiempo."
                    )
                    return@launch
                }

                _uiState.value = RouteTimeUiState.Ready(
                    paradas   = paradas,
                    fromIndex = 0,
                    toIndex   = paradas.size - 1
                )

                // Calcular automáticamente el tramo completo al cargar
                calculateSegment(paradas, fromIndex = 0, toIndex = paradas.size - 1)

            } catch (e: Exception) {
                _uiState.value = RouteTimeUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar las paradas de la ruta")
                )
            }
        }
    }

    // -----------------------------------------------------------------------
    // Cálculo de tramo seleccionado
    // -----------------------------------------------------------------------

    /**
     * Recalcula el tiempo cuando el usuario cambia la parada de origen o destino.
     *
     * @param fromIndex  Índice de la parada de origen en la lista actual.
     * @param toIndex    Índice de la parada de destino en la lista actual.
     */
    fun onSegmentSelected(fromIndex: Int, toIndex: Int) {
        val current = _uiState.value
        val paradas: List<ParadaItem> = when (current) {
            is RouteTimeUiState.Ready  -> current.paradas
            is RouteTimeUiState.Result -> current.paradas
            else -> return  // no hay datos cargados aún
        }

        // FA-01: origen y destino iguales o inválidos — no calcular
        if (fromIndex >= toIndex || fromIndex < 0 || toIndex >= paradas.size) {
            _uiState.value = RouteTimeUiState.Error(
                "Selecciona una parada de origen anterior a la de destino."
            )
            return
        }

        calculateSegment(paradas, fromIndex, toIndex)
    }

    // -----------------------------------------------------------------------
    // Lógica interna
    // -----------------------------------------------------------------------

    private fun calculateSegment(
        paradas: List<ParadaItem>,
        fromIndex: Int,
        toIndex: Int
    ) {
        val stops = paradas.map { Pair(it.latitud, it.longitud) }
        val minutes = TravelTimeEstimator.estimateMinutesForSegment(stops, fromIndex, toIndex)

        if (minutes == null) {
            _uiState.value = RouteTimeUiState.Error(
                "No se pudo calcular el tiempo. Verifica los datos de las paradas."
            )
            return
        }

        // Distancia acumulada del tramo (para el detalle)
        var totalKm = 0.0
        for (i in fromIndex until toIndex) {
            totalKm += TravelTimeEstimator.haversineDistanceKm(
                paradas[i].latitud,     paradas[i].longitud,
                paradas[i + 1].latitud, paradas[i + 1].longitud
            )
        }

        _uiState.value = RouteTimeUiState.Result(
            paradas       = paradas,
            fromIndex     = fromIndex,
            toIndex       = toIndex,
            totalMinutes  = minutes,
            formattedTime = TravelTimeEstimator.formatMinutes(minutes),
            stopCount     = toIndex - fromIndex,  // paradas intermedias + destino
            distanceKm    = (totalKm * 10).toLong() / 10.0  // redondear a 1 decimal
        )
    }

    fun resetState() { _uiState.value = RouteTimeUiState.Idle }
}
