package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.incidentes.Incidente
import com.example.xalabus.data.incidentes.IncidentesRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados del flujo de reporte de incidente (CU-13) */
sealed class IncidentUiState {
    object Idle : IncidentUiState()
    object Loading : IncidentUiState()
    object Success : IncidentUiState()
    data class Error(val message: String) : IncidentUiState()
}

class IncidentViewModel : ViewModel() {

    private val repository = IncidentesRepository()
    private val supabase   = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<IncidentUiState>(IncidentUiState.Idle)
    val uiState: StateFlow<IncidentUiState> = _uiState.asStateFlow()

    /** Coordenadas seleccionadas por el usuario en el mapa */
    private val _selectedLat = MutableStateFlow(19.5438) // Centro de Xalapa por defecto
    private val _selectedLng = MutableStateFlow(-96.9269)
    val selectedLat: StateFlow<Double> = _selectedLat.asStateFlow()
    val selectedLng: StateFlow<Double> = _selectedLng.asStateFlow()

    /** Actualiza el punto en el mapa donde ocurrió el incidente */
    fun updateLocation(lat: Double, lng: Double) {
        _selectedLat.value = lat
        _selectedLng.value = lng
    }

    private fun getCurrentUserId(): String? =
        supabase.auth.currentSessionOrNull()?.user?.id

    /**
     * CU-13 — Envía el reporte de incidente a Supabase.
     * FA-01: coordenadas fuera del rango de Xalapa → Error.
     * FA-02: descripción vacía → Error.
     * Ex-01: error al subir → Error con mensaje.
     */
    fun submitIncidente(descripcion: String) {
        // FA-02: descripción vacía
        if (descripcion.isBlank()) {
            _uiState.value = IncidentUiState.Error("Describe el problema antes de enviar.")
            return
        }

        val lat = _selectedLat.value
        val lng = _selectedLng.value

        // FA-01: validar que las coordenadas sean razonables para Xalapa, Veracruz
        // Rango aproximado: lat 18.5–20.5, lng -97.5–96.0
        if (lat !in 18.5..20.5 || lng !in -97.5..-96.0) {
            _uiState.value = IncidentUiState.Error(
                "Las coordenadas seleccionadas están fuera del área de servicio."
            )
            return
        }

        _uiState.value = IncidentUiState.Loading
        viewModelScope.launch {
            try {
                repository.reportarIncidente(
                    Incidente(
                        userId      = getCurrentUserId(),
                        latitud     = lat,
                        longitud    = lng,
                        descripcion = descripcion.trim()
                    )
                )
                _uiState.value = IncidentUiState.Success
            } catch (e: Exception) {
                // Ex-01: error al subir
                _uiState.value = IncidentUiState.Error(
                    "Error al enviar el reporte: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = IncidentUiState.Idle }
}
