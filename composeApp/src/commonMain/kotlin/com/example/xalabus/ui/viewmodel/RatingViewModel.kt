package com.example.xalabus.ui.viewmodel

import com.example.xalabus.data.ratings.RatingRepository
import com.example.xalabus.data.ratings.RouteWithAvgRating
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Estados CU-23 (Ratear ruta) ─────────────────────────────────────────────
sealed class RatingUiState {
    object Idle    : RatingUiState()
    object Loading : RatingUiState()
    object Success : RatingUiState()
    data class Error(val message: String) : RatingUiState()
}

// ── Estados CU-24 (Visualizar rutas mejor rateadas) ──────────────────────
sealed class TopRatedUiState {
    object Idle    : TopRatedUiState()
    object Loading : TopRatedUiState()
    object Empty   : TopRatedUiState()
    data class Success(
        val routes: List<RouteWithAvgRating>,
        val hasMore: Boolean
    ) : TopRatedUiState()
    data class Error(val message: String) : TopRatedUiState()
}

class RatingViewModel(
    private val repository: RatingRepository = RatingRepository()
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // ── CU-23 ───────────────────────────────────────────────────────────────
    private val _ratingState = MutableStateFlow<RatingUiState>(RatingUiState.Idle)
    val ratingState: StateFlow<RatingUiState> = _ratingState.asStateFlow()

    private val _currentUserScore = MutableStateFlow<Int?>(null)
    val currentUserScore: StateFlow<Int?> = _currentUserScore.asStateFlow()

    /** Carga la calificación previa del usuario para mostrarla en el diálogo */
    fun loadUserRating(routeId: String, userId: String) {
        scope.launch {
            repository.getUserRating(routeId, userId)
                .onSuccess { _currentUserScore.value = it?.score }
        }
    }

    /**
     * CU-23 flujo normal: valida rango [1-5], sanitiza comentario, guarda y notifica.
     * C23-2: score fuera de [1,5] → Error sin guardar.
     * C23-3: fallo de red → Error con mensaje claro de reintento.
     * C23-4: sin userId → Error "Debes iniciar sesión".
     * C23-5: comentario sanitizado (trim) antes de persistir.
     */
    fun submitRating(
        routeId: String,
        routeName: String,
        userId: String?,
        score: Int,
        comment: String?
    ) {
        // C23-4: usuario no autenticado
        if (userId.isNullOrBlank()) {
            _ratingState.value = RatingUiState.Error("Debes iniciar sesión para calificar.")
            return
        }
        // C23-2: puntuación fuera de rango
        if (score !in 1..5) {
            _ratingState.value = RatingUiState.Error("Puntuación inválida. Elige entre 1 y 5.")
            return
        }

        // C23-5: sanitizar comentario (trim + null si está vacío tras limpieza)
        val sanitizedComment = comment?.trim()?.takeIf { it.isNotBlank() }

        _ratingState.value = RatingUiState.Loading
        scope.launch {
            repository.saveRating(routeId, routeName, userId, score, sanitizedComment)
                .onSuccess {
                    _currentUserScore.value = score
                    _ratingState.value = RatingUiState.Success
                    loadTopRated(refresh = true) // actualiza el ranking tras calificar
                }
                .onFailure { e ->
                    // C23-3: error de red — mensaje explícito para el usuario
                    val msg = when {
                        e.message?.contains("network", ignoreCase = true) == true ||
                        e.message?.contains("connect", ignoreCase = true) == true ||
                        e.message?.contains("timeout", ignoreCase = true) == true ->
                            "Sin conexión. Verifica tu red e intenta de nuevo."
                        else -> e.message ?: "Error al guardar la calificación. Intenta de nuevo."
                    }
                    _ratingState.value = RatingUiState.Error(msg)
                }
        }
    }

    fun resetRatingState() {
        _ratingState.value = RatingUiState.Idle
        _currentUserScore.value = null
    }

    // ── CU-24 ───────────────────────────────────────────────────────────────
    private val _topRatedState = MutableStateFlow<TopRatedUiState>(TopRatedUiState.Idle)
    val topRatedState: StateFlow<TopRatedUiState> = _topRatedState.asStateFlow()

    private var currentPage  = 0
    private val pageSize     = 20
    private val loadedRoutes = mutableListOf<RouteWithAvgRating>()
    var cityFilter: String?  = null

    /** C24-1 + C24-3: carga inicial o recarga con paginación incremental */
    fun loadTopRated(refresh: Boolean = false) {
        if (refresh) {
            currentPage = 0
            loadedRoutes.clear()
        }
        // Evita cargas duplicadas mientras ya está cargando
        if (_topRatedState.value is TopRatedUiState.Loading) return

        _topRatedState.value = TopRatedUiState.Loading
        scope.launch {
            repository.getTopRatedRoutes(currentPage, pageSize, cityFilter)
                .onSuccess { newRoutes ->
                    loadedRoutes.addAll(newRoutes)
                    currentPage++
                    _topRatedState.value = when {
                        loadedRoutes.isEmpty() -> TopRatedUiState.Empty  // C24-2
                        else -> TopRatedUiState.Success(
                            routes  = loadedRoutes.toList(),
                            hasMore = newRoutes.size == pageSize
                        )
                    }
                }
                .onFailure { e ->
                    // C24-5: error de conexión — si hay datos en caché (loadedRoutes) los conserva
                    if (loadedRoutes.isNotEmpty()) {
                        _topRatedState.value = TopRatedUiState.Success(
                            routes  = loadedRoutes.toList(),
                            hasMore = false
                        )
                    } else {
                        _topRatedState.value = TopRatedUiState.Error(
                            e.message ?: "Error al cargar datos. Verifica tu conexión."
                        )
                    }
                }
        }
    }

    /** C24-4: aplica filtro por ciudad y recarga desde el inicio */
    fun applyCityFilter(city: String?) {
        cityFilter = city?.trim()?.takeIf { it.isNotBlank() }
        loadTopRated(refresh = true)
    }
}
