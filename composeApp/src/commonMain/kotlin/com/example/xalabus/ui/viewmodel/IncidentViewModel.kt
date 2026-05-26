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

    private val _selectedLat = MutableStateFlow(19.5438) // Centro de Xalapa por defecto
    val selectedLat: StateFlow<Double> = _selectedLat.asStateFlow()

    private val _selectedLng = MutableStateFlow(-96.9269)
    val selectedLng: StateFlow<Double> = _selectedLng.asStateFlow()

    fun updateLocation(lat: Double, lng: Double) {
        _selectedLat.value = lat
        _selectedLng.value = lng
    }

    /**
     * CU-13: Envía un reporte de incidente a Supabase.
     */
    fun submitIncidente(descripcion: String) {
        val latitud = _selectedLat.value
        val longitud = _selectedLng.value

        if (descripcion.isBlank()) {
            _uiState.value = IncidentUiState.Error("La descripción no puede estar vacía.")
            return
        }

        val userId = supabase.auth.currentSessionOrNull()?.user?.id

        _uiState.value = IncidentUiState.Loading
        viewModelScope.launch {
            try {
                val payload = buildMap<String, Any?> {
                    userId?.let { put("user_id", it) }
                    put("latitud", latitud)
                    put("longitud", longitud)
                    put("descripcion", descripcion)
                    put("estado", "pendiente")
                }
                supabase.postgrest
                    .from("reportes")
                    .insert(payload)
                _uiState.value = IncidentUiState.Success
            } catch (e: Exception) {
                _uiState.value = IncidentUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el reporte")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = IncidentUiState.Idle
    }
}
