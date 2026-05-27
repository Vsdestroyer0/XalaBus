package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.incidentes.Incidente
import com.example.xalabus.data.incidentes.IncidentesRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/** CU-13: Estados de la UI de reporte de incidentes */
sealed class IncidentUiState {
    object Idle    : IncidentUiState()
    object Loading : IncidentUiState()
    object Success : IncidentUiState()
    data class Error(val message: String) : IncidentUiState()
}

class IncidentViewModel : ViewModel() {

    private val supabase   = SupabaseClientProvider.client
    private val repository = IncidentesRepository()

    private val _uiState = MutableStateFlow<IncidentUiState>(IncidentUiState.Idle)
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    // Coordenadas del punto seleccionado en el mapa de reporte
    private val _selectedLat = MutableStateFlow(19.5438)
    val selectedLat: StateFlow<Double> = _selectedLat.asStateFlow()

    private val _selectedLng = MutableStateFlow(-96.9269)
    val selectedLng: StateFlow<Double> = _selectedLng.asStateFlow()

    // Lista de incidentes activos (creados en las últimas 4 horas)
    private val _incidentes = MutableStateFlow<List<Incidente>>(emptyList())
    val incidentes: StateFlow<List<Incidente>> = _incidentes.asStateFlow()

    // Límites geográficos del municipio de Xalapa
    // FA-01 (C2): coordenadas fuera de estos límites → "Punto inválido"
    companion object {
        private const val LAT_MIN = 19.48
        private const val LAT_MAX = 19.60
        private const val LNG_MIN = -97.00
        private const val LNG_MAX = -96.85

        /** Intervalo de refresco del mapa: cada 5 minutos */
        private const val REFRESH_INTERVAL_MS = 5 * 60 * 1000L

        fun isWithinXalapa(lat: Double, lng: Double): Boolean =
            lat in LAT_MIN..LAT_MAX && lng in LNG_MIN..LNG_MAX
    }

    fun updateLocation(lat: Double, lng: Double) {
        _selectedLat.value = lat
        _selectedLng.value = lng
    }

    /**
     * Inicia un loop que refresca los incidentes activos cada 5 minutos.
     * Llamar una sola vez al abrir el mapa principal (LaunchedEffect(Unit)).
     * Los incidentes con más de 4h desaparecen automáticamente del mapa
     * sin necesidad de reiniciar la app.
     */
    fun iniciarRefrescoIncidentes() {
        viewModelScope.launch {
            while (isActive) {
                try {
                    _incidentes.value = repository.getIncidentesActivos()
                } catch (e: Exception) {
                    // Fallo silencioso: no bloquea el mapa si hay error de red
                    _incidentes.value = emptyList()
                }
                delay(REFRESH_INTERVAL_MS)
            }
        }
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

        // FA-01 (C2): punto fuera de Xalapa
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
                val incidente = Incidente(
                    userId      = userId,
                    latitud     = latitud,
                    longitud    = longitud,
                    descripcion = descripcion,
                    estado      = "pendiente"
                )
                repository.reportarIncidente(incidente)
                _uiState.value = IncidentUiState.Success
                // Refresco inmediato para que el nuevo punto aparezca en el mapa
                _incidentes.value = repository.getIncidentesActivos()
            } catch (e: Exception) {
                // Ex-01 (C4)
                _uiState.value = IncidentUiState.Error("Error al cargar datos. Intenta de nuevo.")
            }
        }
    }

    fun resetState() {
        _uiState.value = IncidentUiState.Idle
    }
}
