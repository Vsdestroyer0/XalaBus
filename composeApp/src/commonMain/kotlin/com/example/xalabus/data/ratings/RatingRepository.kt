package com.example.xalabus.data.ratings

import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteRating(
    val id: Int? = null,
    @SerialName("route_id")   val routeId: String,
    @SerialName("route_name") val routeName: String,
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

class RatingRepository {

    private val supabase get() = SupabaseClientProvider.client

    /** Guarda (o actualiza vía upsert) la calificación del usuario para una ruta */
    suspend fun saveRating(
        routeId: String,
        routeName: String,
        userId: String,
        score: Int,
        comment: String?
    ): Result<Unit> = withContext(Dispatchers.Default) {
        runCatching {
            supabase.postgrest["route_ratings"].upsert(
                RouteRating(
                    routeId   = routeId,
                    routeName = routeName,
                    userId    = userId,
                    score     = score,
                    comment   = comment?.takeIf { it.isNotBlank() }
                )
            )
            Unit
        }
    }

    /** Obtiene la calificación previa del usuario para una ruta (null si no existe) */
    suspend fun getUserRating(routeId: String, userId: String): Result<RouteRating?> =
        withContext(Dispatchers.Default) {
            runCatching {
                supabase.postgrest["route_ratings"]
                    .select {
                        filter {
                            eq("route_id", routeId)
                            eq("user_id", userId)
                        }
                        limit(1)
                    }
                    .decodeSingleOrNull<RouteRating>()
            }
        }

    /**
     * Rutas mejor calificadas, ordenadas por promedio descendente.
     * Soporta paginación (page * pageSize) y filtro opcional por ciudad.
     */
    suspend fun getTopRatedRoutes(
        page: Int = 0,
        pageSize: Int = 20,
        cityFilter: String? = null
    ): Result<List<RouteWithAvgRating>> = withContext(Dispatchers.Default) {
        runCatching {
            val from = page * pageSize
            val to   = from + pageSize - 1
            supabase.postgrest["route_ratings_summary"]
                .select {
                    if (cityFilter != null) filter { eq("city", cityFilter) }
                    order("avg_score", Order.DESCENDING)
                    range(from.toLong(), to.toLong())
                }
                .decodeList<RouteWithAvgRating>()
        }
    }
}
