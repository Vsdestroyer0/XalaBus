package com.example.xalabus.ui.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.clip
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

private val XalaBg      = Color(0xFF0A0A0A)
private val XalaSurface = Color(0xFF161616)
private val XalaAccent  = Color(0xFFF5C518)
private val XalaText    = Color(0xFFFFFFFF)
private val XalaMuted   = Color(0xFF8A8A8A)
private val XalaOutline = Color(0xFF2C2C2C)
private val XalaError   = Color(0xFFFF4444)

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onNavigateToAdmin: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email    by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var visible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }
    LaunchedEffect(uiState) {
        if (uiState is AuthUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XalaBg),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 })
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Ícono bus ámbar
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(XalaAccent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.DirectionsBus,
                        contentDescription = null,
                        tint = XalaAccent,
                        modifier = Modifier.size(38.dp)
                    )
                }

                // Título
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Bienvenido a", color = XalaMuted, fontSize = 14.sp, letterSpacing = 2.sp)
                    Text("XalaBus", color = XalaText, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(4.dp))

                // Email
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Correo electrónico", color = XalaMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Email, null, tint = XalaAccent.copy(0.7f), modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
                    keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Down) }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = XalaAccent,
                        unfocusedBorderColor    = XalaOutline,
                        focusedContainerColor   = XalaSurface,
                        unfocusedContainerColor = XalaSurface,
                        focusedTextColor        = XalaText,
                        unfocusedTextColor      = XalaText,
                        cursorColor             = XalaAccent,
                        focusedLabelColor       = XalaAccent
                    )
                )

                // Contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = XalaMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null, tint = XalaAccent.copy(0.7f), modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = XalaMuted
                            )
                        }
                    },
                    visualTransformation = if (showPass) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.signIn(email, password)
                    }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = XalaAccent,
                        unfocusedBorderColor    = XalaOutline,
                        focusedContainerColor   = XalaSurface,
                        unfocusedContainerColor = XalaSurface,
                        focusedTextColor        = XalaText,
                        unfocusedTextColor      = XalaText,
                        cursorColor             = XalaAccent,
                        focusedLabelColor       = XalaAccent
                    )
                )

                // Error
                if (uiState is AuthUiState.Error) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = XalaError, modifier = Modifier.size(16.dp))
                        Text((uiState as AuthUiState.Error).message, color = XalaError, fontSize = 13.sp)
                    }
                }

                // Botón Entrar
                Button(
                    onClick = { focusManager.clearFocus(); viewModel.signIn(email, password) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState !is AuthUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = XalaAccent,
                        contentColor   = Color.Black
                    )
                ) {
                    if (uiState is AuthUiState.Loading) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
                    } else {
                        Text("Entrar", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // Registro
                TextButton(onClick = onNavigateToRegister) {
                    Text("¿No tienes cuenta? ", color = XalaMuted, fontSize = 13.sp)
                    Text("Regístrate", color = XalaAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                // ── Acceso Administrador (discreto, al fondo) ────────────────────
                HorizontalDivider(color = XalaOutline)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onNavigateToAdmin() }
                        .padding(vertical = 10.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = XalaMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("Acceso Administrador", color = XalaMuted, fontSize = 12.sp)
                }
            }
        }
    }
}
