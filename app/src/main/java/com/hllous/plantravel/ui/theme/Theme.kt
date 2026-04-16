package com.hllous.plantravel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = NightBlue,
    secondary = NightGrayBlue,
    tertiary = NightViolet,
    background = NightBackground,
    surface = NightSurface,
    onPrimary = NightOnPrimary,
    onSecondary = NightOnBackground,
    onBackground = NightOnBackground,
    onSurface = NightOnBackground,
    surfaceVariant = NightGrayBlue,
    onSurfaceVariant = NightOnBackground,
    outline = NightOnBackground.copy(alpha = 0.6f)
)

private val LightColorScheme = lightColorScheme(
    primary = SkyBlue,
    secondary = LightCyan,
    tertiary = DayAccent,
    background = DayBackground,
    surface = DaySurface,
    onPrimary = DayOnPrimary,
    onSecondary = DayOnBackground,
    onBackground = DayOnBackground,
    onSurface = DayOnBackground,
    surfaceVariant = LightCyan,
    onSurfaceVariant = DayOnBackground,
    outline = DayOnBackground.copy(alpha = 0.45f)
)

@Composable
fun ProyectoPlanTravelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        DarkColorScheme
    } else {
        LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}