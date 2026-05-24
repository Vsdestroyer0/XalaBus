package com.example.xalabus.data.paradas

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * CU-12: Modelo de la tabla `paradas` en Supabase.
 * Representa una parada de camiún registrada por un administrador.
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
