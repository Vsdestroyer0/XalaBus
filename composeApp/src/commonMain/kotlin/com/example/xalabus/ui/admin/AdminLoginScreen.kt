package com.example.xalabus.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val AdminBg         = Color(0xFF080C14)
private val AdminSurface2   = Color(0xFF161E2E)
private val AdminAccent     = Color(0xFF6366F1)
private val AdminAccentSoft = Color(0xFF818CF8)
private val AdminText       = Color(0xFFE2E8F0)
private val AdminMuted      = Color(0xFF64748B)
private val AdminOutline    = Color(0xFF1E2D45)
private val AdminError      = Color(0xFFEF4444)

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var visible  by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    LaunchedEffect(uiState) {
        if (uiState is AdminUiState.Success) {
            onLoginSuccess()
            viewModel.resetState()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(AdminBg, Color(0xFF0A0F1E), AdminBg))
            )
    ) {
        // Botón volver
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F1624))
        ) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = AdminText)
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 4 }),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Ícono escudo
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(AdminAccent.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AdminPanelSettings,
                        contentDescription = null,
                        tint = AdminAccent,
                        modifier = Modifier.size(34.dp)
                    )
                }

                // Título
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Panel de", color = AdminMuted, fontSize = 13.sp, letterSpacing = 3.sp)
                    Text("Administración", color = AdminText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                    Text("XalaBus", color = AdminAccent, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(4.dp))

                // Campo usuario
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Usuario", color = AdminMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, null, tint = AdminAccentSoft, modifier = Modifier.size(20.dp))
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = AdminAccent,
                        unfocusedBorderColor    = AdminOutline,
                        focusedContainerColor   = AdminSurface2,
                        unfocusedContainerColor = AdminSurface2,
                        focusedTextColor        = AdminText,
                        unfocusedTextColor      = AdminText,
                        cursorColor             = AdminAccent,
                        focusedLabelColor       = AdminAccent,
                        unfocusedLabelColor     = AdminMuted
                    )
                )

                // Campo contraseña
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Contraseña", color = AdminMuted, fontSize = 13.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, null, tint = AdminAccentSoft, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                null, tint = AdminMuted
                            )
                        }
                    },
                    visualTransformation = if (showPass) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = {
                        focusManager.clearFocus()
                        viewModel.signIn(username, password)
                    }),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor      = AdminAccent,
                        unfocusedBorderColor    = AdminOutline,
                        focusedContainerColor   = AdminSurface2,
                        unfocusedContainerColor = AdminSurface2,
                        focusedTextColor        = AdminText,
                        unfocusedTextColor      = AdminText,
                        cursorColor             = AdminAccent,
                        focusedLabelColor       = AdminAccent,
                        unfocusedLabelColor     = AdminMuted
                    )
                )

                // Mensaje de error
                if (uiState is AdminUiState.Error) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ErrorOutline, null, tint = AdminError, modifier = Modifier.size(16.dp))
                        Text((uiState as AdminUiState.Error).message, color = AdminError, fontSize = 13.sp)
                    }
                }

                // Botón ingresar
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signIn(username, password)
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    enabled = uiState !is AdminUiState.Loading,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AdminAccent,
                        contentColor   = Color.White,
                        disabledContainerColor = AdminAccent.copy(alpha = 0.5f)
                    )
                ) {
                    if (uiState is AdminUiState.Loading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Shield, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Ingresar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
