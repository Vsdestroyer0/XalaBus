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
 * CU-23: Diálogo para calificar una ruta con 1–5 estrellas y comentario opcional.
 * Solo accesible para usuarios autenticados (guard en App.kt).
 * Cubre: C23-1 (flujo normal), C23-2 (validación), C23-3 (error red),
 *        C23-4 (sin sesión), C23-5 (comentario con límite de caracteres).
 */
@Composable
fun RatingDialog(
    routeName: String,
    routeId: String,
    viewModel: RatingViewModel,
    onDismiss: () -> Unit
) {
    val uiState      by viewModel.ratingState.collectAsState()
    val previousScore by viewModel.currentUserScore.collectAsState()

    var selectedScore by remember(previousScore) { mutableStateOf(previousScore ?: 0) }
    var comment       by remember { mutableStateOf("") }
    val maxCommentLen = 300

    LaunchedEffect(routeId) {
        viewModel.loadUserRating(routeId)
    }

    // Auto-cierre tras éxito con pequeño delay para que el usuario vea la confirmación
    LaunchedEffect(uiState) {
        if (uiState is RatingUiState.Success) {
            delay(1200)
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
                Text(
                    routeName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // ── Selector de estrellas (1–5) ──────────────────────────
                Text(
                    "Selecciona tu puntuación:",
                    style = MaterialTheme.typography.labelMedium
                )
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier              = Modifier.fillMaxWidth()
                ) {
                    for (star in 1..5) {
                        IconButton(
                            onClick  = { selectedScore = star },
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = if (star <= selectedScore)
                                    Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "$star estrellas",
                                tint     = if (star <= selectedScore)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                }

                // Texto descriptivo de la puntuación
                if (selectedScore > 0) {
                    Text(
                        text = when (selectedScore) {
                            1 -> "Muy mala"
                            2 -> "Mala"
                            3 -> "Regular"
                            4 -> "Buena"
                            5 -> "Excelente"
                            else -> ""
                        },
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // ── Comentario opcional (C23-5) ───────────────────────────
                OutlinedTextField(
                    value         = comment,
                    onValueChange = { if (it.length <= maxCommentLen) comment = it },
                    label         = { Text("Comentario (opcional)") },
                    placeholder   = { Text("Comparte tu experiencia...") },
                    maxLines      = 4,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences
                    ),
                    supportingText = {
                        Text(
                            "${comment.length}/$maxCommentLen",
                            modifier = Modifier.fillMaxWidth(),
                            style    = MaterialTheme.typography.labelSmall
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // ── Estado: cargando ──────────────────────────────────────
                if (uiState is RatingUiState.Loading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }

                // ── Estado: error (C23-2, C23-3) ─────────────────────────
                AnimatedVisibility(uiState is RatingUiState.Error) {
                    val msg = (uiState as? RatingUiState.Error)?.message.orEmpty()
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Text(
                            msg,
                            modifier   = Modifier.padding(12.dp),
                            color      = MaterialTheme.colorScheme.onErrorContainer,
                            style      = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // ── Estado: éxito (C23-1) ─────────────────────────────────
                AnimatedVisibility(uiState is RatingUiState.Success) {
                    Text(
                        "✓ ¡Calificación guardada!",
                        color      = MaterialTheme.colorScheme.primary,
                        style      = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    viewModel.submitRating(
                        routeId = routeId,
                        score   = selectedScore,
                        comment = comment
                    )
                },
                enabled = selectedScore in 1..5 && uiState !is RatingUiState.Loading
            ) {
                Text("Enviar calificación")
            }
        },
        dismissButton = {
            TextButton(onClick = {
                viewModel.resetRatingState()
                onDismiss()
            }) {
                Text("Cancelar")
            }
        }
    )
}
