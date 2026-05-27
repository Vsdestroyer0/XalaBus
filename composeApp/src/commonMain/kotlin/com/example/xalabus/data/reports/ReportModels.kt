package com.example.xalabus.data.reports

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GeneralReport(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val message: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class RouteReport(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("route_id")
    val routeId: Int,
    val message: String,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class RouteStop(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    @SerialName("route_id")
    val routeId: Int,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val status: String = "pending",
    val popularity: Int = 1,
    @SerialName("created_at")
    val createdAt: String? = null
)

@Serializable
data class PopularityUpdate(
    val popularity: Int
)
