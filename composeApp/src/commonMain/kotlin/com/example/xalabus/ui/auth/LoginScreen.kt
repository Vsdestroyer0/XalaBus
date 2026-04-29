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
private val XalaAccent  = Color(0xFFF5C518)   // amarillo autobús
private val XalaText    = Color(0xFFFFFFFF)
private val XalaMuted   = Color(0xFF8A8A8A)
private val XalaOutline = Color(0xFF2C2C2C)
private val XalaError   = Color(0xFFFF4444)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {

            // ── Ícono de marca ──────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(XalaAccent, shape = RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(32.dp),
                    tint = Color.Black
                )
            }

            Spacer(Modifier.height(24.dp))

            Text(
                text = "Bienvenido a",
                fontWeight = FontWeight.Normal,
                fontSize = 28.sp,
                color = XalaText
            )
            Text(
                text = "XalaBus",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 38.sp,
                color = XalaAccent
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "Tu guía de transporte en Xalapa",
                color = XalaMuted,
                fontSize = 14.sp
            )

            Spacer(Modifier.height(48.dp))

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
                            imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = if (passwordVisible) "Ocultar" else "Mostrar"
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
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        viewModel.signIn(email, password)
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

            // ── Botón Entrar ────────────────────────────────────────────────
            Button(
                onClick = {
                    focusManager.clearFocus()
                    viewModel.signIn(email, password)
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
                    Text("Entrar", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── Link a Registro ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("¿No tienes cuenta? ", color = XalaMuted, fontSize = 14.sp)
                TextButton(
                    onClick = {
                        viewModel.resetState()
                        onNavigateToRegister()
                    },
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text(
                        "Regístrate",
                        color = XalaAccent,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
