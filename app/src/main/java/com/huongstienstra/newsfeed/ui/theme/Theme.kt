package com.huongstienstra.newsfeed.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ColorOnDarkPrimary = Color.White
private val ColorOnLightAccent = Color(0xFF111111)

private val DarkColorScheme = darkColorScheme(
    primary = NewsFlowTealDark,
    onPrimary = ColorOnLightAccent,
    primaryContainer = NewsFlowTeal,
    onPrimaryContainer = Color.White,
    secondary = NewsFlowCoralDark,
    onSecondary = ColorOnLightAccent,
    secondaryContainer = NewsFlowCoral,
    onSecondaryContainer = Color.White,
    tertiary = NewsFlowAmberDark,
    onTertiary = ColorOnLightAccent,
    background = BackgroundDark,
    onBackground = InkDark,
    surface = SurfaceDark,
    onSurface = InkDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = MutedDark,
    outline = OutlineDark,
    error = ErrorDark,
)

private val LightColorScheme = lightColorScheme(
    primary = NewsFlowTeal,
    onPrimary = ColorOnDarkPrimary,
    primaryContainer = NewsFlowTealDark,
    onPrimaryContainer = InkLight,
    secondary = NewsFlowCoral,
    onSecondary = ColorOnDarkPrimary,
    secondaryContainer = NewsFlowCoralDark,
    onSecondaryContainer = InkLight,
    tertiary = NewsFlowAmber,
    onTertiary = InkLight,
    background = BackgroundLight,
    onBackground = InkLight,
    surface = SurfaceLight,
    onSurface = InkLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = MutedLight,
    outline = OutlineLight,
    error = ErrorLight,
)

@Composable
fun NewsFeedSystemDesignTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (dynamicColor) {
        if (darkTheme) DarkColorScheme else LightColorScheme
    } else {
        if (darkTheme) DarkColorScheme else LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
