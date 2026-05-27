package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.core.util.MapBounds
import com.example.xalabus.core.util.NetworkMonitor
import com.example.xalabus.core.util.StopProximityHelper
import com.example.xalabus.data.SupabaseClientProvider
import com.example.xalabus.data.repository.RouteRepository
import com.example.xalabus.data.reports.ReportsRepository
import com.example.xalabus.data.reports.RouteStop
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados de la UI de reportes generales y cambios de ruta */
sealed class ReportUiState {
    object Idle    : ReportUiState()
    object Loading : ReportUiState()
    object Success : ReportUiState()
    data class Error(val message: String) : ReportUiState()
}

/** CU-09: estados específicos para agregar parada */
sealed class StopUiState {
    object Idle    : StopUiState()
    object Loading : StopUiState()
    data class Success(val message: String) : StopUiState()
    data class Error(val message: String) : StopUiState()
}

class ReportsViewModel(
    private val routeRepository: RouteRepository? = null,
    private val reportsRepository: ReportsRepository = ReportsRepository()
) : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ReportUiState>(ReportUiState.Idle)
    val uiState: StateFlow<ReportUiState> = _uiState.asStateFlow()

    private val _stopUiState = MutableStateFlow<StopUiState>(StopUiState.Idle)
    val stopUiState: StateFlow<StopUiState> = _stopUiState.asStateFlow()

    fun submitGeneralReport(message: String) {
        if (message.isBlank()) {
            _uiState.value = ReportUiState.Error("El mensaje no puede estar vacío.")
            return
        }
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: "anonymous"
                supabase.postgrest
                    .from("general_reports")
                    .insert(mapOf("user_id" to userId, "message" to message))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el reporte")
                )
            }
        }
    }

    fun submitRouteReport(routeId: Int, message: String) {
        if (message.isBlank()) {
            _uiState.value = ReportUiState.Error("El mensaje no puede estar vacío.")
            return
        }
        _uiState.value = ReportUiState.Loading
        viewModelScope.launch {
            try {
                val userId = supabase.auth.currentUserOrNull()?.id ?: "anonymous"
                supabase.postgrest
                    .from("route_reports")
                    .insert(mapOf(
                        "user_id" to userId,
                        "route_id" to routeId,
                        "message" to message
                    ))
                _uiState.value = ReportUiState.Success
            } catch (e: Exception) {
                _uiState.value = ReportUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el reporte")
                )
            }
        }
    }

    /**
     * CU-09: el usuario indica que hay una parada en su ubicación actual.
     * FA-01 fuera del mapa | Ex-02 sin internet | proximidad → popularidad.
     */
    fun submitStopHere(routeId: Int, latitude: Double, longitude: Double) {
        submitRouteStop(
            routeId = routeId,
            description = "Parada reportada por el usuario",
            latitude = latitude,
            longitude = longitude
        )
    }

    /**
     * Compatibilidad con `RouteStopDialog`.
     * Envía una sugerencia de parada con descripción + coordenadas.
     */
    fun submitRouteStop(routeId: Int, description: String, latitude: Double, longitude: Double) {
        if (routeId <= 0) {
            _stopUiState.value = StopUiState.Error("Ruta no válida.")
            return
        }

        // FA-01: fuera del área del mapa offline
        if (!MapBounds.contains(latitude, longitude)) {
            _stopUiState.value = StopUiState.Error(
                "Estás fuera del área del mapa local. Acércate a la zona de cobertura."
            )
            return
        }

        // Ex-02: sin conexión
        if (!NetworkMonitor.isOnline()) {
            _stopUiState.value = StopUiState.Error(
                "Sin conexión a internet. No se puede enviar la solicitud."
            )
            return
        }

        if (description.isBlank()) {
            _stopUiState.value = StopUiState.Error("La descripción no puede estar vacía.")
            return
        }

        val userId = supabase.auth.currentUserOrNull()?.id
        if (userId == null) {
            _stopUiState.value = StopUiState.Error(
                "Inicia sesión para reportar una parada."
            )
            return
        }

        _stopUiState.value = StopUiState.Loading
        viewModelScope.launch {
            try {
                val routeIdStr = routeId.toString()
                val localStops = routeRepository?.getStopsForRoute(routeIdStr).orEmpty()

                // Parada cercana en caché local
                StopProximityHelper.findNearbyLocalStop(localStops, latitude, longitude)?.let { nearby ->
                    routeRepository?.incrementLocalStopPopularity(nearby.id)
                    trySyncRemotePopularity(routeId, latitude, longitude, nearby.id)
                    _stopUiState.value = StopUiState.Success(
                        "Ya existe una parada cerca. Se aumentó su popularidad."
                    )
                    return@launch
                }

                // Parada cercana en Supabase
                val nearbyRemote = reportsRepository.findNearbyStop(routeId, latitude, longitude)
                if (nearbyRemote?.id != null) {
                    reportsRepository.incrementPopularity(
                        nearbyRemote.id,
                        nearbyRemote.popularity
                    )
                    routeRepository?.incrementLocalStopPopularity(nearbyRemote.id)
                    _stopUiState.value = StopUiState.Success(
                        "Ya existe una parada cerca. Se aumentó su popularidad."
                    )
                    return@launch
                }

                // Nueva solicitud pendiente de revisión
                val stop = RouteStop(
                    userId = userId,
                    routeId = routeId,
                    description = description.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    status = "pending",
                    popularity = 1
                )
                reportsRepository.suggestRouteStop(stop)
                _stopUiState.value = StopUiState.Success(
                    "Solicitud enviada. Un administrador la revisará."
                )
            } catch (e: Exception) {
                _stopUiState.value = StopUiState.Error(
                    mapStopError(e)
                )
            }
        }
    }

    private suspend fun trySyncRemotePopularity(
        routeId: Int,
        lat: Double,
        lng: Double,
        localStopId: String
    ) {
        try {
            val remote = reportsRepository.findNearbyStop(routeId, lat, lng)
            if (remote?.id != null) {
                reportsRepository.incrementPopularity(remote.id, remote.popularity)
            }
        } catch (_: Exception) {
            // La popularidad local ya se actualizó; remoto es best-effort
        }
    }

    /** CU-09 paso 4: sincronizar paradas aceptadas al almacenamiento local */
    fun syncAcceptedStopsToLocal(routeId: Int, onComplete: () -> Unit = {}) {
        if (!NetworkMonitor.isOnline() || routeRepository == null) return
        viewModelScope.launch {
            try {
                val accepted = reportsRepository.getAcceptedStopsForRoute(routeId)
                routeRepository.replaceStopsForRoute(routeId.toString(), accepted)
                onComplete()
            } catch (e: Exception) {
                println("CU-09 sync stops: ${e.message}")
            }
        }
    }

    fun resetState() {
        _uiState.value = ReportUiState.Idle
    }

    fun resetStopState() {
        _stopUiState.value = StopUiState.Idle
    }

    private fun mapStopError(e: Exception): String {
        val msg = e.message.orEmpty()
        return when {
            msg.contains("500", ignoreCase = true) ||
                msg.contains("502", ignoreCase = true) ||
                msg.contains("503", ignoreCase = true) ||
                msg.contains("service unavailable", ignoreCase = true) ->
                "El sistema de peticiones no está disponible. Intenta más tarde."
            else -> ErrorMapper.toUserMessage(e, "al registrar la parada")
        }
    }
}
