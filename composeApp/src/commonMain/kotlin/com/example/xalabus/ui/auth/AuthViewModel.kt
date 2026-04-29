package com.example.xalabus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Estados posibles de la UI de autenticación */
sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    object Success : AuthUiState()
    data class Error(val message: String) : AuthUiState()
}

class AuthViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    /** Devuelve true si ya hay una sesión activa al iniciar la app */
    fun isSessionActive(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    /** Inicia sesión con email y contraseña */
    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Por favor completa todos los campos.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Error al iniciar sesión. Verifica tus credenciales."
                )
            }
        }
    }

    /** Registra un nuevo usuario con email y contraseña */
    fun signUp(email: String, password: String, confirmPassword: String) {
        if (email.isBlank() || password.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = AuthUiState.Error("Por favor completa todos los campos.")
            return
        }
        if (password != confirmPassword) {
            _uiState.value = AuthUiState.Error("Las contraseñas no coinciden.")
            return
        }
        if (password.length < 6) {
            _uiState.value = AuthUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        _uiState.value = AuthUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.signUpWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                _uiState.value = AuthUiState.Success
            } catch (e: Exception) {
                _uiState.value = AuthUiState.Error(
                    e.message ?: "Error al registrarse. Intenta con otro correo."
                )
            }
        }
    }

    /** Cierra sesión del usuario actual */
    fun signOut() {
        viewModelScope.launch {
            try {
                supabase.auth.signOut()
            } catch (_: Exception) { }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}
