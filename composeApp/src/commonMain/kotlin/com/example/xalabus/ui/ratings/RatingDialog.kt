package com.example.xalabus.ui.ratings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.example.xalabus.ui.viewmodel.RatingUiState
import com.example.xalabus.ui.viewmodel.RatingViewModel
import kotlinx.coroutines.delay

/**
 * CU-23: Diálogo para calificar una ruta (1-5 estrellas + comentario opcional).
 * Cubre: C23-1 (flujo normal), C23-2 (validación rango), C23-3 (error red),
 *        C23-4 (sin sesión manejado en ViewModel), C23-5 (comentario con límite).
 *
 * @param routeName   Nombre de la ruta a calificar (también se guarda en la tabla).
 * @param routeId     ID de la ruta.
 * @param userId      UID del usuario autenticado; null si no hay sesión.
 * @param viewModel   RatingViewModel compartido.
 * @param onDismiss   Callback al cerrar el diálogo.
 */
@Composable
fun RatingDialog(
    routeName: String,
    routeId: String,
    userId: String?,
    viewModel: RatingViewModel,
    onDismiss: () -> Unit
) {
    val uiState       by viewModel.ratingState.collectAsState()
    val previousScore by viewModel.currentUserScore.collectAsState()

    var selectedScore by remember(previousScore) { mutableStateOf(previousScore ?: 0) }
    var comment       by remember { mutableStateOf("") }
    val maxCommentLen = 300

    // Carga puntuación previa si el usuario ya calificó esta ruta
    LaunchedEffect(Unit) {
        if (userId != null) viewModel.loadUserRating(routeId, userId)
    }

    // C23-1: cierre automático 1.2 s tras éxito
    LaunchedEffect(uiState) {
        if (uiState is RatingUiState.Success) {
            delay(1_200)
            viewModel.resetRatingState()
            onDismiss()
        }
    }

    AlertDialog(
        onDismissRequest = {
            viewModel.resetRatingState()
            onDismiss()
        },
        title = {
            Text(
                "Calificar ruta",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier            = Modifier.fillMaxWidth()
            ) {
                // Nombre de la ruta
                Text(
                    routeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Selector de estrellas 1–5 ──────────────────────────────
                Text("Selecciona una puntuación:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (star in 1..5) {
                        IconButton(onClick = { selectedScore = star }) {
                            Icon(
                                imageVector = if (star <= selectedScore)
                                    Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Estrella $star",
                                tint     = if (star <= selectedScore)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }

                // ── Comentario opcional (C23-5: máx 300 caracteres) ────────
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { if (it.length <= maxCommentLen) comment = it },
                    label         = { Text("Comentario (opcional)") },
                    placeholder   = { Text("Comparte tu experiencia...") },
                    maxLines      = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    supportingText = { Text("${comment.length}/$maxCommentLen") },
                    modifier       = Modifier.fillMaxWidth()
                )

                // ── Mensaje de error (C23-2, C23-3, C23-4) ─────────────────
                AnimatedVisibility(uiState is RatingUiState.Error) {
                    val msg = (uiState as? RatingUiState.Error)?.message.orEmpty()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            msg,
                            modifier = Modifier.padding(12.dp),
                            color    = MaterialTheme.colorScheme.onErrorContainer,
                            style    = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // ── Confirmación de éxito (C23-1) ───────────────────────────
                AnimatedVisibility(uiState is RatingUiState.Success) {
                    Text(
                        "\u2713 ¡Calificación guardada!",
                        color      = MaterialTheme.colorScheme.primary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Indicador de carga ──────────────────────────────────────
                if (uiState is RatingUiState.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitRating(
                        routeId   = routeId,
                        routeName = routeName,   // se guarda en route_name
                        userId    = userId,
                        score     = selectedScore,
                        comment   = comment
                    )
                },
                enabled = selectedScore in 1..5 && uiState !is RatingUiState.Loading
            ) { Text("Enviar") }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.resetRatingState()
                onDismiss()
            }) { Text("Cancelar") }
        }
    )
}
