package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ── Modelos ───────────────────────────────────────────────────────────────────

@Serializable
data class RouteRating(
    val id: Int? = null,
    @SerialName("route_id")   val routeId: String,
    @SerialName("user_id")    val userId: String,
    val score: Int,
    val comment: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class RouteWithAvgRating(
    val id: String,
    val name: String,
    @SerialName("avg_score")    val avgScore: Double,
    @SerialName("rating_count") val ratingCount: Int
)

// ── Estados CU-23 ─────────────────────────────────────────────────────────────

sealed class RatingUiState {
    object Idle    : RatingUiState()
    object Loading : RatingUiState()
    object Success : RatingUiState()
    data class Error(val message: String) : RatingUiState()
}

// ── Estados CU-24 ─────────────────────────────────────────────────────────────

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

// ── ViewModel ─────────────────────────────────────────────────────────────────

class RatingViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client
    private val PAGE_SIZE = 20

    // ── CU-23 ────────────────────────────────────────────────────────────────

    private val _ratingState = MutableStateFlow<RatingUiState>(RatingUiState.Idle)
    val ratingState: StateFlow<RatingUiState> = _ratingState.asStateFlow()

    private val _currentUserScore = MutableStateFlow<Int?>(null)
    val currentUserScore: StateFlow<Int?> = _currentUserScore.asStateFlow()

    /** Carga calificación previa del usuario para la ruta (si existe) */
    fun loadUserRating(routeId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                val result = supabase.postgrest
                    .from("route_ratings")
                    .select(Columns.ALL) {
                        filter {
                            eq("route_id", routeId)
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    .decodeList<RouteRating>()
                _currentUserScore.value = result.firstOrNull()?.score
            } catch (_: Exception) {
                _currentUserScore.value = null
            }
        }
    }

    /**
     * CU-23 flujo normal: valida rango [1-5], guarda y notifica.
     * C23-2: score fuera de [1,5] → Error sin guardar.
     * C23-4: sin sesión activa → Error con aviso de login.
     */
    fun submitRating(routeId: String, score: Int, comment: String?) {
        // C23-4: usuario no autenticado
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            _ratingState.value = RatingUiState.Error("Debes iniciar sesión para calificar.")
            return
        }
        // C23-2: puntuación fuera de rango
        if (score !in 1..5) {
            _ratingState.value = RatingUiState.Error("Puntuación inválida. Elige entre 1 y 5 estrellas.")
            return
        }

        _ratingState.value = RatingUiState.Loading
        viewModelScope.launch {
            try {
                supabase.postgrest
                    .from("route_ratings")
                    .upsert(
                        RouteRating(
                            routeId = routeId,
                            userId  = userId,
                            score   = score,
                            comment = comment?.takeIf { it.isNotBlank() }
                        )
                    )
                _currentUserScore.value = score
                _ratingState.value = RatingUiState.Success
                // Refrescar top-rated para que el nuevo promedio se refleje
                refreshTopRated()
            } catch (e: Exception) {
                // C23-3: error de red
                _ratingState.value = RatingUiState.Error(
                    ErrorMapper.toUserMessage(e, "al guardar calificación")
                )
            }
        }
    }

    fun resetRatingState() {
        _ratingState.value = RatingUiState.Idle
        _currentUserScore.value = null
    }

    // ── CU-24 ────────────────────────────────────────────────────────────────

    private val _topRatedState = MutableStateFlow<TopRatedUiState>(TopRatedUiState.Idle)
    val topRatedState: StateFlow<TopRatedUiState> = _topRatedState.asStateFlow()

    private var currentPage = 0
    private val loadedRoutes = mutableListOf<RouteWithAvgRating>()
    var cityFilter: String? = null
        private set

    /** C24-1 + C24-3: carga inicial o incremental ordenada por promedio desc */
    fun loadTopRated(refresh: Boolean = false) {
        if (_topRatedState.value is TopRatedUiState.Loading) return
        if (refresh) {
            currentPage = 0
            loadedRoutes.clear()
        }
        _topRatedState.value = TopRatedUiState.Loading
        val from = (currentPage * PAGE_SIZE).toLong()
        val to   = (from + PAGE_SIZE - 1)
        viewModelScope.launch {
            try {
                val newRoutes = supabase.postgrest
                    .from("route_ratings_summary")
                    .select(Columns.ALL) {
                        if (cityFilter != null) filter { eq("city", cityFilter!!) }
                        order("avg_score", Order.DESCENDING)
                        range(from, to)
                    }
                    .decodeList<RouteWithAvgRating>()

                loadedRoutes.addAll(newRoutes)
                currentPage++
                _topRatedState.value = if (loadedRoutes.isEmpty()) {
                    // C24-2: ninguna ruta tiene calificaciones
                    TopRatedUiState.Empty
                } else {
                    TopRatedUiState.Success(
                        routes  = loadedRoutes.toList(),
                        hasMore = newRoutes.size == PAGE_SIZE
                    )
                }
            } catch (e: Exception) {
                // C24-5: error de conexión
                _topRatedState.value = TopRatedUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cargar rutas")
                )
            }
        }
    }

    /** C24-4: aplica filtro por ciudad y recarga desde cero */
    fun applyCityFilter(city: String?) {
        cityFilter = city
        loadTopRated(refresh = true)
    }

    private fun refreshTopRated() {
        if (_topRatedState.value !is TopRatedUiState.Idle) {
            loadTopRated(refresh = true)
        }
    }
}
