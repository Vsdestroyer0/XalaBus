package com.example.xalabus

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform