package com.example.xalabus.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteJson(
    val id: Long,
    val name: String,
    val desc: String? = null,
    val fare: String? = "$12.00",
    val frequency: String? = "15 min",
    val variants: List<VariantJson> = emptyList()
)

@Serializable
data class VariantJson(
    val direction: String,
    val coords: List<List<Double>>
)

// Añádela aquí abajo


@Serializable
data class StopJson(
    val id: String,
    val lat: Double,
    val lng: Double,
    @SerialName("rId")
    val routeId: String = ""
)