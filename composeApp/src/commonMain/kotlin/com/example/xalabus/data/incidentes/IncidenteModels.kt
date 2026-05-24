package com.example.xalabus.data.incidentes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de la tabla `reportes` en Supabase (CU-13).
 * El campo foto_url es opcional (null si no se adjunta foto).
 * estado: 'pendiente' por defecto.
 */
@Serializable
data class Incidente(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    @SerialName("foto_url")
    val fotoUrl: String? = null,
    val estado: String = "pendiente",
    @SerialName("created_at")
    val createdAt: String? = null
)
