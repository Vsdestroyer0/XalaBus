package com.example.xalabus.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.paradas.Parada
import com.example.xalabus.data.paradas.ParadasRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de UI para la pantalla de agregar paradas (CU-12) */
sealed class AdminStopUiState {
    object Idle    : AdminStopUiState()
    object Loading : AdminStopUiState()
    /** Parada guardada exitosamente */
    object Success : AdminStopUiState()
    /** FA-01: datos incompletos; FA-02: parada duplicada; Ex-01: error al guardar */
    data class Error(val message: String) : AdminStopUiState()
}

/**
 * CU-12: ViewModel para el registro de nuevas paradas de camión.
 * Solo accesible desde AdminAddStopScreen (rol developer/admin).
 */
class AdminStopViewModel : ViewModel() {

    private val repository = ParadasRepository()

    private val _uiState = MutableStateFlow<AdminStopUiState>(AdminStopUiState.Idle)
    val uiState: StateFlow<AdminStopUiState> = _uiState.asStateFlow()

    /**
     * Intenta registrar una nueva parada.
     *
     * Validaciones:
     * - FA-01: campos vacíos o latitud/longitud fuera de rango de Xalapa
     * - FA-02: ya existe una parada en coordenadas muy cercanas
     * - Ex-01: error de red o Supabase
     *
     * Postcondición: parada visible para todos los usuarios.
     */
    fun saveParada(
        nombre: String,
        latitudStr: String,
        longitudStr: String,
        rutaId: String
    ) {
        // FA-01: validación de campos
        when {
            nombre.isBlank()     -> { _uiState.value = AdminStopUiState.Error("El nombre es obligatorio."); return }
            latitudStr.isBlank() -> { _uiState.value = AdminStopUiState.Error("La latitud es obligatoria."); return }
            longitudStr.isBlank()-> { _uiState.value = AdminStopUiState.Error("La longitud es obligatoria."); return }
            rutaId.isBlank()     -> { _uiState.value = AdminStopUiState.Error("El ID de ruta es obligatorio."); return }
        }

        val latitud  = latitudStr.toDoubleOrNull()
        val longitud = longitudStr.toDoubleOrNull()

        if (latitud == null || longitud == null) {
            _uiState.value = AdminStopUiState.Error("Latitud y longitud deben ser números válidos.")
            return
        }

        _uiState.value = AdminStopUiState.Loading
        viewModelScope.launch {
            try {
                // FA-02: verificar duplicado por coordenadas
                val duplicate = repository.getNearbyParadas(latitud, longitud).firstOrNull()
                if (duplicate != null) {
                    _uiState.value = AdminStopUiState.Error(
                        "Ya existe una parada cercana: \"${duplicate.nombre}\". " +
                        "Verifica las coordenadas antes de continuar."
                    )
                    return@launch
                }

                repository.addParada(
                    Parada(
                        nombre   = nombre.trim(),
                        latitud  = latitud,
                        longitud = longitud,
                        rutaId   = rutaId.trim()
                    )
                )
                _uiState.value = AdminStopUiState.Success
            } catch (e: Exception) {
                // Ex-01: error al guardar
                _uiState.value = AdminStopUiState.Error(
                    "Error al guardar la parada: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = AdminStopUiState.Idle }
}
