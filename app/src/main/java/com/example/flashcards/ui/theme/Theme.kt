package com.example.flashcards.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = FlowPrimary,
    secondary = FlowPrimaryLight,
    tertiary = FlowSuccess,
    background = Color(0xFF1E1E1E),
    surface = Color(0xFF2D2D2D),
    onPrimary = Color.White,
    onBackground = Color.White,
    onSurface = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = FlowPrimary,
    secondary = FlowPrimaryLight,
    tertiary = FlowSuccess,
    background = FlowBackground,
    surface = FlowSurface,
    onPrimary = Color.White,
    onSecondary = FlowTextPrimary,
    onTertiary = Color.White,
    onBackground = FlowTextPrimary,
    onSurface = FlowTextPrimary,
    surfaceVariant = FlowBackground,
    onSurfaceVariant = FlowTextSecondary
)

@Composable
fun FlashCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamicColor to enforce FlowCards branding
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}