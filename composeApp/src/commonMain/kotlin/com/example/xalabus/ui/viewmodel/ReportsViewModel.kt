package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.reports.GeneralReport
import com.example.xalabus.data.reports.ReportsRepository
import com.example.xalabus.data.reports.RouteReport
import com.example.xalabus.data.reports.RouteStop
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ReportUiState {
    object Idle : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

class ReportsViewModel : ViewModel() {
    private val repository = ReportsRepository()
    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private fun getCurrentUserId(): String? {
        return supabase.auth.currentSessionOrNull()?.user?.id
    }

    fun submitGeneralReport(message: String) {
        if (message.isBlank()) {
            _uiState.value = ReportUiState.Error("El mensaje no puede estar vacío.")
            return
        }
        
        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = ReportUiState.Error("Debes iniciar sesión para reportar.")
            return
        }

        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                repository.sendGeneralReport(GeneralReport(userId = userId, message = message))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al enviar reporte: ${e.message}")
            }
        }
    }

    fun submitRouteReport(routeId: Int, message: String) {
        if (message.isBlank()) {
            _uiState.value = ReportUiState.Error("El mensaje no puede estar vacío.")
            return
        }

        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = ReportUiState.Error("Debes iniciar sesión para reportar.")
            return
        }

        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                repository.sendRouteReport(RouteReport(userId = userId, routeId = routeId, message = message))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al enviar reporte: ${e.message}")
            }
        }
    }

    fun submitRouteStop(routeId: Int, description: String, lat: Double, lng: Double) {
        if (description.isBlank()) {
            _uiState.value = ReportUiState.Error("La descripción no puede estar vacía.")
            return
        }

        val userId = getCurrentUserId()
        if (userId == null) {
            _uiState.value = ReportUiState.Error("Debes iniciar sesión para sugerir paradas.")
            return
        }

        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                repository.suggestRouteStop(
                    RouteStop(
                        userId = userId,
                        routeId = routeId,
                        description = description,
                        latitude = lat,
                        longitude = lng
                    )
                )
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error("Error al sugerir parada: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }
}
