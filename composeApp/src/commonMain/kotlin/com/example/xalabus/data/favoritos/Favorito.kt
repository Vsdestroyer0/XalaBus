package com.example.xalabus.data.favoritos

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * CU-10: Modelo de la tabla `favoritos` en Supabase.
 * Representa una ruta guardada como favorita por un usuario autenticado.
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
