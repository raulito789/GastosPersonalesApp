package com.raulito.gastospersonales.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val TealPrimary = Color(0xFF0F766E)
private val TealSecondary = Color(0xFF14B8A6)
private val AmberTertiary = Color(0xFFD97706)
private val SurfaceLight = Color(0xFFF6F8FA)
private val SurfaceDark = Color(0xFF0F172A)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCCFBF1),
    onPrimaryContainer = Color(0xFF042F2E),
    secondary = TealSecondary,
    onSecondary = Color.White,
    tertiary = AmberTertiary,
    background = SurfaceLight,
    surface = Color.White,
    surfaceVariant = Color(0xFFE2E8F0),
    error = Color(0xFFDC2626)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF2DD4BF),
    onPrimary = Color(0xFF042F2E),
    primaryContainer = Color(0xFF115E59),
    onPrimaryContainer = Color(0xFFCCFBF1),
    secondary = TealSecondary,
    tertiary = Color(0xFFFBBF24),
    background = SurfaceDark,
    surface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFF334155),
    error = Color(0xFFF87171)
)

@Composable
fun GastOSTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
