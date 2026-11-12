package com.example.ui.theme

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
    primary = RecallVioletPrimary,
    onPrimary = RecallVioletOnPrimary,
    primaryContainer = RecallVioletContainer,
    onPrimaryContainer = RecallVioletOnContainer,
    secondary = RecallCyanSecondary,
    onSecondary = RecallCyanOnSecondary,
    secondaryContainer = RecallCyanContainer,
    onSecondaryContainer = RecallCyanOnContainer,
    tertiary = RecallAmberTertiary,
    onTertiary = RecallAmberOnTertiary,
    tertiaryContainer = RecallAmberContainer,
    onTertiaryContainer = RecallAmberOnContainer,
    background = RecallDarkBackground,
    onBackground = RecallDarkOnSurface,
    surface = RecallDarkSurface,
    onSurface = RecallDarkOnSurface,
    surfaceVariant = RecallDarkSurfaceVariant,
    onSurfaceVariant = RecallDarkOnSurfaceVariant,
    outline = Color(0xFF475569)
)

private val LightColorScheme = lightColorScheme(
    primary = RecallVioletPrimary,
    onPrimary = RecallVioletOnPrimary,
    primaryContainer = RecallVioletOnContainer,
    onPrimaryContainer = RecallVioletContainer,
    secondary = Color(0xFF0891B2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCFFAFE),
    onSecondaryContainer = Color(0xFF164E63),
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFEF3C7),
    onTertiaryContainer = Color(0xFF78350F),
    background = RecallLightBackground,
    onBackground = RecallLightOnSurface,
    surface = RecallLightSurface,
    onSurface = RecallLightOnSurface,
    surfaceVariant = RecallLightSurfaceVariant,
    onSurfaceVariant = RecallLightOnSurfaceVariant,
    outline = Color(0xFFCBD5E1)
)

@Composable
fun RecallTheme(
    darkTheme: Boolean = true, // Consistently dark cybernetic theme with high-contrast text throughout
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backwards compatibility alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) = RecallTheme(darkTheme, dynamicColor, content)
