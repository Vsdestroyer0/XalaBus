package com.example.xalabus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.OtpType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * CU-03: Estados de la pantalla de recuperación de contraseña.
 * Flujo: IDLE → EMAIL_SENT → OTP_VERIFIED → PASSWORD_CHANGED
 */
sealed class ForgotPasswordUiState {
    object Idle : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    /** Correo enviado, esperando que el usuario ingrese el código OTP */
    object EmailSent : ForgotPasswordUiState()
    /** OTP verificado, el usuario puede ingresar la nueva contraseña */
    object OtpVerified : ForgotPasswordUiState()
    /** Contraseña cambiada con éxito */
    object PasswordChanged : ForgotPasswordUiState()
    data class Error(val message: String) : ForgotPasswordUiState()
}

class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    // Almacena el email para reutilizarlo en verifyOtp
    private var pendingEmail: String = ""

    /**
     * CU-03 Paso 1: Envía correo de recuperación con código OTP.
     * FA-01: correo no encontrado → ErrorMapper lo traduce.
     */
    fun sendPasswordReset(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa tu correo electrónico.")
            return
        }
        pendingEmail = email.trim()
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(pendingEmail)
                _uiState.value = ForgotPasswordUiState.EmailSent
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error(
                    ErrorMapper.toUserMessage(e, "al enviar el correo")
                )
            }
        }
    }

    /**
     * CU-03 Paso 2: Verifica el código OTP recibido por correo.
     * FA-02: código inválido → ErrorMapper lo traduce.
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
                    type  = OtpType.Email.RECOVERY,
                    email = pendingEmail,
                    token = otp.trim()
                )
                _uiState.value = ForgotPasswordUiState.OtpVerified
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error(
                    ErrorMapper.toUserMessage(e, "al verificar el código")
                )
            }
        }
    }

    /**
     * CU-03 Paso 3: Actualiza la contraseña del usuario autenticado vía OTP.
     */
    fun updatePassword(newPassword: String, confirmPassword: String) {
        if (newPassword.isBlank() || confirmPassword.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Por favor completa ambos campos.")
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
                _uiState.value = ForgotPasswordUiState.Error(
                    ErrorMapper.toUserMessage(e, "al cambiar la contraseña")
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = ForgotPasswordUiState.Idle
        pendingEmail = ""
    }
}
