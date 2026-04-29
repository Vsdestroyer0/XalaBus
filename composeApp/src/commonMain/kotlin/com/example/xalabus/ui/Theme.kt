package com.example.xalabus.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// ────────────────────────────────────────────────────────────
//  XALABUS THEME
//  Dark  — fondo negro profundo + acento ámbar brillante
//  Light — fondo marfil cálido  + acento ámbar oscuro (legible)
// ────────────────────────────────────────────────────────────

// —— Ámbar ————————————————————————————————————————————————
private val AmberBright = Color(0xFFF5C518)  // sobre fondo oscuro
private val AmberDeep   = Color(0xFFC67C00)  // sobre fondo claro (≥4.5:1 WCAG AA)
private val AmberContainer  = Color(0xFF2A2100)  // dark
private val AmberContainerL = Color(0xFFFFE5A0)  // light
private val OnAmberContainer  = Color(0xFFF5C518)
private val OnAmberContainerL = Color(0xFF3D2100)

// —— Dark ————————————————————————————————————————————————
val XalaBusDarkColors = darkColorScheme(
    primary              = AmberBright,
    onPrimary            = Color(0xFF000000),
    primaryContainer     = AmberContainer,
    onPrimaryContainer   = OnAmberContainer,

    secondary            = Color(0xFF8A8A8A),
    onSecondary          = Color(0xFF000000),
    secondaryContainer   = Color(0xFF1E1E1E),
    onSecondaryContainer = Color(0xFFCCCCCC),

    tertiary             = Color(0xFFAAAAAA),
    onTertiary           = Color(0xFF000000),

    background           = Color(0xFF0A0A0A),
    onBackground         = Color(0xFFFFFFFF),

    surface              = Color(0xFF161616),
    onSurface            = Color(0xFFFFFFFF),
    surfaceVariant       = Color(0xFF1E1E1E),
    onSurfaceVariant     = Color(0xFF8A8A8A),

    outline              = Color(0xFF2C2C2C),
    outlineVariant       = Color(0xFF3A3A3A),

    error                = Color(0xFFFF4444),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFF400000),
    onErrorContainer     = Color(0xFFFF9999),

    inverseSurface       = Color(0xFFF5F2EC),
    inverseOnSurface     = Color(0xFF1A1A1A),
    inversePrimary       = AmberDeep,
)

// —— Light ————————————————————————————————————————————————
val XalaBusLightColors = lightColorScheme(
    primary              = AmberDeep,
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = AmberContainerL,
    onPrimaryContainer   = OnAmberContainerL,

    secondary            = Color(0xFF6B6560),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFF5F2EC),
    onSecondaryContainer = Color(0xFF3A3530),

    tertiary             = Color(0xFF8B7355),
    onTertiary           = Color(0xFFFFFFFF),

    background           = Color(0xFFFAFAF8),  // marfil cálido
    onBackground         = Color(0xFF1A1A1A),

    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1A1A1A),
    surfaceVariant       = Color(0xFFF5F2EC),
    onSurfaceVariant     = Color(0xFF5C5748),

    outline              = Color(0xFFC4BFB6),
    outlineVariant       = Color(0xFFDDD9D2),

    error                = Color(0xFFB00020),
    onError              = Color(0xFFFFFFFF),
    errorContainer       = Color(0xFFFFDAD4),
    onErrorContainer     = Color(0xFF410001),

    inverseSurface       = Color(0xFF161616),
    inverseOnSurface     = Color(0xFFFFFFFF),
    inversePrimary       = AmberBright,
)
