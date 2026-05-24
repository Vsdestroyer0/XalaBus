package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.paradas.Parada
import com.example.xalabus.data.paradas.ParadasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** CU-12: Estados de la UI de administración de paradas */
sealed class AdminParadasUiState {
    object Idle    : AdminParadasUiState()
    object Loading : AdminParadasUiState()
    object Success : AdminParadasUiState()
    data class Error(val message: String) : AdminParadasUiState()
    /** FA-02: parada duplicada por coordenadas cercanas */
    data class DuplicateWarning(val cercanas: List<Parada>) : AdminParadasUiState()
}

class AdminParadasViewModel : ViewModel() {

    private val repository = ParadasRepository()

    private val _uiState = MutableStateFlow<AdminParadasUiState>(AdminParadasUiState.Idle)
    val uiState: StateFlow<AdminParadasUiState> = _uiState.asStateFlow()

    /**
     * CU-12: Guarda una nueva parada.
     * FA-01: validación de datos.
     * FA-02: detección de paradas cercanas antes de guardar.
     */
    fun addParada(
        nombre: String,
        latitudStr: String,
        longitudStr: String,
        rutaId: String,
        forzar: Boolean = false
    ) {
        val lat = latitudStr.toDoubleOrNull()
        val lng = longitudStr.toDoubleOrNull()

        if (nombre.isBlank() || latitudStr.isBlank() || longitudStr.isBlank() || rutaId.isBlank()) {
            _uiState.value = AdminParadasUiState.Error("Completa todos los campos requeridos.")
            return
        }

        if (lat == null || lng == null) {
            _uiState.value = AdminParadasUiState.Error("Las coordenadas deben ser números válidos.")
            return
        }

        _uiState.value = AdminParadasUiState.Loading
        viewModelScope.launch {
            try {
                // FA-02: Verificar duplicados si no se está forzando
                if (!forzar) {
                    val cercanas = repository.getNearbyParadas(lat, lng)
                    if (cercanas.isNotEmpty()) {
                        _uiState.value = AdminParadasUiState.DuplicateWarning(cercanas)
                        return@launch
                    }
                }

                val nuevaParada = Parada(
                    nombre = nombre,
                    latitud = lat,
                    longitud = lng,
                    rutaId = rutaId
                )
                repository.addParada(nuevaParada)
                _uiState.value = AdminParadasUiState.Success
            } catch (e: Exception) {
                _uiState.value = AdminParadasUiState.Error(
                    ErrorMapper.toUserMessage(e, "al guardar la parada")
                )
            }
        }
    }

    /** Carga todas las paradas de una ruta específica */
    fun loadParadasForRoute(rutaId: String) {
        _uiState.value = AdminParadasUiState.Loading
        viewModelScope.launch {
            try {
                repository.getParadasByRuta(rutaId)
                _uiState.value = AdminParadasUiState.Success
            } catch (e: Exception) {
                _uiState.value = AdminParadasUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar las paradas")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = AdminParadasUiState.Idle
    }
}
