package com.example.xalabus.core.util

/** CU-09 Ex-02: comprobación de conectividad por plataforma. */
expect object NetworkMonitor {
    fun isOnline(): Boolean
}
