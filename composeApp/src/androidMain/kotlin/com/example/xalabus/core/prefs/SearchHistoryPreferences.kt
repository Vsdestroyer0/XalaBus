package com.example.xalabus.core.prefs

import android.content.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

actual object SearchHistoryPreferences {
    private const val PREFS_NAME = "xalabus_prefs"
    private const val KEY_HISTORY = "search_history"

    actual fun getSearchHistory(): List<String> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_HISTORY, null) ?: return emptyList()
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
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    actual fun deleteSearchQuery(query: String) {
        val current = getSearchHistory().toMutableList()
        current.remove(query)
        val json = Json.encodeToString(current)
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_HISTORY, json).apply()
    }

    actual fun clearSearchHistory() {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
