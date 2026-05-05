package com.example.xalabus.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ─────────────────────────────────────────────────────────────────────────────
//  Código de acceso que el administrador debe ingresar además de sus
//  credenciales de Supabase. Cámbialo por el valor que prefieras.
// ─────────────────────────────────────────────────────────────────────────────
private const val ADMIN_SECRET_CODE = "XALA-ADMIN-2025"

sealed class AdminUiState {
    object Idle    : AdminUiState()
    object Loading : AdminUiState()
    object Success : AdminUiState()
    data class Error(val message: String) : AdminUiState()
}

class AdminViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<AdminUiState>(AdminUiState.Idle)
    val uiState: StateFlow<AdminUiState> = _uiState.asStateFlow()

    /**
     * Inicia sesión como administrador.
     * Valida primero el código secreto antes de llamar a Supabase,
     * así evitamos peticiones innecesarias con códigos incorrectos.
     */
    fun signInAsAdmin(email: String, password: String, adminCode: String) {
        if (email.isBlank() || password.isBlank() || adminCode.isBlank()) {
            _uiState.value = AdminUiState.Error("Completa todos los campos.")
            return
        }
        if (adminCode.trim() != ADMIN_SECRET_CODE) {
            _uiState.value = AdminUiState.Error("Código de administrador incorrecto.")
            return
        }
        _uiState.value = AdminUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email    = email.trim()
                    this.password = password
                }
                _uiState.value = AdminUiState.Success
            } catch (e: Exception) {
                _uiState.value = AdminUiState.Error(
                    e.message ?: "Credenciales inválidas. Verifica tu cuenta."
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try { supabase.auth.signOut() } catch (_: Exception) { }
        }
    }

    fun resetState() {
        _uiState.value = AdminUiState.Idle
    }

    fun isAdminSessionActive(): Boolean =
        supabase.auth.currentSessionOrNull() != null
}
