package com.example.xalabus.data.favoritos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de la tabla `favoritos` en Supabase (CU-10).
 * user_id referencia auth.users(id) ON DELETE CASCADE.
 */
@Serializable
data class Favorito(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("route_id")
    val routeId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
