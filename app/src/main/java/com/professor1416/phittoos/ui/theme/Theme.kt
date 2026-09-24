package com.professor1416.phittoos.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.professor1416.phittoos.data.preferences.AppThemeMode

private val LightColorScheme = lightColorScheme(
    primary = EmeraldGreenDark,
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

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldGreen,
    onPrimary = Slate950,
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
    themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    darkTheme: Boolean = when (themeMode) {
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
    },
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val financialColors = if (darkTheme) DarkFinancialColors else LightFinancialColors

    CompositionLocalProvider(LocalFinancialColors provides financialColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
