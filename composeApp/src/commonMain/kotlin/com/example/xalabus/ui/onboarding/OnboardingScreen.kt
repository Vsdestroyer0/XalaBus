package com.example.xalabus.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

// ── Colores Premium XalaBus ──────────────────────────────────────────────────
private val XalaAmber = Color(0xFFF5C518)
private val XalaDark  = Color(0xFF0A0A0A)
private val XalaCard  = Color(0xFF1E1E1E)

// ── Modelo de Pasos ──────────────────────────────────────────────────────────
data class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val highlightLabel: String
)

val onboardingSteps = listOf(
    OnboardingStep(
        icon = Icons.Default.DirectionsBus,
        title = "Bienvenido a XalaBus",
        description = "La forma más inteligente de moverte por Xalapa. Todo el transporte en la palma de tu mano.",
        highlightLabel = "TU GUÍA"
    ),
    OnboardingStep(
        icon = Icons.Default.Search,
        title = "Encuentra tu ruta",
        description = "Escribe el nombre de tu colonia o una calle y te diremos qué camión te lleva.",
        highlightLabel = "BUSCADOR"
    ),
    OnboardingStep(
        icon = Icons.Default.Map,
        title = "Sigue el trayecto",
        description = "Toca cualquier ruta para ver el trazado exacto sobre el mapa en tiempo real.",
        highlightLabel = "EL MAPA"
    ),
    OnboardingStep(
        icon = Icons.Default.Help,
        title = "Resuelve tus dudas",
        description = "¿No sabes cuánto cuesta o cada cuánto pasa? Toca el signo de interrogación.",
        highlightLabel = "AYUDA"
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { onboardingSteps.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == onboardingSteps.lastIndex

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(XalaDark)
    ) {
        // ── Fondo de Luces (Ambient Light) ───────────────────────────────────
        AmbientGlow(pagerState.currentPage)

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cabecera con botón Saltar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp, end = 20.dp, start = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "XalaBus",
                    color = XalaAmber,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp
                )

                if (!isLastPage) {
                    TextButton(onClick = onFinish) {
                        Text("Saltar", color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }

            // Pager de contenido
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                userScrollEnabled = true
            ) { page ->
                OnboardingPageContent(onboardingSteps[page])
            }

            // Footer con indicadores y botón
            OnboardingFooter(
                pagerState = pagerState,
                onNext = {
                    if (isLastPage) onFinish()
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            )
        }
    }
}

@Composable
private fun OnboardingPageContent(step: OnboardingStep) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ── Spotlight (El Foco) ─────────────────────────────────────────────
        Box(contentAlignment = Alignment.Center) {
            // Glow radial
            Canvas(modifier = Modifier.size(240.dp)) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(XalaAmber.copy(alpha = 0.15f), Color.Transparent),
                        center = center,
                        radius = size.width / 2
                    )
                )
            }

            // Círculo central con icono
            Surface(
                modifier = Modifier
                    .size(120.dp)
                    .scale(pulseScale),
                shape = CircleShape,
                color = XalaAmber,
                shadowElevation = 20.dp
            ) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    modifier = Modifier.padding(32.dp),
                    tint = Color.Black
                )
            }

            // Etiqueta flotante (Highlight)
            Box(
                modifier = Modifier
                    .offset(y = (-70).dp, x = 50.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = step.highlightLabel,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(60.dp))

        // ── Tooltip Style Card ──────────────────────────────────────────────
        TooltipCard(step.title, step.description)
    }
}

@Composable
private fun TooltipCard(title: String, description: String) {
    Column(
        modifier = Modifier
            .padding(horizontal = 32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Triángulo del Tooltip
        Canvas(modifier = Modifier.size(20.dp, 10.dp)) {
            val path = Path().apply {
                moveTo(size.width / 2, 0f)
                lineTo(0f, size.height)
                lineTo(size.width, size.height)
                close()
            }
            drawPath(path, XalaCard)
        }

        // Cuerpo del Tooltip
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(XalaCard, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingFooter(
    pagerState: androidx.compose.foundation.pager.PagerState,
    onNext: () -> Unit
) {
    val isLastPage = pagerState.currentPage == onboardingSteps.lastIndex

    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Indicadores
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            repeat(onboardingSteps.size) { index ->
                val active = index == pagerState.currentPage
                Box(
                    modifier = Modifier
                        .size(height = 6.dp, width = if (active) 24.dp else 6.dp)
                        .clip(CircleShape)
                        .background(if (active) XalaAmber else Color.White.copy(alpha = 0.2f))
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Botón principal
        Button(
            onClick = onNext,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isLastPage) XalaAmber else Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(
                text = if (isLastPage) "¡EMPEZAR AHORA!" else "SIGUIENTE",
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun AmbientGlow(currentPage: Int) {
    val offset by animateFloatAsState(
        targetValue = currentPage * 200f,
        animationSpec = tween(1000)
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(XalaAmber.copy(alpha = 0.05f), Color.Transparent),
                radius = 800f
            ),
            center = center.copy(x = center.x + (offset - 300f))
        )
    }
}
