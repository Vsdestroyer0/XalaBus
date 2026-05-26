package com.example.xalabus.core.prefs

import platform.Foundation.NSUserDefaults
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

actual object SearchHistoryPreferences {
    private const val KEY_HISTORY = "search_history"

    actual fun getSearchHistory(): List<String> {
        val json = NSUserDefaults.standardUserDefaults.stringForKey(KEY_HISTORY) ?: return emptyList()
        return try {
            Json.decodeFromString<List<String>>(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    actual fun saveSearchQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val current = getSearchHistory().toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        if (current.size > 10) {
            current.removeAt(current.size - 1)
        }
        val json = Json.encodeToString(current)
        NSUserDefaults.standardUserDefaults.setObject(json, KEY_HISTORY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun deleteSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        val json = Json.encodeToString(current)
        NSUserDefaults.standardUserDefaults.setObject(json, KEY_HISTORY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }

    actual fun clearSearchHistory() {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(KEY_HISTORY)
        NSUserDefaults.standardUserDefaults.synchronize()
    }
}
