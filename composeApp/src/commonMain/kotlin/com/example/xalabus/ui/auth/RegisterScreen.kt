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
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── Paleta XalaBus Dark ─────────────────────────────────────────────────────
private val XalaBg      = Color(0xFF0A0A0A)
private val XalaSurface = Color(0xFF161616)
private val XalaAccent  = Color(0xFFF5C518)
private val XalaText    = Color(0xFFFFFFFF)
private val XalaMuted   = Color(0xFF8A8A8A)
private val XalaOutline = Color(0xFF2C2C2C)
private val XalaError   = Color(0xFFFF4444)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var registrationDone by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            registrationDone = true
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor        = XalaAccent,
        unfocusedBorderColor      = XalaOutline,
        focusedLabelColor         = XalaAccent,
        unfocusedLabelColor       = XalaMuted,
        cursorColor               = XalaAccent,
        focusedLeadingIconColor   = XalaAccent,
        unfocusedLeadingIconColor = XalaMuted,
        focusedTrailingIconColor   = XalaAccent,
        unfocusedTrailingIconColor = XalaMuted,
        focusedTextColor          = XalaText,
        unfocusedTextColor        = XalaText,
        focusedContainerColor     = XalaSurface,
        unfocusedContainerColor   = XalaSurface,
        errorBorderColor          = XalaError,
        errorTextColor            = XalaText,
        errorContainerColor       = XalaSurface,
        errorLeadingIconColor     = XalaError,
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XalaBg)
    ) {

        // ── Pantalla de éxito ───────────────────────────────────────────────
        if (registrationDone) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .background(XalaAccent, shape = RoundedCornerShape(24.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = Color.Black
                    )
                }
                Spacer(Modifier.height(24.dp))
                Text(
                    text = "¡Registro exitoso!",
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = XalaText
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Revisa tu correo para confirmar tu cuenta y luego inicia sesión.",
                    color = XalaMuted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(40.dp))
                Button(
                    onClick = {
                        registrationDone = false
                        viewModel.resetState()
                        onNavigateToLogin()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XalaAccent,
                        contentColor   = Color.Black
                    )
                ) {
                    Text("Ir a Iniciar Sesión", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }
            return@Box
        }

        // ── Formulario de registro ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {

            // Botón volver
            IconButton(
                onClick = {
                    viewModel.resetState()
                    onNavigateToLogin()
                },
                modifier = Modifier
                    .background(XalaSurface, shape = RoundedCornerShape(12.dp))
                    .size(44.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Regresar", tint = XalaText)
            }

            Spacer(Modifier.height(28.dp))

            Text(
                text = "Crear cuenta",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 34.sp,
                color = XalaText
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Únete a XalaBus",
                color = XalaMuted,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(36.dp))

            // ── Campo Email ─────────────────────────────────────────────────
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Correo electrónico") },
                leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = fieldColors,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = uiState is AuthUiState.Error
            )

            Spacer(Modifier.height(12.dp))

            // ── Campo Contraseña ────────────────────────────────────────────
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
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
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                isError = uiState is AuthUiState.Error
            )

            Spacer(Modifier.height(12.dp))

            // ── Campo Confirmar Contraseña ───────────────────────────────────
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirmar contraseña") },
                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                        Icon(
                            if (confirmPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null
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
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.signUp(email, password, confirmPassword)
                    }
                ),
                isError = uiState is AuthUiState.Error
            )

            if (uiState is AuthUiState.Error) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = (uiState as AuthUiState.Error).message,
                    color = XalaError,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(32.dp))

            // ── Botón Crear cuenta ──────────────────────────────────────────
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signUp(email, password, confirmPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                enabled = uiState !is AuthUiState.Loading,
                colors = ButtonDefaults.buttonColors(
                    containerColor         = XalaAccent,
                    contentColor           = Color.Black,
                    disabledContainerColor = XalaAccent.copy(alpha = 0.4f),
                    disabledContentColor   = Color.Black.copy(alpha = 0.4f)
                )
            ) {
                if (uiState is AuthUiState.Loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.Black,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Crear cuenta", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Link a Login ─────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿Ya tienes cuenta? ", color = XalaMuted, fontSize = 14.sp)
                TextButton(
                    onClick = {
                        viewModel.resetState()
                        onNavigateToLogin()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Inicia sesión",
                        color = XalaAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
