package com.example.xalabus.ui.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Paleta XalaBus Dark (consistente con LoginScreen) ───────────────────────
private val FpBg      = Color(0xFF0A0A0A)
private val FpSurface = Color(0xFF161616)
private val FpAccent  = Color(0xFFF5C518)
private val FpText    = Color(0xFFFFFFFF)
private val FpMuted   = Color(0xFF8A8A8A)
private val FpOutline = Color(0xFF2C2C2C)
private val FpError   = Color(0xFFFF4444)
private val FpSuccess = Color(0xFF4CAF50)

/** Pasos del flujo de recuperación */
private enum class ForgotStep { EMAIL, OTP, NEW_PASSWORD, DONE }

/**
 * Pantalla de recuperación de contraseña (CU-03).
 * Flujo: ingresar correo → verificar código OTP → nueva contraseña → éxito.
 *
 * @param viewModel  ViewModel que gestiona la lógica con Supabase Auth.
 * @param onBack     Navegar de regreso a LoginScreen.
 * @param onSuccess  Navegar a Login después de cambiar la contraseña.
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onBack: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var otpToken by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var currentStep by remember { mutableStateOf(ForgotStep.EMAIL) }

    // Avanzar de paso según el estado del ViewModel
    LaunchedEffect(uiState) {
        when (uiState) {
            is ForgotPasswordUiState.EmailSent   -> currentStep = ForgotStep.OTP
            is ForgotPasswordUiState.OtpVerified -> currentStep = ForgotStep.NEW_PASSWORD
            is ForgotPasswordUiState.PasswordChanged -> {
                currentStep = ForgotStep.DONE
            }
            else -> Unit
        }
    }

    // Navegar a Login cuando el usuario toca "Continuar" en la pantalla de éxito
    if (currentStep == ForgotStep.DONE) {
        LaunchedEffect(Unit) { /* espera interacción del usuario */ }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor      = FpAccent,
        unfocusedBorderColor    = FpOutline,
        focusedLabelColor       = FpAccent,
        unfocusedLabelColor     = FpMuted,
        cursorColor             = FpAccent,
        focusedTextColor        = FpText,
        unfocusedTextColor      = FpText,
        focusedContainerColor   = FpSurface,
        unfocusedContainerColor = FpSurface,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FpBg)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {
            // ── Botón Volver ────────────────────────────────────────────────
            IconButton(
                onClick = {
                    viewModel.resetState()
                    onBack()
                },
                modifier = Modifier
                    .background(FpSurface, shape = RoundedCornerShape(12.dp))
                    .size(44.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = FpText)
            }

            Spacer(Modifier.height(32.dp))

            // ── Ícono del paso actual ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FpAccent, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (currentStep) {
                        ForgotStep.EMAIL        -> Icons.Default.Email
                        ForgotStep.OTP          -> Icons.Default.Pin
                        ForgotStep.NEW_PASSWORD -> Icons.Default.Lock
                        ForgotStep.DONE         -> Icons.Default.CheckCircle
                    },
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Recuperar",
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = FpText
            )
            Text(
                text = "Contraseña",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                color = FpAccent
            )

            Spacer(Modifier.height(8.dp))

            // Indicador de pasos
            StepIndicator(
                totalSteps = 3,
                currentStep = when (currentStep) {
                    ForgotStep.EMAIL        -> 0
                    ForgotStep.OTP          -> 1
                    ForgotStep.NEW_PASSWORD -> 2
                    ForgotStep.DONE         -> 2
                }
            )

            Spacer(Modifier.height(40.dp))

            // ── Contenido animado por paso ─────────────────────────────────
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "forgot_step_transition"
            ) { step ->
                when (step) {

                    // ── PASO 1: Ingresar correo ────────────────────────────
                    ForgotStep.EMAIL -> {
                        Column {
                            Text(
                                text = "Ingresa el correo asociado a tu cuenta y te enviaremos un código de verificación.",
                                color = FpMuted,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Correo electrónico") },
                                leadingIcon = { Icon(Icons.Default.Email, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Email,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    viewModel.sendResetEmail(email)
                                }),
                                isError = uiState is ForgotPasswordUiState.Error
                            )
                            ErrorText(uiState)
                            Spacer(Modifier.height(28.dp))
                            PrimaryButton(
                                text = "Enviar código",
                                isLoading = uiState is ForgotPasswordUiState.Loading,
                                onClick = { viewModel.sendResetEmail(email) }
                            )
                        }
                    }

                    // ── PASO 2: Verificar código OTP ───────────────────────
                    ForgotStep.OTP -> {
                        Column {
                            Text(
                                text = "Revisa tu bandeja de entrada y escribe el código de 6 dígitos que recibiste en $email.",
                                color = FpMuted,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = otpToken,
                                onValueChange = { if (it.length <= 6) otpToken = it },
                                label = { Text("Código de verificación") },
                                leadingIcon = { Icon(Icons.Default.Pin, null) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    viewModel.verifyOtp(email, otpToken)
                                }),
                                isError = uiState is ForgotPasswordUiState.Error
                            )
                            ErrorText(uiState)
                            Spacer(Modifier.height(8.dp))
                            // Reenviar código
                            TextButton(onClick = { viewModel.sendResetEmail(email) }) {
                                Text("Reenviar código", color = FpAccent, fontSize = 13.sp)
                            }
                            Spacer(Modifier.height(16.dp))
                            PrimaryButton(
                                text = "Verificar código",
                                isLoading = uiState is ForgotPasswordUiState.Loading,
                                onClick = { viewModel.verifyOtp(email, otpToken) }
                            )
                        }
                    }

                    // ── PASO 3: Nueva contraseña ───────────────────────────
                    ForgotStep.NEW_PASSWORD -> {
                        Column {
                            Text(
                                text = "Crea una nueva contraseña segura para tu cuenta.",
                                color = FpMuted,
                                fontSize = 14.sp
                            )
                            Spacer(Modifier.height(24.dp))
                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text("Nueva contraseña") },
                                leadingIcon = { Icon(Icons.Default.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(
                                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                            null, tint = FpMuted
                                        )
                                    }
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Next
                                ),
                                isError = uiState is ForgotPasswordUiState.Error
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                label = { Text("Confirmar contraseña") },
                                leadingIcon = { Icon(Icons.Default.LockReset, null) },
                                visualTransformation = PasswordVisualTransformation(),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = fieldColors,
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Password,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(onDone = {
                                    viewModel.updatePassword(newPassword, confirmPassword)
                                }),
                                isError = uiState is ForgotPasswordUiState.Error
                            )
                            ErrorText(uiState)
                            Spacer(Modifier.height(28.dp))
                            PrimaryButton(
                                text = "Cambiar contraseña",
                                isLoading = uiState is ForgotPasswordUiState.Loading,
                                onClick = { viewModel.updatePassword(newPassword, confirmPassword) }
                            )
                        }
                    }

                    // ── PASO 4: Éxito ──────────────────────────────────────
                    ForgotStep.DONE -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = FpSuccess,
                                modifier = Modifier.size(72.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                text = "¡Contraseña cambiada!",
                                color = FpText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = "Ya puedes iniciar sesión con tu nueva contraseña.",
                                color = FpMuted,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(32.dp))
                            PrimaryButton(
                                text = "Ir a Iniciar sesión",
                                isLoading = false,
                                onClick = {
                                    viewModel.resetState()
                                    onSuccess()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Componentes auxiliares reutilizables ────────────────────────────────────

@Composable
private fun PrimaryButton(
    text: String,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(12.dp),
        enabled = !isLoading,
        colors = ButtonDefaults.buttonColors(
            containerColor = FpAccent,
            contentColor = Color.Black
        )
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                color = Color.Black,
                strokeWidth = 2.dp
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun ErrorText(uiState: ForgotPasswordUiState) {
    if (uiState is ForgotPasswordUiState.Error) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = uiState.message,
            color = FpError,
            fontSize = 12.sp
        )
    }
}

/** Indicador visual de progreso de pasos (puntos) */
@Composable
private fun StepIndicator(totalSteps: Int, currentStep: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(totalSteps) { index ->
            Box(
                modifier = Modifier
                    .size(if (index == currentStep) 10.dp else 6.dp)
                    .background(
                        color = if (index <= currentStep) FpAccent else FpOutline,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}
