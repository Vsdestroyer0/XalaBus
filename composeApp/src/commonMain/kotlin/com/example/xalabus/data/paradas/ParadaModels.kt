package com.example.xalabus.data.paradas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Modelo de la tabla `paradas` en Supabase (CU-12).
 * Solo accesible para agregar por rol admin/developer.
 */
@Serializable
data class Parada(
    val id: String? = null,
    val nombre: String,
    val latitud: Double,
    val longitud: Double,
    @SerialName("ruta_id")
    val rutaId: String,
    @SerialName("created_at")
    val createdAt: String? = null
)
