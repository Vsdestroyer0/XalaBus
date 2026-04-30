package com.example.xalabus.core.prefs

import platform.Foundation.NSUserDefaults

actual object OnboardingPreferences {
    private const val KEY_ONBOARDING = "onboarding_completed"

    actual fun isOnboardingCompleted(): Boolean {
        return NSUserDefaults.standardUserDefaults.boolForKey(KEY_ONBOARDING)
    }

    actual fun markOnboardingCompleted() {
        NSUserDefaults.standardUserDefaults.setBool(true, KEY_ONBOARDING)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
