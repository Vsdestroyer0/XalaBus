package com.example.xalabus.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xalabus.core.util.ErrorMapper
import com.example.xalabus.data.SupabaseClientProvider
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.user.UserInfo
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
     * Fix CU-03: Guardamos el accessToken que Supabase devuelve al verificar el OTP.
     * Es necesario porque verifyEmailOtp establece una sesión temporal y updateUser
     * la necesita activa para autorizarse. Si el SessionManager de KMP no la persiste
     * entre corrutinas, este token es nuestra fuente de verdad.
     */
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
     *
     * FIX: Después de verifyEmailOtp, leemos la sesión activa del cliente
     * y guardamos el accessToken en memoria para usarlo en updatePassword.
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
                // Guardar el accessToken de la sesión recién establecida
                recoveryAccessToken = supabase.auth.currentSessionOrNull()?.accessToken
                if (recoveryAccessToken == null) {
                    // La sesión no se estableció correctamente
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
     * FIX: Usamos updateUser que opera sobre la sesión activa en el cliente.
     * Si por alguna razón la sesión fue limpiada entre corrutinas (bug conocido
     * del KMP client con SessionManager en memoria), reimportamos el token
     * antes de llamar a updateUser para garantizar el contexto autenticado.
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
            // Sesión expirada o se perdió el token — pedir al usuario que repita el flujo
            _uiState.value = ForgotPasswordUiState.Error(
                "La sesión de recuperación expiró. Por favor solicita un nuevo código."
            )
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                // Reimportar la sesión activa si fue limpiada entre corrutinas
                val session = supabase.auth.currentSessionOrNull()
                if (session == null) {
                    supabase.auth.importAuthToken(token)
                }
                supabase.auth.updateUser {
                    password = newPassword
                }
                recoveryAccessToken = null
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
        recoveryAccessToken = null
    }
}
