package com.example.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Light Color Scheme
private val LightColorScheme = lightColorScheme(
    primary = SleekLightPrimary,
    onPrimary = SleekLightOnPrimary,
    primaryContainer = SleekLightPrimaryContainer,
    onPrimaryContainer = SleekLightOnPrimaryContainer,
    secondary = SleekLightSecondary,
    onSecondary = SleekLightOnSecondary,
    secondaryContainer = SleekLightSecondaryContainer,
    onSecondaryContainer = SleekLightOnSecondaryContainer,
    tertiary = SleekLightTertiary,
    tertiaryContainer = SleekLightTertiaryContainer,
    onTertiaryContainer = SleekLightOnTertiaryContainer,
    background = SleekLightBackground,
    surface = SleekLightSurface,
    onBackground = SleekLightOnBackground,
    onSurface = SleekLightOnSurface,
    surfaceVariant = SleekLightSurfaceVariant,
    onSurfaceVariant = SleekLightOnSurfaceVariant,
    outline = SleekLightOutline
)

// Dark Color Scheme
private val DarkColorScheme = darkColorScheme(
    primary = SleekDarkPrimary,
    onPrimary = SleekDarkOnPrimary,
    primaryContainer = SleekDarkPrimaryContainer,
    onPrimaryContainer = SleekDarkOnPrimaryContainer,
    secondary = SleekDarkSecondary,
    onSecondary = SleekDarkOnSecondary,
    secondaryContainer = SleekDarkSecondaryContainer,
    onSecondaryContainer = SleekDarkOnSecondaryContainer,
    tertiary = SleekDarkTertiary,
    tertiaryContainer = SleekDarkTertiaryContainer,
    onTertiaryContainer = SleekDarkOnTertiaryContainer,
    background = SleekDarkBackground,
    surface = SleekDarkSurface,
    onBackground = SleekDarkOnBackground,
    onSurface = SleekDarkOnSurface,
    surfaceVariant = SleekDarkSurfaceVariant,
    onSurfaceVariant = SleekDarkOnSurfaceVariant,
    outline = SleekDarkOutline
)

// High Contrast Color Scheme (Accessibility)
private val HighContrastColorScheme = darkColorScheme(
    primary = HighContrastPrimary,
    onPrimary = Color.Black,
    primaryContainer = Color.White,
    onPrimaryContainer = Color.Black,
    secondary = HighContrastPrimary,
    background = HighContrastBackground,
    surface = HighContrastSurface,
    onBackground = Color.White,
    onSurface = Color.White
)

@Composable
fun AeroStrangeTheme(
    isDark: Boolean = true,
    isHighContrast: Boolean = false,
    content: @Composable () -> Unit
) {
    val colors = when {
        isHighContrast -> HighContrastColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colors,
        typography = Typography,
        content = content
    )
}
