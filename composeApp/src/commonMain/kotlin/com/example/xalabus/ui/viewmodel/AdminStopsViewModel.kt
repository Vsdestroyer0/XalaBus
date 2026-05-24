package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.reports.ReportsRepository
import com.example.xalabus.data.reports.RouteStop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AdminStopsUiState {
    object Loading : AdminStopsUiState()
    data class Success(val stops: List<RouteStop>) : AdminStopsUiState()
    data class Error(val message: String) : AdminStopsUiState()
}

class AdminStopsViewModel : ViewModel() {
    private val repository = ReportsRepository()

    private val _uiState = MutableStateFlow<AdminStopsUiState>(AdminStopsUiState.Loading)
    val uiState: StateFlow<AdminStopsUiState> = _uiState.asStateFlow()

    init {
        fetchPendingStops()
    }

    fun fetchPendingStops() {
        _uiState.value = AdminStopsUiState.Loading
        viewModelScope.launch {
            try {
                val stops = repository.getPendingStops()
                _uiState.value = AdminStopsUiState.Success(stops)
            } catch (e: Exception) {
                _uiState.value = AdminStopsUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar las paradas")
                )
            }
        }
    }

    fun updateStopStatus(stopId: String, status: String, description: String) {
        viewModelScope.launch {
            try {
                repository.updateStopStatus(stopId, status, description)
                // Refrescar la lista despues de actualizar
                fetchPendingStops()
            } catch (e: Exception) {
                _uiState.value = AdminStopsUiState.Error(
                    ErrorMapper.toUserMessage(e, "al actualizar la parada")
                )
            }
        }
    }
}
