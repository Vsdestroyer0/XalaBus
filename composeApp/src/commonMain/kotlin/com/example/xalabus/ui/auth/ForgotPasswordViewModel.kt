package com.example.xalabus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
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

/**
 * Palabras clave que Supabase/GoTrue incluye en el mensaje de error
 * cuando se intenta actualizar la contraseña con la misma contraseña actual.
 */
private val SAME_PASSWORD_KEYWORDS = listOf(
    "same password",
    "different from the old password",
    "should be different from the old password",
    "new password should be different"
)

private fun isSamePasswordError(e: Exception): Boolean {
    val msg = e.message?.lowercase() ?: return false
    return SAME_PASSWORD_KEYWORDS.any { keyword -> msg.contains(keyword) }
}

class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    // Almacena el email para reutilizarlo en verifyOtp
    private var pendingEmail: String = ""

    // Guarda el accessToken que Supabase devuelve al verificar el OTP de recuperación.
    // Necesario porque verifyEmailOtp establece una sesión temporal que puede no
    // persistir entre corrutinas en el SessionManager de KMP.
    private var recoveryAccessToken: String? = null

    /**
     * CU-03 Paso 1: Envía correo de recuperación con código OTP.
     * FA-01: correo no encontrado → ErrorMapper lo traduce.
     */
    fun sendRecoveryEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa tu correo electrónico.")
            return
        }
        pendingEmail = email.trim()
        recoveryAccessToken = null
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
     * Guarda el accessToken de la sesión establecida para usarlo en updatePassword.
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
                recoveryAccessToken = supabase.auth.currentSessionOrNull()?.accessToken
                if (recoveryAccessToken == null) {
                    _uiState.value = ForgotPasswordUiState.Error(
                        "No se pudo establecer la sesión de recuperación. Intenta de nuevo."
                    )
                } else {
                    _uiState.value = ForgotPasswordUiState.OtpVerified
                }
            } catch (e: Exception) {
                _uiState.value = ForgotPasswordUiState.Error(
                    ErrorMapper.toUserMessage(e, "al verificar el código")
                )
            }
        }
    }

    /**
     * CU-03 Paso 3: Actualiza la contraseña del usuario autenticado vía OTP.
     *
     * Antes de llamar a updateUser se verifica que la sesión sigue activa;
     * si no, se reimporta el accessToken guardado en el Paso 2.
     *
     * Fix UX: si Supabase rechaza la nueva contraseña por ser igual a la actual,
     * se muestra un mensaje claro al usuario en lugar del error genérico del backend.
     */
    fun updatePassword(newPassword: String) {
        if (newPassword.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Por favor completa el campo.")
            return
        }
        if (newPassword.length < 6) {
            _uiState.value = ForgotPasswordUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            return
        }
        val token = recoveryAccessToken
        if (token == null) {
            _uiState.value = ForgotPasswordUiState.Error(
                "La sesión de recuperación expiró. Por favor solicita un nuevo código."
            )
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                // Reimportar sesión si fue limpiada entre corrutinas
                if (supabase.auth.currentSessionOrNull() == null) {
                    supabase.auth.importAuthToken(token)
                }
                supabase.auth.updateUser {
                    password = newPassword
                }
                recoveryAccessToken = null
                _uiState.value = ForgotPasswordUiState.PasswordChanged
            } catch (e: Exception) {
                // Mensaje específico y claro cuando el usuario intenta usar la misma contraseña
                val mensaje = if (isSamePasswordError(e)) {
                    "La nueva contraseña debe ser diferente a la contraseña actual."
                } else {
                    ErrorMapper.toUserMessage(e, "al cambiar la contraseña")
                }
                _uiState.value = ForgotPasswordUiState.Error(mensaje)
            }
        }
    }

    fun resetState() {
        _uiState.value = ForgotPasswordUiState.Idle
        pendingEmail = ""
        recoveryAccessToken = null
    }
}
