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

/**
 * Estados posibles del flujo de recuperación de contraseña (CU-03).
 *
 * Flujo principal:
 *   Idle → [ingresar correo] → Loading → CodeSent → [ingresar OTP] → Loading → PasswordChanged
 *
 * Flujos alternativos:
 *   FA-01: correo no encontrado  → Error
 *   FA-02: código inválido/expirado → Error
 *   Ex-01: error de red/servidor  → Error
 */
sealed class ForgotPasswordUiState {
    /** Estado inicial, sin acción en curso */
    object Idle : ForgotPasswordUiState()

    /** Petición en proceso (spinner) */
    object Loading : ForgotPasswordUiState()

    /** Correo enviado correctamente — mostrar campo OTP */
    data class CodeSent(val email: String) : ForgotPasswordUiState()

    /** Contraseña cambiada exitosamente */
    object PasswordChanged : ForgotPasswordUiState()

    /** Error en cualquier paso del flujo */
    data class Error(val message: String) : ForgotPasswordUiState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /**
     * Paso 1 — Enviar correo con código OTP de recuperación.
     * FA-01: si el correo no está registrado Supabase puede no lanzar error visible,
     *        pero el usuario no recibirá el código.
     */
    fun sendPasswordResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Por favor ingresa tu correo electrónico.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(email.trim())
                _uiState.value = ForgotPasswordUiState.CodeSent(email.trim())
            } catch (e: Exception) {
                // FA-01: correo no encontrado u otros errores de red (Ex-01)
                _uiState.value = ForgotPasswordUiState.Error(
                    e.message ?: "Error al enviar el correo. Verifica la dirección ingresada."
                )
            }
        }
    }

    /**
     * Paso 2 — Verificar el código OTP y establecer la nueva contraseña.
     * FA-02: token inválido o expirado → Supabase lanza excepción.
     */
    fun verifyOtpAndChangePassword(
        email: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ) {
        when {
            otp.isBlank() -> {
                _uiState.value = ForgotPasswordUiState.Error("Ingresa el código recibido en tu correo.")
                return
            }
            newPassword.isBlank() -> {
                _uiState.value = ForgotPasswordUiState.Error("La nueva contraseña no puede estar vacía.")
                return
            }
            newPassword.length < 6 -> {
                _uiState.value = ForgotPasswordUiState.Error("La contraseña debe tener al menos 6 caracteres.")
                return
            }
            newPassword != confirmPassword -> {
                _uiState.value = ForgotPasswordUiState.Error("Las contraseñas no coinciden.")
                return
            }
        }

        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                // Verificar el OTP de tipo "recovery" con el email
                supabase.auth.verifyEmailOtp(
                    type = io.github.jan.supabase.auth.providers.builtin.OtpType.Email.RECOVERY,
                    email = email.trim(),
                    token = otp.trim()
                )
                // Una vez verificado, actualizar la contraseña del usuario
                supabase.auth.updateUser {
                    password = newPassword
                }
                _uiState.value = ForgotPasswordUiState.PasswordChanged
            } catch (e: Exception) {
                // FA-02: código inválido/expirado (Ex-01)
                _uiState.value = ForgotPasswordUiState.Error(
                    e.message ?: "Código incorrecto o expirado. Solicita uno nuevo."
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ForgotPasswordUiState.Idle
    }
}
