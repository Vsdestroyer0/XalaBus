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

/** Estados posibles del flujo de recuperación de contraseña */
sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    /** Correo de recuperación enviado exitosamente — paso 1 completado */
    object EmailSent : ForgotPasswordUiState()
    /** OTP verificado — paso 2 completado, usuario puede cambiar contraseña */
    object OtpVerified : ForgotPasswordUiState()
    /** Contraseña cambiada exitosamente — flujo completado */
    object PasswordChanged : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /**
     * Paso 1: Envía un correo con código OTP al email proporcionado.
     * FA-01: Si el correo no existe en Supabase Auth, el servicio igualmente responde OK
     * por seguridad; el usuario verá un mensaje genérico.
     */
    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Por favor ingresa tu correo electrónico.")
            return
        }
        val emailTrimmed = email.trim()
        if (!emailTrimmed.contains("@")) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa un correo electrónico válido.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(emailTrimmed)
                _uiState.value = ForgotPasswordUiState.EmailSent
            } catch (e: Exception) {
                // Ex-01: error de red o servicio
                _uiState.value = ForgotPasswordUiState.Error(
                    "No se pudo enviar el correo. Verifica tu conexión e intenta nuevamente."
                )
            }
        }
    }

    /**
     * Paso 2: Verifica el código OTP de 6 dígitos enviado al correo.
     * FA-02: Código inválido o expirado → estado Error con mensaje claro.
     */
    fun verifyOtp(email: String, token: String) {
        if (token.isBlank() || token.length < 6) {
            _uiState.value = ForgotPasswordUiState.Error("El código debe tener 6 dígitos.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.verifyEmailOtp(
                    type = io.github.jan.supabase.auth.user.UserTokenType.Recovery,
                    email = email.trim(),
                    token = token.trim()
                )
                _uiState.value = ForgotPasswordUiState.OtpVerified
            } catch (e: Exception) {
                // FA-02: código inválido o expirado
                _uiState.value = ForgotPasswordUiState.Error(
                    "Código incorrecto o expirado. Solicita uno nuevo."
                )
            }
        }
    }

    /**
     * Paso 3: Actualiza la contraseña del usuario (requiere sesión activa post-OTP).
     */
    fun updatePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Por favor completa todos los campos.")
            return
        }
        if (newPassword != confirmPassword) {
            _uiState.value = ForgotPasswordUiState.Error("Las contraseñas no coinciden.")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = ForgotPasswordUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.updateUser {
                    password = newPassword
                }
                _uiState.value = ForgotPasswordUiState.PasswordChanged
            } catch (e: Exception) {
                // Ex-01: error al actualizar
                _uiState.value = ForgotPasswordUiState.Error(
                    "Error al cambiar la contraseña. Intenta nuevamente."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ForgotPasswordUiState.Idle
    }
}
