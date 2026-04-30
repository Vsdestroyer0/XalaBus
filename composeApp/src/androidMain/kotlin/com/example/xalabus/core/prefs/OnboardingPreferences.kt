package com.example.xalabus.core.prefs

import android.content.Context

// Contexto proporcionado desde MainActivity al iniciar la app
internal lateinit var appContext: Context

actual object OnboardingPreferences {
    private const val PREFS_NAME = "xalabus_prefs"
    private const val KEY_ONBOARDING = "onboarding_completed"

    actual fun isOnboardingCompleted(): Boolean {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getBoolean(KEY_ONBOARDING, false)
    }

    actual fun markOnboardingCompleted() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(KEY_ONBOARDING, true).apply()
    }
}
