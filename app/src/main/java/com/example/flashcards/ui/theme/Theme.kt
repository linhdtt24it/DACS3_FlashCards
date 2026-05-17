package com.example.flashcards.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val M3DarkColorScheme = darkColorScheme(
    primary              = DarkFlowColors.primary,
    onPrimary            = DarkFlowColors.onPrimary,
    primaryContainer     = DarkFlowColors.primaryContainer,
    onPrimaryContainer   = DarkFlowColors.textPrimary,
    secondary            = DarkFlowColors.primary,
    background           = DarkFlowColors.background,
    onBackground         = DarkFlowColors.textPrimary,
    surface              = DarkFlowColors.surface,
    onSurface            = DarkFlowColors.textPrimary,
    surfaceVariant       = DarkFlowColors.surfaceVariant,
    onSurfaceVariant     = DarkFlowColors.textSecondary,
    outline              = DarkFlowColors.stroke,
    error                = DarkFlowColors.error,
)

private val M3LightColorScheme = lightColorScheme(
    primary              = LightFlowColors.primary,
    onPrimary            = LightFlowColors.onPrimary,
    primaryContainer     = LightFlowColors.primaryContainer,
    onPrimaryContainer   = LightFlowColors.textPrimary,
    secondary            = LightFlowColors.primary,
    background           = LightFlowColors.background,
    onBackground         = LightFlowColors.textPrimary,
    surface              = LightFlowColors.surface,
    onSurface            = LightFlowColors.textPrimary,
    surfaceVariant       = LightFlowColors.surfaceVariant,
    onSurfaceVariant     = LightFlowColors.textSecondary,
    outline              = LightFlowColors.stroke,
    error                = LightFlowColors.error,
)

@Composable
fun FlashCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val m3Scheme = if (darkTheme) M3DarkColorScheme else M3LightColorScheme
    val flowColors = if (darkTheme) DarkFlowColors else LightFlowColors

    // Tint status bar theo theme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = flowColors.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(LocalFlowColors provides flowColors) {
        MaterialTheme(
            colorScheme = m3Scheme,
            typography  = Typography,
            content     = content
        )
    }
}