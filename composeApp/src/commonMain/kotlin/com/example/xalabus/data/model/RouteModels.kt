package com.example.xalabus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteJson(
    val id: Long,
    val name: String,
    val desc: String? = null,
    val variants: List<VariantJson>
)

@Serializable
data class VariantJson(
    val direction: String,
    val coords: List<List<Double>>
)
