package com.hllous.plantravel.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColorScheme = lightColorScheme(
    primary                = AtlasLightPrimary,
    onPrimary              = AtlasLightOnPrimary,
    primaryContainer       = AtlasLightPrimaryContainer,
    onPrimaryContainer     = AtlasLightOnPrimaryContainer,
    secondary              = AtlasLightSecondary,
    onSecondary            = AtlasLightOnSecondary,
    secondaryContainer     = AtlasLightSecondaryContainer,
    onSecondaryContainer   = AtlasLightOnSecondaryContainer,
    tertiary               = AtlasLightTertiary,
    onTertiary             = AtlasLightOnTertiary,
    tertiaryContainer      = AtlasLightTertiaryContainer,
    onTertiaryContainer    = AtlasLightOnTertiaryContainer,
    error                  = AtlasLightError,
    onError                = AtlasLightOnError,
    errorContainer         = AtlasLightErrorContainer,
    onErrorContainer       = AtlasLightOnErrorContainer,
    background             = AtlasLightBackground,
    onBackground           = AtlasLightOnBackground,
    surface                = AtlasLightSurface,
    onSurface              = AtlasLightOnSurface,
    surfaceVariant         = AtlasLightSurfaceVariant,
    onSurfaceVariant       = AtlasLightOnSurfaceVariant,
    outline                = AtlasLightOutline,
    outlineVariant         = AtlasLightOutlineVariant,
    inverseSurface         = AtlasLightInverseSurface,
    inverseOnSurface       = AtlasLightInverseOnSurface,
    inversePrimary         = AtlasLightInversePrimary,
    scrim                  = AtlasLightScrim,
)

private val DarkColorScheme = darkColorScheme(
    primary                = AtlasDarkPrimary,
    onPrimary              = AtlasDarkOnPrimary,
    primaryContainer       = AtlasDarkPrimaryContainer,
    onPrimaryContainer     = AtlasDarkOnPrimaryContainer,
    secondary              = AtlasDarkSecondary,
    onSecondary            = AtlasDarkOnSecondary,
    secondaryContainer     = AtlasDarkSecondaryContainer,
    onSecondaryContainer   = AtlasDarkOnSecondaryContainer,
    tertiary               = AtlasDarkTertiary,
    onTertiary             = AtlasDarkOnTertiary,
    tertiaryContainer      = AtlasDarkTertiaryContainer,
    onTertiaryContainer    = AtlasDarkOnTertiaryContainer,
    error                  = AtlasDarkError,
    onError                = AtlasDarkOnError,
    errorContainer         = AtlasDarkErrorContainer,
    onErrorContainer       = AtlasDarkOnErrorContainer,
    background             = AtlasDarkBackground,
    onBackground           = AtlasDarkOnBackground,
    surface                = AtlasDarkSurface,
    onSurface              = AtlasDarkOnSurface,
    surfaceVariant         = AtlasDarkSurfaceVariant,
    onSurfaceVariant       = AtlasDarkOnSurfaceVariant,
    outline                = AtlasDarkOutline,
    outlineVariant         = AtlasDarkOutlineVariant,
    inverseSurface         = AtlasDarkInverseSurface,
    inverseOnSurface       = AtlasDarkInverseOnSurface,
    inversePrimary         = AtlasDarkInversePrimary,
    scrim                  = AtlasDarkScrim,
)

@Composable
fun ProyectoPlanTravelTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
