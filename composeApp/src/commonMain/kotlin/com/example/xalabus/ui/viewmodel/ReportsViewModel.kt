package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la UI de reportes generales y paradas */
sealed class ReportUiState {
    object Idle    : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ReportsViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    /** Envía un reporte de cambio sobre una ruta específica */
    fun submitRouteReport(routeId: Int, message: String) {
        if (message.isBlank()) {
            _uiState.value = ReportUiState.Error("El mensaje no puede estar vacío.")
            return
        }
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("route_reports")
                    .insert(mapOf("route_id" to routeId, "message" to message))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el reporte")
                )
            }
        }
    }

    /** Envía una sugerencia de parada de camión */
    fun submitStopSuggestion(routeId: Int, latitude: Double, longitude: Double) {
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("stop_suggestions")
                    .insert(mapOf(
                        "route_id"  to routeId,
                        "latitud"   to latitude,
                        "longitud"  to longitude
                    ))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(
                    ErrorMapper.toUserMessage(e, "al sugerir la parada")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }
}
