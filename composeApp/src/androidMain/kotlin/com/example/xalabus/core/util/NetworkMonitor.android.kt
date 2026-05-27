package com.example.xalabus.core.util

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.xalabus.core.prefs.appContext

actual object NetworkMonitor {
    actual fun isOnline(): Boolean {
        val cm = appContext.getSystemService(ConnectivityManager::class.java) ?: return false
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
