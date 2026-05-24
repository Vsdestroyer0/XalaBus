package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.paradas.Parada
import com.example.xalabus.data.paradas.ParadasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados del flujo de agregar parada (CU-12) */
sealed class AdminParadasUiState {
    object Idle : AdminParadasUiState()
    object Loading : AdminParadasUiState()
    object Success : AdminParadasUiState()
    data class Error(val message: String) : AdminParadasUiState()
    /** FA-02: parada duplicada detectada por coordenadas cercanas */
    data class DuplicateWarning(val cercanas: List<Parada>) : AdminParadasUiState()
}

class AdminParadasViewModel : ViewModel() {

    private val repository = ParadasRepository()

    private val _uiState = MutableStateFlow<AdminParadasUiState>(AdminParadasUiState.Idle)
    val uiState: StateFlow<AdminParadasUiState> = _uiState.asStateFlow()

    /**
     * CU-12 — Registra una nueva parada con coordenadas GPS.
     * FA-01: datos incompletos → Error.
     * FA-02: parada duplicada por coordenadas cercanas → DuplicateWarning.
     * Ex-01: error al guardar → Error.
     * @param forzar si true, omite la verificación de duplicados y guarda de todas formas.
     */
    fun addParada(
        nombre: String,
        latitud: String,
        longitud: String,
        rutaId: String,
        forzar: Boolean = false
    ) {
        // FA-01: validar datos incompletos
        if (nombre.isBlank() || latitud.isBlank() || longitud.isBlank() || rutaId.isBlank()) {
            _uiState.value = AdminParadasUiState.Error("Todos los campos son obligatorios.")
            return
        }

        val lat = latitud.toDoubleOrNull()
        val lng = longitud.toDoubleOrNull()

        if (lat == null || lng == null) {
            _uiState.value = AdminParadasUiState.Error("Latitud y longitud deben ser números válidos.")
            return
        }

        _uiState.value = AdminParadasUiState.Loading
        viewModelScope.launch {
            try {
                // FA-02: verificar paradas cercanas (solo si no se está forzando)
                if (!forzar) {
                    val cercanas = repository.getNearbyParadas(lat, lng)
                    if (cercanas.isNotEmpty()) {
                        _uiState.value = AdminParadasUiState.DuplicateWarning(cercanas)
                        return@launch
                    }
                }

                repository.addParada(
                    Parada(
                        nombre   = nombre.trim(),
                        latitud  = lat,
                        longitud = lng,
                        rutaId   = rutaId.trim()
                    )
                )
                _uiState.value = AdminParadasUiState.Success
            } catch (e: Exception) {
                // Ex-01: error al guardar
                _uiState.value = AdminParadasUiState.Error(
                    "Error al guardar la parada: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = AdminParadasUiState.Idle }
}
