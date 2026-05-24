package com.example.xalabus.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

/**
 * CU-03: Pantalla de recuperación de contraseña en tres pasos.
 *
 * Paso 1 (ENTER_EMAIL)    → el usuario ingresa su correo electrónico
 * Paso 2 (ENTER_CODE)     → el usuario ingresa el código OTP de 6 dígitos
 * Paso 3 (ENTER_PASSWORD) → el usuario ingresa y confirma su nueva contraseña
 *
 * @param onBack         Navega hacia atrás (a LoginScreen)
 * @param onPasswordChanged Navega al login tras cambio exitoso de contraseña
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onBack: () -> Unit,
    onPasswordChanged: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    // Estado local de los campos de texto
    var email         by remember { mutableStateOf("") }
    var otpCode       by remember { mutableStateOf("") }
    var newPassword   by remember { mutableStateOf("") }
    var confirmPass   by remember { mutableStateOf("") }

    // Paso actual del flujo (controlado por el ViewModel a través del estado)
    var currentStep by remember { mutableStateOf(ForgotPasswordStep.ENTER_EMAIL) }

    // Procesamiento de cambios de estado del ViewModel
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is ForgotPasswordUiState.StepCompleted -> currentStep = state.nextStep
            is ForgotPasswordUiState.PasswordChanged -> onPasswordChanged()
            else -> Unit
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Recuperar contraseña") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // Indicador de progreso del flujo (3 pasos)
            StepIndicator(currentStep = currentStep)

            Spacer(modifier = Modifier.height(32.dp))

            when (currentStep) {
                // ----- PASO 1: Ingresar correo -----
                ForgotPasswordStep.ENTER_EMAIL -> {
                    Icon(
                        imageVector = Icons.Default.MailOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Ingresa tu correo registrado",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Te enviaremos un código de verificación.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value         = email,
                        onValueChange = { email = it },
                        label         = { Text("Correo electrónico") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick  = { viewModel.sendResetEmail(email) },
                        enabled  = uiState !is ForgotPasswordUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is ForgotPasswordUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Enviar código")
                        }
                    }
                }

                // ----- PASO 2: Ingresar código OTP -----
                ForgotPasswordStep.ENTER_CODE -> {
                    Text(
                        text = "Revisa tu correo",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Ingresa el código de 6 dígitos que enviamos a tu correo.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value         = otpCode,
                        onValueChange = { if (it.length <= 6) otpCode = it },
                        label         = { Text("Código OTP") },
                        singleLine    = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.NumberPassword,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick  = { viewModel.verifyOtp(otpCode) },
                        enabled  = uiState !is ForgotPasswordUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is ForgotPasswordUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Verificar código")
                        }
                    }
                    TextButton(onClick = { viewModel.sendResetEmail(email) }) {
                        Text("Reenviar código")
                    }
                }

                // ----- PASO 3: Nueva contraseña -----
                ForgotPasswordStep.ENTER_PASSWORD -> {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Nueva contraseña",
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedTextField(
                        value         = newPassword,
                        onValueChange = { newPassword = it },
                        label         = { Text("Nueva contraseña") },
                        singleLine    = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = confirmPass,
                        onValueChange = { confirmPass = it },
                        label         = { Text("Confirmar contraseña") },
                        singleLine    = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction    = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { focusManager.clearFocus() }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick  = { viewModel.updatePassword(newPassword, confirmPass) },
                        enabled  = uiState !is ForgotPasswordUiState.Loading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (uiState is ForgotPasswordUiState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Cambiar contraseña")
                        }
                    }
                }
            }

            // Mensaje de error (FA-01, FA-02, Ex-01)
            if (uiState is ForgotPasswordUiState.Error) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = (uiState as ForgotPasswordUiState.Error).message,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Indicador visual de los 3 pasos del flujo de recuperación */
@Composable
private fun StepIndicator(currentStep: ForgotPasswordStep) {
    val steps = ForgotPasswordStep.entries
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, step ->
            val isActive   = step == currentStep
            val isComplete = step.ordinal < currentStep.ordinal
            Surface(
                shape = MaterialTheme.shapes.small,
                color = when {
                    isActive || isComplete -> MaterialTheme.colorScheme.primary
                    else                  -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(width = if (isActive) 24.dp else 16.dp, height = 8.dp)
            ) {}
        }
    }
}
