package com.example.xalabus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteJson(
    val id: Long,
    val name: String,
    val desc: String? = null,
    val fare: String? = "$9.00",
    val frequency: String? = "15 min",
    val variants: List<VariantJson>
)
@Serializable
data class VariantJson(
    val direction: String,
    val coords: List<List<Double>>
)
