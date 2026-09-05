package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Slate900,
    onPrimary = Color.White,
    primaryContainer = Slate100,
    onPrimaryContainer = Slate900,
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = BrandTealLight,
    onSecondaryContainer = Slate900,
    tertiary = CoralOrange,
    onTertiary = Color.White,
    background = Slate50,
    onBackground = Slate900,
    surface = Color.White,
    onSurface = Slate900,
    surfaceVariant = Slate100,
    onSurfaceVariant = Slate700,
    outline = Slate200,
    outlineVariant = Slate100
)

// Reserved for future dedicated dark theme implementation
@Suppress("unused")
private val DarkColorScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Slate900,
    primaryContainer = Slate800,
    onPrimaryContainer = Color.White,
    secondary = BrandTeal,
    onSecondary = Color.White,
    secondaryContainer = Slate700,
    onSecondaryContainer = Color.White,
    tertiary = CoralOrange,
    onTertiary = Color.White,
    background = Slate900,
    onBackground = Color.White,
    surface = Slate800,
    onSurface = Color.White,
    surfaceVariant = Slate700,
    onSurfaceVariant = Slate200,
    outline = Slate700,
    outlineVariant = Slate800
)

@Composable
fun PhittoosTheme(
    darkTheme: Boolean = false, // Phittoos MVP enforces a deterministic brand color scheme independent of system dark/light mode
    dynamicColor: Boolean = false, // Keep Phittoos brand identity consistent (no dynamic colors)
    content: @Composable () -> Unit
) {
    // For the current MVP, Phittoos has one controlled visual design (slate/navy accents, white cards/surfaces, dark text).
    // Always use LightColorScheme so system dark mode does NOT alter Phittoos UI colors.
    val colorScheme = LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
