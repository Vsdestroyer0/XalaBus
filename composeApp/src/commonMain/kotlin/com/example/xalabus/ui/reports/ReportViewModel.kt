package com.example.xalabus.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.reportes.Reporte
import com.example.xalabus.data.reportes.ReportesRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Coordenadas del punto seleccionado en el mapa (CU-13) */
data class MapPoint(val latitud: Double, val longitud: Double)

/** Estados de UI para el envío de reportes (CU-13) */
sealed class ReportUiState {
    object Idle    : ReportUiState()
    object Loading : ReportUiState()
    /** Reporte enviado exitosamente */
    object Success : ReportUiState()
    /**
     * FA-01: punto inválido (no se seleccionó ubicación en el mapa)
     * FA-02: texto vacío
     * Ex-01: error al subir
     */
    data class Error(val message: String) : ReportUiState()
}

/**
 * CU-13: ViewModel para reportar inconvenientes en una ruta.
 *
 * Expone:
 * - [uiState]: estado del envío
 * - [selectedPoint]: coordenadas del punto tocado en el mapa
 */
class ReportViewModel : ViewModel() {

    private val repository = ReportesRepository()
    private val supabase   = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _selectedPoint = MutableStateFlow<MapPoint?>(null)
    val selectedPoint: StateFlow<MapPoint?> = _selectedPoint

    /** Establece el punto seleccionado por el usuario en el mapa */
    fun setMapPoint(latitud: Double, longitud: Double) {
        _selectedPoint.value = MapPoint(latitud, longitud)
    }

    /**
     * Envía el reporte a Supabase.
     *
     * Validaciones:
     * - FA-01: [selectedPoint] es null → el usuario no seleccionó un punto en el mapa
     * - FA-02: [descripcion] está vacía
     * - Ex-01: error de red o Supabase
     *
     * @param descripcion Texto que describe el inconveniente
     * @param fotoUrl     URL de la foto (opcional, puede ser null)
     */
    fun sendReport(descripcion: String, fotoUrl: String? = null) {
        val point = _selectedPoint.value

        // FA-01: no se seleccionó punto en el mapa
        if (point == null) {
            _uiState.value = ReportUiState.Error(
                "Toca el mapa para indicar la ubicación del inconveniente."
            )
            return
        }

        // FA-02: descripción vacía
        if (descripcion.isBlank()) {
            _uiState.value = ReportUiState.Error(
                "Describe brevemente el inconveniente antes de enviarlo."
            )
            return
        }

        val userId = supabase.auth.currentSessionOrNull()?.user?.id

        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                repository.insertReporte(
                    Reporte(
                        userId      = userId,
                        latitud     = point.latitud,
                        longitud    = point.longitud,
                        descripcion = descripcion.trim(),
                        fotoUrl     = fotoUrl,
                        estado      = "pendiente"
                    )
                )
                _uiState.value = ReportUiState.Success
                _selectedPoint.value = null
            } catch (e: Exception) {
                // Ex-01: traducir con ErrorMapper en lugar de exponer e.message
                _uiState.value = ReportUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el reporte")
                )
            }
        }
    }

    fun resetState() { _uiState.value = ReportUiState.Idle }
}
