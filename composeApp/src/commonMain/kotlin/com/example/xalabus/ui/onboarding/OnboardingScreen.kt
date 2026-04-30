package com.example.xalabus.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ---------------------------------------------------------------------------
// Modelo de cada paso del walkthrough
// ---------------------------------------------------------------------------
data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val gradientStart: Color,
    val gradientEnd: Color
)

val onboardingSteps = listOf(
    OnboardingStep(
        icon = Icons.Default.DirectionsBus,
        title = "Bienvenido a XalaBus",
        description = "Tu guía de camiones urbanos en Xalapa. Consulta rutas, trayectos y tarifas de forma rápida y sencilla.",
        gradientStart = Color(0xFF1565C0),
        gradientEnd = Color(0xFF42A5F5)
    ),
    OnboardingStep(
        icon = Icons.Default.Search,
        title = "Busca tu ruta",
        description = "Usa la barra de búsqueda para encontrar rutas por nombre o colonia.\n\nEjemplo: \"Avila Camacho\" o \"Centro\".",
        gradientStart = Color(0xFF00695C),
        gradientEnd = Color(0xFF26A69A)
    ),
    OnboardingStep(
        icon = Icons.Default.Map,
        title = "Explora el mapa",
        description = "Selecciona cualquier ruta de la lista para ver su trayecto completo sobre el mapa de Xalapa.",
        gradientStart = Color(0xFF6A1B9A),
        gradientEnd = Color(0xFFAB47BC)
    ),
    OnboardingStep(
        icon = Icons.Default.Help,
        title = "¿Tienes dudas?",
        description = "Toca el botón ❓ en la pantalla principal para consultar las preguntas frecuentes sobre la app.",
        gradientStart = Color(0xFFE65100),
        gradientEnd = Color(0xFFFFA726)
    )
)

// ---------------------------------------------------------------------------
// Pantalla principal del walkthrough
// ---------------------------------------------------------------------------
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSteps.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingSteps.lastIndex

    val currentStep = onboardingSteps[pagerState.currentPage]

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(currentStep.gradientStart, currentStep.gradientEnd)
                )
            )
    ) {
        // Botón "Saltar" — esquina superior derecha (oculto en última página)
        AnimatedVisibility(
            visible = !isLastPage,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 20.dp),
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            TextButton(onClick = onFinish) {
                Text(
                    text = "Saltar",
                    color = Color.White.copy(alpha = 0.8f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            }
        }

        // Contenido central — pager
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(0.08f))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(step = onboardingSteps[page])
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Indicadores de punto
            PageIndicatorRow(
                pageCount = onboardingSteps.size,
                currentPage = pagerState.currentPage
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Botón principal
            Button(
                onClick = {
                    if (isLastPage) {
                        onFinish()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = currentStep.gradientStart
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
            ) {
                Text(
                    text = if (isLastPage) "¡Comenzar!" else "Siguiente",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (!isLastPage) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Página individual del walkthrough
// ---------------------------------------------------------------------------
@Composable
fun OnboardingPage(step: OnboardingStep) {
    var visible by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.6f,
        animationSpec = tween(durationMillis = 400),
        label = "iconScale"
    )

    LaunchedEffect(step) {
        visible = false
        visible = true
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícono en círculo blanco semitransparente
        Box(
            modifier = Modifier
                .size(140.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = step.icon,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 3 }
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = step.title,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                Text(
                    text = step.description,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 26.sp
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Indicadores de página (puntos)
// ---------------------------------------------------------------------------
@Composable
fun PageIndicatorRow(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val isSelected = index == currentPage
            val width by animateFloatAsState(
                targetValue = if (isSelected) 28f else 8f,
                animationSpec = tween(300),
                label = "indicatorWidth"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(width.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) Color.White
                        else Color.White.copy(alpha = 0.4f)
                    )
            )
        }
    }
}
