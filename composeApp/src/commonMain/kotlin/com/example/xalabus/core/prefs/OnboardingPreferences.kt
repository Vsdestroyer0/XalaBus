package com.example.xalabus.core.prefs

/**
 * Maneja la preferencia de si el usuario ya completó el walkthrough.
 * - Android: usa SharedPreferences
 * - iOS: usa NSUserDefaults
 */
expect object OnboardingPreferences {
    fun isOnboardingCompleted(): Boolean
    fun markOnboardingCompleted()
}
