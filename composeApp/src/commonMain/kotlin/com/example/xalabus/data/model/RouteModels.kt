package com.example.xalabus.data.model

import kotlinx.serialization.Serializable

@Serializable
data class RouteJson(
    val id: String,
    val name: String,
    val variants: List<VariantJson>
)

@Serializable
data class VariantJson(
    val direction: String,
    val coords: List<List<Double>>
)