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

/** Pasos del flujo de recuperación de contraseña (CU-03) */
enum class ForgotPasswordStep {
    ENTER_EMAIL,      // Paso 1: usuario ingresa su correo
    ENTER_CODE,       // Paso 2: usuario ingresa el código OTP recibido
    ENTER_PASSWORD    // Paso 3: usuario ingresa la nueva contraseña
}

/** Estados de UI para la pantalla de recuperación de contraseña */
sealed class ForgotPasswordUiState {
    object Idle    : ForgotPasswordUiState()
    object Loading : ForgotPasswordUiState()
    /** Indica éxito y a qué paso avanzar */
    data class StepCompleted(val nextStep: ForgotPasswordStep) : ForgotPasswordUiState()
    /** Contraseña cambiada con éxito: el usuario puede hacer login */
    object PasswordChanged : ForgotPasswordUiState()
    /** FA-01: correo no encontrado; FA-02: código inválido; Ex-01: error general */
    data class Error(val message: String) : ForgotPasswordUiState()
}

/**
 * CU-03: ViewModel para el flujo de recuperación de contraseña por correo electrónico.
 *
 * Flujo:
 *   [sendResetEmail] → Supabase envía OTP al correo
 *   [verifyOtp]      → valida el código de 6 dígitos
 *   [updatePassword] → establece la nueva contraseña
 */
class ForgotPasswordViewModel : ViewModel() {

    private val supabase = SupabaseClientProvider.client

    private val _uiState = MutableStateFlow<ForgotPasswordUiState>(ForgotPasswordUiState.Idle)
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()

    /** Correo guardado para reutilizarlo en la verificación OTP */
    private var pendingEmail: String = ""

    /**
     * Paso 1: Envía correo de recuperación.
     * FA-01: correo no encontrado en Supabase → Error con mensaje amigable.
     * Ex-01: error de red u otro → Error genérico.
     */
    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = ForgotPasswordUiState.Error("Ingresa un correo electrónico válido.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        pendingEmail = email.trim()
        viewModelScope.launch {
            try {
                supabase.auth.resetPasswordForEmail(pendingEmail)
                _uiState.value = ForgotPasswordUiState.StepCompleted(
                    nextStep = ForgotPasswordStep.ENTER_CODE
                )
            } catch (e: Exception) {
                val msg = when {
                    // FA-01: correo no registrado
                    e.message?.contains("User not found", ignoreCase = true) == true ->
                        "No encontramos una cuenta con ese correo."
                    // Ex-01: error general
                    else -> "Error al enviar el código: ${e.message}"
                }
                _uiState.value = ForgotPasswordUiState.Error(msg)
            }
        }
    }

    /**
     * Paso 2: Verifica el código OTP de 6 dígitos enviado al correo.
     * FA-02: código inválido o expirado → Error con mensaje descriptivo.
     * Ex-01: error general.
     */
    fun verifyOtp(token: String) {
        if (token.isBlank() || token.length < 6) {
            _uiState.value = ForgotPasswordUiState.Error("El código debe tener 6 dígitos.")
            return
        }
        _uiState.value = ForgotPasswordUiState.Loading
        viewModelScope.launch {
            try {
                supabase.auth.verifyEmailOtp(
                    type  = io.github.jan.supabase.auth.providers.builtin.OtpType.Email.RECOVERY,
                    email = pendingEmail,
                    token = token.trim()
                )
                _uiState.value = ForgotPasswordUiState.StepCompleted(
                    nextStep = ForgotPasswordStep.ENTER_PASSWORD
                )
            } catch (e: Exception) {
                val msg = when {
                    // FA-02: token inválido
                    e.message?.contains("invalid", ignoreCase = true) == true ||
                    e.message?.contains("expired", ignoreCase = true) == true ->
                        "Código incorrecto o expirado. Solicita uno nuevo."
                    // Ex-01
                    else -> "Error al verificar el código: ${e.message}"
                }
                _uiState.value = ForgotPasswordUiState.Error(msg)
            }
        }
    }

    /**
     * Paso 3: Actualiza la contraseña del usuario (requiere sesión activa tras OTP).
     * Ex-01: error al actualizar.
     */
    fun updatePassword(newPassword: String, confirmPassword: String) {
        when {
            newPassword.isBlank() ->
                _uiState.value = ForgotPasswordUiState.Error("La contraseña no puede estar vacía.")
            newPassword.length < 6 ->
                _uiState.value = ForgotPasswordUiState.Error("La contraseña debe tener al menos 6 caracteres.")
            newPassword != confirmPassword ->
                _uiState.value = ForgotPasswordUiState.Error("Las contraseñas no coinciden.")
            else -> {
                _uiState.value = ForgotPasswordUiState.Loading
                viewModelScope.launch {
                    try {
                        supabase.auth.updateUser { password = newPassword }
                        _uiState.value = ForgotPasswordUiState.PasswordChanged
                    } catch (e: Exception) {
                        _uiState.value = ForgotPasswordUiState.Error(
                            "Error al cambiar la contraseña: ${e.message}"
                        )
                    }
                }
            }
        }
    }

    fun resetState() { _uiState.value = ForgotPasswordUiState.Idle }
}
