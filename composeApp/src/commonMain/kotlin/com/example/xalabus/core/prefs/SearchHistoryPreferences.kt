package com.example.xalabus.core.prefs

expect object SearchHistoryPreferences {
    fun getSearchHistory(): List<String>
    fun saveSearchQuery(query: String)
    fun deleteSearchQuery(query: String)
    fun clearSearchHistory()
}
