package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─── Bounding-box del municipio de Xalapa (CU-13 FA-01) ─────────────────────
private const val XALAPA_LAT_MIN = 19.48
private const val XALAPA_LAT_MAX = 19.62
private const val XALAPA_LNG_MIN = -97.00
private const val XALAPA_LNG_MAX = -96.85

/** Comprueba si las coordenadas caen dentro del bounding-box de Xalapa. */
fun isWithinXalapa(lat: Double, lng: Double): Boolean =
    lat  in XALAPA_LAT_MIN..XALAPA_LAT_MAX &&
    lng  in XALAPA_LNG_MIN..XALAPA_LNG_MAX

// ─── Estado de la UI (CU-13) ────────────────────────────────────────────────
/**
 * Estados de UI para el envío de reportes de incidente (CU-13).
 *
 * - [Idle]    : estado inicial / después de resetear
 * - [Loading] : carga en progreso (Ex-01 pendiente)
 * - [Success] : C1 — reporte guardado y visible para otros usuarios
 * - [Error]   : FA-01 'Punto inválido' | FA-02 campo vacío | Ex-01 fallo de red
 */
sealed class IncidentUiState {
    object Idle    : IncidentUiState()
    object Loading : IncidentUiState()
    object Success : IncidentUiState()
    data class Error(val message: String) : IncidentUiState()
}

/**
 * CU-13: ViewModel para reportar algún inconveniente con la ruta.
 *
 * Expone:
 *  - [uiState]       : estado del envío
 *  - [selectedLat]   : latitud del punto marcado por el usuario
 *  - [selectedLng]   : longitud del punto marcado por el usuario
 *  - [pointSelected] : true cuando el usuario ya tocó el mapa
 *
 * Validaciones aplicadas en [submitIncidente]:
 *  - FA-01 (C2): punto fuera de Xalapa → "Punto inválido"
 *  - FA-02 (C3): descripción vacía     → mensaje de campo vacío
 *  - Ex-01 (C4): error de Supabase     → "Error al cargar datos"
 */
class IncidentViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<IncidentUiState>(IncidentUiState.Idle)
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    // Centro de Xalapa como valor por defecto
    private val _selectedLat = MutableStateFlow(19.5438)
    val selectedLat: StateFlow<Double> = _selectedLat.asStateFlow()

    private val _selectedLng = MutableStateFlow(-96.9269)
    val selectedLng: StateFlow<Double> = _selectedLng.asStateFlow()

    /**
     * Indica si el usuario ya seleccionó un punto en el mapa.
     * Comienza en false; se vuelve true al llamar [updateLocation].
     * Se resetea a false en [resetState].
     */
    private val _pointSelected = MutableStateFlow(false)
    val pointSelected: StateFlow<Boolean> = _pointSelected.asStateFlow()

    /**
     * Actualiza la ubicación seleccionada desde el mapa.
     * Marca [pointSelected] = true.
     *
     * FA-01: si las coordenadas están fuera del bounding-box de Xalapa,
     * el estado pasa a Error con el mensaje "Punto inválido".
     */
    fun updateLocation(lat: Double, lng: Double) {
        _selectedLat.value = lat
        _selectedLng.value = lng
        _pointSelected.value = true

        // FA-01 (C2): validar inmediatamente al marcar el punto
        if (!isWithinXalapa(lat, lng)) {
            _uiState.value = IncidentUiState.Error("Punto inválido: selecciona un punto dentro del mapa de Xalapa.")
        } else {
            // Limpiar error previo de FA-01 si ahora el punto es válido
            if (_uiState.value is IncidentUiState.Error) {
                _uiState.value = IncidentUiState.Idle
            }
        }
    }

    /**
     * Envía el reporte de inconveniente a Supabase.
     *
     * Orden de validaciones (según tabla de casos de prueba):
     *  1. FA-01 (C2): punto no seleccionado o fuera del mapa → Error
     *  2. FA-02 (C3): descripción vacía                      → Error
     *  3. Ex-01 (C4): fallo de red / Supabase                → Error
     *
     * Éxito (C1): reporte guardado, estado → Success.
     *
     * @param descripcion Texto que describe el inconveniente (obligatorio)
     * @param fotoUrl     URL de la foto adjunta (opcional, null por defecto)
     */
    fun submitIncidente(descripcion: String, fotoUrl: String? = null) {
        val lat = _selectedLat.value
        val lng = _selectedLng.value

        // FA-01 (C2): el punto aún no fue seleccionado por el usuario
        if (!_pointSelected.value) {
            _uiState.value = IncidentUiState.Error(
                "Punto inválido: toca el mapa para indicar la ubicación del inconveniente."
            )
            return
        }

        // FA-01 (C2): punto fuera del bounding-box de Xalapa
        if (!isWithinXalapa(lat, lng)) {
            _uiState.value = IncidentUiState.Error(
                "Punto inválido: el punto seleccionado está fuera del área de Xalapa."
            )
            return
        }

        // FA-02 (C3): descripción vacía
        if (descripcion.isBlank()) {
            _uiState.value = IncidentUiState.Error(
                "El cuadro de texto está vacío. Escribe una descripción antes de enviar."
            )
            return
        }

        val userId = supabase.auth.currentSessionOrNull()?.user?.id

        _uiState.value = IncidentUiState.Loading
        viewModelScope.launch {
            try {
                val payload = buildMap<String, Any?> {
                    userId?.let { put("user_id", it) }
                    put("latitud", lat)
                    put("longitud", lng)
                    put("descripcion", descripcion.trim())
                    put("estado", "pendiente")
                    fotoUrl?.let { put("foto_url", it) }
                }
                supabase.postgrest
                    .from("reportes")
                    .insert(payload)
                _uiState.value = IncidentUiState.Success
                _pointSelected.value = false
            } catch (e: Exception) {
                // Ex-01 (C4): error al cargar / guardar datos
                _uiState.value = IncidentUiState.Error(
                    "Error al cargar datos. Por favor, vuelve a intentarlo."
                )
            }
        }
    }

    /** Reinicia el estado de la UI y el punto seleccionado. */
    fun resetState() {
        _uiState.value = IncidentUiState.Idle
        _pointSelected.value = false
    }
}
