package com.example.xalabus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Estados del flujo de recuperación de contraseña (CU-03).
 * FA-01: correo no encontrado → Error
 * FA-02: código inválido → Error
 * Ex-01: error de red/servidor → Error
 */
sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    /** Correo enviado correctamente — pasar a ingreso del código OTP */
    object EmailSent : ForgotPasswordUiState()
    /** OTP verificado — el usuario puede cambiar su contraseña */
    object OtpVerified : ForgotPasswordUiState()
    /** Contraseña cambiada exitosamente */
    object PasswordChanged : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** Correo ingresado por el usuario (se guarda para la etapa de verificación OTP) */
    private var currentEmail: String = ""

    /**
     * CU-03 — Paso 1: Envía el código de recuperación al correo.
     * FA-01: correo no encontrado → Supabase devuelve error → Error.
     */
    fun sendRecoveryEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa tu correo electrónico.")
            return
        }
        currentEmail = email.trim()
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(currentEmail)
                _uiState.value = ForgotPasswordUiState.EmailSent
            } catch (e: Exception) {
                // FA-01 / Ex-01
                _uiState.value = ForgotPasswordUiState.Error(
                    "No se pudo enviar el correo: ${e.message}"
                )
            }
        }
    }

    /**
     * CU-03 — Paso 2: Verifica el OTP recibido por correo.
     * FA-02: código inválido → Supabase devuelve error → Error.
     */
    fun verifyOtp(otp: String) {
        if (otp.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa el código de verificación.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.verifyEmailOtp(
                    type  = io.github.jan.supabase.auth.OtpType.Email.RECOVERY,
                    email = currentEmail,
                    token = otp.trim()
                )
                _uiState.value = ForgotPasswordUiState.OtpVerified
            } catch (e: Exception) {
                // FA-02 / Ex-01
                _uiState.value = ForgotPasswordUiState.Error(
                    "Código inválido o expirado: ${e.message}"
                )
            }
        }
    }

    /**
     * CU-03 — Paso 3: Actualiza la contraseña tras verificar el OTP.
     * Postcondición: usuario puede acceder con la nueva contraseña.
     */
    fun updatePassword(newPassword: String) {
        if (newPassword.length < 6) {
            _uiState.value = ForgotPasswordUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.updateUser { password = newPassword }
                _uiState.value = ForgotPasswordUiState.PasswordChanged
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error(
                    "Error al cambiar contraseña: ${e.message}"
                )
            }
        }
    }

    fun resetState() { _uiState.value = ForgotPasswordUiState.Idle }
}
