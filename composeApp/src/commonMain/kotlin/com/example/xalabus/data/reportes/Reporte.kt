package com.example.xalabus.data.reportes

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * CU-13: Modelo de la tabla `reportes` en Supabase.
 * Representa un inconveniente reportado por un usuario autenticado.
 */
@Serializable
data class Reporte(
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
