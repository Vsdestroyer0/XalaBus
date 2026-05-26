package com.example.xalabus.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.paradas.Parada
import com.example.xalabus.data.paradas.ParadasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de UI para la lista de paradas sugeridas pendientes de aprobación */
sealed class AdminStopsUiState {
    object Idle    : AdminStopsUiState()
    object Loading : AdminStopsUiState()
    data class Success(val stops: List<Parada>) : AdminStopsUiState()
    data class Error(val message: String) : AdminStopsUiState()
}

/**
 * ViewModel para gestionar paradas sugeridas pendientes de aprobación.
 * Usado por AdminDashboardScreen en la sección "Paradas Sugeridas".
 */
class AdminStopsViewModel : ViewModel() {

    private val repository = ParadasRepository()

    private val _uiState = MutableStateFlow<AdminStopsUiState>(AdminStopsUiState.Idle)
    val uiState: StateFlow<AdminStopsUiState> = _uiState.asStateFlow()

    fun fetchPendingStops() {
        _uiState.value = AdminStopsUiState.Loading
        viewModelScope.launch {
            try {
                val stops = repository.getPendingParadas()
                _uiState.value = AdminStopsUiState.Success(stops)
            } catch (e: Exception) {
                _uiState.value = AdminStopsUiState.Error(
                    e.message ?: "Error al cargar las sugerencias de paradas."
                )
            }
        }
    }

    fun approveStop(parada: Parada) {
        viewModelScope.launch {
            try {
                repository.approveParada(parada)
                fetchPendingStops()
            } catch (e: Exception) {
                _uiState.value = AdminStopsUiState.Error(
                    e.message ?: "Error al aprobar la parada."
                )
            }
        }
    }

    fun rejectStop(parada: Parada) {
        viewModelScope.launch {
            try {
                repository.rejectParada(parada)
                fetchPendingStops()
            } catch (e: Exception) {
                _uiState.value = AdminStopsUiState.Error(
                    e.message ?: "Error al rechazar la parada."
                )
            }
        }
    }
}
