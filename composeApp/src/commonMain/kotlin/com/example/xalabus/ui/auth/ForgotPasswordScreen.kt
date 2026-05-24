package com.example.xalabus.ui.auth

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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Paleta XalaBus (consistente con LoginScreen) ─────────────────────────────
private val FpBg       = Color(0xFF0A0A0A)
private val FpSurface  = Color(0xFF161616)
private val FpAccent   = Color(0xFFF5C518)
private val FpText     = Color(0xFFFFFFFF)
private val FpMuted    = Color(0xFF8A8A8A)
private val FpOutline  = Color(0xFF2C2C2C)
private val FpError    = Color(0xFFFF4444)
private val FpSuccess  = Color(0xFF4CAF50)

/**
 * Pantalla de recuperación de contraseña (CU-03).
 *
 * Flujo en dos pasos:
 *   Paso 1 — Ingresar correo → enviar OTP
 *   Paso 2 — Ingresar código OTP + nueva contraseña → cambio exitoso
 *
 * @param viewModel  ViewModel que orquesta la lógica de recuperación
 * @param onSuccess  Callback cuando la contraseña fue cambiada exitosamente
 * @param onBack     Callback para regresar a la pantalla de login
 */
@Composable
fun ForgotPasswordScreen(
    viewModel: ForgotPasswordViewModel,
    onSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Campos del Paso 1
    var email by remember { mutableStateOf("") }

    // Campos del Paso 2
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Cuando el estado sea PasswordChanged, notificar al padre
    LaunchedEffect(uiState) {
        if (uiState is ForgotPasswordUiState.PasswordChanged) {
            onSuccess()
            viewModel.resetState()
        }
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
            // ── Botón Volver ──────────────────────────────────────────────────
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

            // ── Ícono ───────────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(FpAccent, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LockReset,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }

            Spacer(Modifier.height(24.dp))

            // ── Título dinámico según el paso actual ────────────────────────
            val isCodeSent = uiState is ForgotPasswordUiState.CodeSent

            Text(
                text = if (isCodeSent) "Verifica tu correo" else "Recuperar",
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = FpText
            )
            Text(
                text = if (isCodeSent) "Ingresa el código" else "Contraseña",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                color = FpAccent
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = if (isCodeSent)
                    "Revisa la bandeja de ${(uiState as ForgotPasswordUiState.CodeSent).email}"
                else
                    "Te enviaremos un código de recuperación",
                color = FpMuted,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(40.dp))

            // ───────────────────────────────────────────────────────────────
            // PASO 1: campo de correo
            // ───────────────────────────────────────────────────────────────
            if (!isCodeSent) {
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = FpMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.sendPasswordResetEmail(email)
                    }),
                    isError = uiState is ForgotPasswordUiState.Error
                )

                // Mensaje de error FA-01 / Ex-01
                if (uiState is ForgotPasswordUiState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (uiState as ForgotPasswordUiState.Error).message,
                        color = FpError,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.sendPasswordResetEmail(email)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is ForgotPasswordUiState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FpAccent,
                        contentColor = Color.Black
                    )
                ) {
                    if (uiState is ForgotPasswordUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Enviar código", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }
            }

            // ───────────────────────────────────────────────────────────────
            // PASO 2: código OTP + nueva contraseña
            // ───────────────────────────────────────────────────────────────
            if (isCodeSent) {
                val sentEmail = (uiState as ForgotPasswordUiState.CodeSent).email

                // Campo OTP
                OutlinedTextField(
                    value = otp,
                    onValueChange = { otp = it.filter { c -> c.isDigit() } },
                    label = { Text("Código de verificación") },
                    leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null, tint = FpMuted) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = uiState is ForgotPasswordUiState.Error
                )

                Spacer(Modifier.height(12.dp))

                // Nueva contraseña
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("Nueva contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = FpMuted) },
                    trailingIcon = {
                        IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                            Icon(
                                imageVector = if (newPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = FpMuted
                            )
                        }
                    },
                    visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    isError = uiState is ForgotPasswordUiState.Error
                )

                Spacer(Modifier.height(12.dp))

                // Confirmar contraseña
                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text("Confirmar contraseña") },
                    leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = null, tint = FpMuted) },
                    trailingIcon = {
                        IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                            Icon(
                                imageVector = if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = FpMuted
                            )
                        }
                    },
                    visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.verifyOtpAndChangePassword(sentEmail, otp, newPassword, confirmPassword)
                    }),
                    isError = uiState is ForgotPasswordUiState.Error
                )

                // Mensaje de error FA-02 / Ex-01
                if (uiState is ForgotPasswordUiState.Error) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = (uiState as ForgotPasswordUiState.Error).message,
                        color = FpError,
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.verifyOtpAndChangePassword(sentEmail, otp, newPassword, confirmPassword)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    enabled = uiState !is ForgotPasswordUiState.Loading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FpAccent,
                        contentColor = Color.Black
                    )
                ) {
                    if (uiState is ForgotPasswordUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.Black,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cambiar contraseña", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Opción de reenviar código
                TextButton(
                    onClick = { viewModel.sendPasswordResetEmail(sentEmail) },
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reenviar código", color = FpMuted, fontSize = 13.sp)
                }
            }
        }
    }
}
