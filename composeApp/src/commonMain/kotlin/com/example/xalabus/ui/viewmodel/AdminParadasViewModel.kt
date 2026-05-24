package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

/** CU-12: Modelo de datos de una parada de camión */
@Serializable
data class Parada(
    val id: String = "",
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    val ruta_id: String
)

/** CU-12: Estados de la UI de administración de paradas */
sealed class ParadasUiState {
    object Idle    : ParadasUiState()
    object Loading : ParadasUiState()
    object Success : ParadasUiState()
    data class Error(val message: String) : ParadasUiState()
    /** FA-02: parada duplicada por coordenadas cercanas */
    object DuplicateWarning : ParadasUiState()
}

class AdminParadasViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ParadasUiState>(ParadasUiState.Idle)
    val uiState: StateFlow<ParadasUiState> = _uiState.asStateFlow()

    /**
     * CU-12: Guarda una nueva parada en Supabase.
     * FA-01: datos incompletos → validación previa.
     * FA-02: duplicado por coordenadas → detectado vía ErrorMapper (unique_violation).
     */
    fun saveParada(nombre: String, latitud: Double, longitud: Double, rutaId: String) {
        if (nombre.isBlank() || rutaId.isBlank()) {
            _uiState.value = ParadasUiState.Error("Completa todos los campos requeridos.")
            return
        }
        if (latitud == 0.0 || longitud == 0.0) {
            _uiState.value = ParadasUiState.Error("Las coordenadas no son válidas.")
            return
        }
        _uiState.value = ParadasUiState.Loading
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("paradas")
                    .insert(mapOf(
                        "nombre"   to nombre,
                        "latitud"  to latitud,
                        "longitud" to longitud,
                        "ruta_id"  to rutaId
                    ))
                _uiState.value = ParadasUiState.Success
            } catch (e: Exception) {
                val msg = ErrorMapper.toUserMessage(e, "al guardar la parada")
                // FA-02: si es duplicado, usar estado específico
                if (msg.contains("ya existe")) {
                    _uiState.value = ParadasUiState.DuplicateWarning
                } else {
                    _uiState.value = ParadasUiState.Error(msg)
                }
            }
        }
    }

    /** Carga todas las paradas de una ruta específica */
    fun loadParadasForRoute(rutaId: String) {
        _uiState.value = ParadasUiState.Loading
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("paradas")
                    .select(Columns.ALL) {
                        filter { eq("ruta_id", rutaId) }
                    }
                    .decodeList<Parada>()
                _uiState.value = ParadasUiState.Success
            } catch (e: Exception) {
                _uiState.value = ParadasUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar las paradas")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ParadasUiState.Idle
    }
}
