package com.example.xalabus.ui.admin

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

// Credenciales del administrador (locales, sin Supabase)
private const val ADMIN_USER = "admin"
private const val ADMIN_PASS = "admin"

sealed class AdminUiState {
    object Idle    : AdminUiState()
    object Loading : AdminUiState()
    object Success : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

class AdminViewModel {

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    private var _isAuthenticated = false
    val isAuthenticated: Boolean get() = _isAuthenticated

    fun signIn(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AdminUiState.Error("Completa todos los campos.")
            return
        }
        _uiState.value = AdminUiState.Loading
        if (username.trim() == ADMIN_USER && password == ADMIN_PASS) {
            _isAuthenticated = true
            _uiState.value = AdminUiState.Success
        } else {
            _uiState.value = AdminUiState.Error("Usuario o contraseña incorrectos.")
        }
    }

    fun signOut() {
        _isAuthenticated = false
        _uiState.value = AdminUiState.Idle
    }

    fun resetState() {
        _uiState.value = AdminUiState.Idle
    }
}
