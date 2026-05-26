package com.example.xalabus.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

/**
 * CU-03 — Pantalla de recuperación de contraseña.
 *
 * Flujo en 3 pasos:
 *  1. Ingresar correo → envío del código OTP
 *  2. Ingresar código OTP (longitud configurable por Supabase, actualmente 8 dígitos)
 *  3. Ingresar y confirmar nueva contraseña
 *
 * FA-01: correo no encontrado → mensaje de error en UI
 * FA-02: código inválido       → mensaje de error en UI
 * Ex-01: error de red          → mensaje de error en UI
 */

// Longitud del OTP enviado por Supabase. Si se cambia en el Dashboard, actualizar aquí.
private const val OTP_LENGTH = 8

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // Campos controlados localmente para no exponer estado mutable en el ViewModel
    var email       by remember { mutableStateOf("") }
    var otp         by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }

    // Navegar al login cuando el proceso termine exitosamente
    LaunchedEffect(uiState) {
        if (uiState is ForgotPasswordUiState.PasswordChanged) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() }
            ) { state ->
                when (state) {
                    // ── Paso 1: Ingreso de correo ─────────────────────────────
                    is ForgotPasswordUiState.Idle,
                    is ForgotPasswordUiState.Loading,
                    is ForgotPasswordUiState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Ingresa tu correo",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Te enviaremos un código de verificación.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value         = email,
                                onValueChange = { email = it },
                                label         = { Text("Correo electrónico") },
                                leadingIcon   = { Icon(Icons.Default.Email, null) },
                                modifier      = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction    = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.sendRecoveryEmail(email) }
                                ),
                                singleLine = true,
                                isError    = state is ForgotPasswordUiState.Error
                            )

                            // Mensaje de error FA-01 / Ex-01
                            if (state is ForgotPasswordUiState.Error) {
                                Text(
                                    state.message,
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick  = { viewModel.sendRecoveryEmail(email) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled  = state !is ForgotPasswordUiState.Loading
                            ) {
                                if (state is ForgotPasswordUiState.Loading) {
                                    CircularProgressIndicator(
                                        modifier   = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color      = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Text("Enviar código")
                                }
                            }
                        }
                    }

                    // ── Paso 2: Ingresar código OTP ───────────────────────────
                    is ForgotPasswordUiState.EmailSent -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Código enviado",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                // Texto actualizado para reflejar los 8 dígitos reales
                                "Revisa tu correo e ingresa el código de $OTP_LENGTH dígitos.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value         = otp,
                                // fix: maxLength ajustado a OTP_LENGTH (8) en lugar de 6
                                onValueChange = { if (it.length <= OTP_LENGTH) otp = it },
                                label         = { Text("Código de verificación ($OTP_LENGTH dígitos)") },
                                modifier      = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction    = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = { viewModel.verifyOtp(otp) }
                                ),
                                singleLine = true
                            )

                            Button(
                                onClick  = { viewModel.verifyOtp(otp) },
                                modifier = Modifier.fillMaxWidth(),
                                // Habilitar solo cuando se hayan ingresado todos los dígitos
                                enabled  = otp.length == OTP_LENGTH
                            ) { Text("Verificar código") }

                            TextButton(onClick = { viewModel.sendRecoveryEmail(email) }) {
                                Text("Reenviar código")
                            }
                        }
                    }

                    // ── Paso 3: Nueva contraseña ──────────────────────────────
                    is ForgotPasswordUiState.OtpVerified -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Nueva contraseña",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold
                            )

                            OutlinedTextField(
                                value               = newPassword,
                                onValueChange       = { newPassword = it },
                                label               = { Text("Nueva contraseña") },
                                modifier            = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions     = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction    = ImeAction.Next
                                ),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value               = confirmPass,
                                onValueChange       = { confirmPass = it },
                                label               = { Text("Confirmar contraseña") },
                                modifier            = Modifier.fillMaxWidth(),
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions     = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction    = ImeAction.Done
                                ),
                                isError = confirmPass.isNotEmpty() && newPassword != confirmPass,
                                singleLine = true
                            )

                            if (confirmPass.isNotEmpty() && newPassword != confirmPass) {
                                Text(
                                    "Las contraseñas no coinciden.",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }

                            Button(
                                onClick  = { viewModel.updatePassword(newPassword) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled  = newPassword.isNotBlank() && newPassword == confirmPass
                            ) { Text("Cambiar contraseña") }
                        }
                    }

                    // Estado final — la navegación se maneja con LaunchedEffect arriba
                    is ForgotPasswordUiState.PasswordChanged -> {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
