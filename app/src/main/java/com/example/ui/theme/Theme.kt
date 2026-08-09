package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    primaryContainer = GreenContainer,
    onPrimary = Color.White,
    onPrimaryContainer = Color(0xFF00210B),
    surface = Color.White,
    onSurface = Color(0xFF1F1F1F),
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF444746),
    background = SurfaceLight,
    onBackground = Color(0xFF1F1F1F)
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    primaryContainer = Color(0xFF1B5E20),
    onPrimary = Color(0xFF003811),
    onPrimaryContainer = Color(0xFFA5D6A7),
    surface = Color(0xFF121212),
    onSurface = Color(0xFFE2E2E2),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFC4C7C5),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE2E2E2)
)

@Composable
fun UangkuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
