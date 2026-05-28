package com.example.xalabus.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.postgrest
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
    @SerialName("route_name") val routeName: String = "",
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

    /**
     * Carga calificación previa del usuario usando la RPC get_user_route_rating.
     * Evita exponer la tabla directamente y aprovecha SECURITY DEFINER.
     */
    fun loadUserRating(routeId: String) {
        val userId = supabase.auth.currentSessionOrNull()?.user?.id ?: return
        viewModelScope.launch {
            try {
                val result = supabase.postgrest
                    .rpc(
                        "get_user_route_rating",
                        mapOf(
                            "p_route_id" to routeId,
                            "p_user_id"  to userId
                        )
                    )
                    .decodeList<RouteRating>()
                _currentUserScore.value = result.firstOrNull()?.score
            } catch (_: Exception) {
                _currentUserScore.value = null
            }
        }
    }

    /**
     * CU-23 flujo normal: valida rango [1-5], hace upsert y notifica.
     * C23-2: score fuera de [1,5]  → Error sin guardar.
     * C23-4: sin sesión activa     → Error con aviso de login.
     * C23-3: error de red          → Error con mensaje para reintentar.
     */
    fun submitRating(routeId: String, routeName: String, score: Int, comment: String?) {
        // C23-4
        val userId = supabase.auth.currentSessionOrNull()?.user?.id
        if (userId == null) {
            _ratingState.value = RatingUiState.Error("Debes iniciar sesión para calificar.")
            return
        }
        // C23-2
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
                            routeId   = routeId,
                            routeName = routeName,
                            userId    = userId,
                            score     = score,
                            comment   = comment?.takeIf { it.isNotBlank() }
                        )
                    ) {
                        onConflict = "route_id,user_id"
                    }
                _currentUserScore.value = score
                _ratingState.value = RatingUiState.Success
                // Refrescar top-rated para reflejar el nuevo promedio
                refreshTopRated()
            } catch (e: Exception) {
                // C23-3
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

    /**
     * C24-1 + C24-3: carga paginada usando la RPC get_top_rated_routes.
     * La RPC devuelve resultados ya ordenados por avg_score DESC.
     * C24-5: si falla la conexión, emite TopRatedUiState.Error.
     */
    fun loadTopRated(refresh: Boolean = false) {
        if (_topRatedState.value is TopRatedUiState.Loading) return
        if (refresh) {
            currentPage = 0
            loadedRoutes.clear()
        }
        _topRatedState.value = TopRatedUiState.Loading
        val offset = currentPage * PAGE_SIZE
        viewModelScope.launch {
            try {
                val newRoutes = supabase.postgrest
                    .rpc(
                        "get_top_rated_routes",
                        mapOf(
                            "p_limit"  to PAGE_SIZE,
                            "p_offset" to offset
                        )
                    )
                    .decodeList<RouteWithAvgRating>()

                loadedRoutes.addAll(newRoutes)
                currentPage++
                _topRatedState.value = if (loadedRoutes.isEmpty()) {
                    // C24-2: ninguna ruta tiene calificaciones aún
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
