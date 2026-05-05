package com.example.xalabus.ui.admin

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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

// ─── Paleta exclusiva del panel admin ────────────────────────────────────────
private val AdminBg         = Color(0xFF080C14)   // navy profundo
private val AdminSurface    = Color(0xFF0F1624)   // superficie oscura
private val AdminSurface2   = Color(0xFF161E2E)   // campos
private val AdminAccent     = Color(0xFF6366F1)   // índigo eléctrico
private val AdminAccentSoft = Color(0xFF818CF8)   // índigo suave (hover / placeholder)
private val AdminText       = Color(0xFFE2E8F0)   // blanco frío
private val AdminMuted      = Color(0xFF64748B)   // slate gris
private val AdminOutline    = Color(0xFF1E2D45)   // borde sutil
private val AdminError      = Color(0xFFEF4444)   // rojo error

@Composable
fun AdminLoginScreen(
    viewModel: AdminViewModel,
    onLoginSuccess: () -> Unit,
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email     by remember { mutableStateOf("") }
    var password  by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    var showPass  by remember { mutableStateOf(false) }
    var showCode  by remember { mutableStateOf(false) }
    var visible   by remember { mutableStateOf(false) }

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
                Brush.verticalGradient(
                    listOf(AdminBg, Color(0xFF0A0F1E), AdminBg)
                )
            )
    ) {
        // Botón volver
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .padding(16.dp)
                .align(Alignment.TopStart)
                .clip(RoundedCornerShape(12.dp))
                .background(AdminSurface)
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
                        .size(80.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.radialGradient(
                                listOf(AdminAccent.copy(alpha = 0.25f), Color.Transparent)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(AdminAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AdminPanelSettings,
                            contentDescription = null,
                            tint = AdminAccent,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Título
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Panel de",
                        color = AdminMuted,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 3.sp
                    )
                    Text(
                        "Administración",
                        color = AdminText,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        "XalaBus",
                        color = AdminAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(Modifier.height(4.dp))

                // Campo Email
                AdminTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = "Correo electrónico",
                    leadingIcon = Icons.Default.Email,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Campo Contraseña
                AdminTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = "Contraseña",
                    leadingIcon = Icons.Default.Lock,
                    visualTransformation = if (showPass) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showPass = !showPass }) {
                            Icon(
                                if (showPass) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AdminMuted
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    )
                )

                // Campo Código Admin
                AdminTextField(
                    value = adminCode,
                    onValueChange = { adminCode = it },
                    label = "Código de administrador",
                    leadingIcon = Icons.Default.Key,
                    visualTransformation = if (showCode) VisualTransformation.None
                                          else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { showCode = !showCode }) {
                            Icon(
                                if (showCode) Icons.Default.VisibilityOff
                                else Icons.Default.Visibility,
                                contentDescription = null,
                                tint = AdminMuted
                            )
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signInAsAdmin(email, password, adminCode)
                        }
                    )
                )

                // Error
                if (uiState is AdminUiState.Error) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = AdminError,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            (uiState as AdminUiState.Error).message,
                            color = AdminError,
                            fontSize = 13.sp
                        )
                    }
                }

                // Botón ingresar
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signInAsAdmin(email, password, adminCode)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
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
                        Text("Ingresar al panel", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

// ─── Campo de texto reutilizable con estilo admin ─────────────────────────────
@Composable
private fun AdminTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = AdminMuted, fontSize = 13.sp) },
        leadingIcon = { Icon(leadingIcon, null, tint = AdminAccentSoft, modifier = Modifier.size(20.dp)) },
        trailingIcon = trailingIcon,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
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
            unfocusedLabelColor     = AdminMuted,
        )
    )
}
