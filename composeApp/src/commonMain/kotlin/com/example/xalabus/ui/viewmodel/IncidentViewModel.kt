package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** CU-13: Estados de la UI de reporte de incidentes */
sealed class IncidentUiState {
    object Idle    : IncidentUiState()
    object Loading : IncidentUiState()
    object Success : IncidentUiState()
    data class Error(val message: String) : IncidentUiState()
}

class IncidentViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<IncidentUiState>(IncidentUiState.Idle)
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    // Coordenadas iniciales: centro de Xalapa
    private val _selectedLat = MutableStateFlow(19.5438)
    val selectedLat: StateFlow<Double> = _selectedLat.asStateFlow()

    private val _selectedLng = MutableStateFlow(-96.9269)
    val selectedLng: StateFlow<Double> = _selectedLng.asStateFlow()

    // Límites geográficos aproximados del municipio de Xalapa
    // FA-01 (C2): coordenadas fuera de estos límites → "Punto inválido"
    companion object {
        private const val LAT_MIN = 19.48
        private const val LAT_MAX = 19.60
        private const val LNG_MIN = -97.00
        private const val LNG_MAX = -96.85

        fun isWithinXalapa(lat: Double, lng: Double): Boolean =
            lat in LAT_MIN..LAT_MAX && lng in LNG_MIN..LNG_MAX
    }

    fun updateLocation(lat: Double, lng: Double) {
        _selectedLat.value = lat
        _selectedLng.value = lng
    }

    /**
     * CU-13: Envía un reporte de incidente a Supabase.
     *
     * Validaciones (tabla de casos de prueba):
     *  C2 / FA-01 — coordenadas fuera de Xalapa  → error "Punto inválido"
     *  C3 / FA-02 — descripción vacía             → error con validación
     *  C4 / Ex-01 — fallo de red o datos          → error "Error al cargar datos"
     */
    fun submitIncidente(descripcion: String) {
        val latitud  = _selectedLat.value
        val longitud = _selectedLng.value

        // FA-01 (C2): validar que el punto esté dentro del mapa de Xalapa
        if (!isWithinXalapa(latitud, longitud)) {
            _uiState.value = IncidentUiState.Error("Punto inválido. Selecciona un punto dentro del mapa de Xalapa.")
            return
        }

        // FA-02 (C3): descripción obligatoria
        if (descripcion.isBlank()) {
            _uiState.value = IncidentUiState.Error("El campo de descripción no puede estar vacío.")
            return
        }

        val userId = supabase.auth.currentSessionOrNull()?.user?.id

        _uiState.value = IncidentUiState.Loading
        viewModelScope.launch {
            try {
                val payload = buildMap<String, Any?> {
                    userId?.let { put("user_id", it) }
                    put("tipo",        "otro") // Campo obligatorio según esquema
                    put("latitud",     latitud)
                    put("longitud",    longitud)
                    put("descripcion", descripcion)
                    put("estado",      "activo") // Estado válido según el CHECK del esquema
                }
                supabase.postgrest
                    .from("reportes")
                    .insert(payload)
                _uiState.value = IncidentUiState.Success
            } catch (e: Exception) {
                // Ex-01 (C4): error al cargar/guardar datos
                _uiState.value = IncidentUiState.Error("Error al cargar datos. Intenta de nuevo.")
            }
        }
    }

    fun resetState() {
        _uiState.value = IncidentUiState.Idle
    }
}
