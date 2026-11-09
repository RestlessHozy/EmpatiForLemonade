package com.example.empati.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/*
 * Light: "daylight on paper" – warm stone canvas, white raised cards, sage primary.
 * Dark:  "quiet evening"     – green-black canvas (not purple), lifted tonal cards, luminous sage.
 */
private val LightColorScheme: ColorScheme = lightColorScheme(
    primary = Sage40,
    onPrimary = Color.White,
    primaryContainer = Sage95,
    onPrimaryContainer = Sage20,
    inversePrimary = Sage80,

    secondary = Clay40,
    onSecondary = Color.White,
    secondaryContainer = Clay95,
    onSecondaryContainer = Clay10,

    tertiary = Mist40,
    onTertiary = Color.White,
    tertiaryContainer = Mist90,
    onTertiaryContainer = Mist10,

    error = Terra40,
    onError = Color.White,
    errorContainer = Terra90,
    onErrorContainer = Terra10,

    background = StoneLightBackground,
    onBackground = StoneLightOnSurface,
    surface = StoneLightBackground,
    onSurface = StoneLightOnSurface,
    surfaceVariant = StoneLightHigh,
    onSurfaceVariant = StoneLightOnVariant,
    surfaceTint = Sage40,
    inverseSurface = StoneLightInverse,
    inverseOnSurface = StoneLightInverseOn,

    outline = StoneLightOutline,
    outlineVariant = StoneLightOutlineVariant,
    scrim = Color.Black,

    surfaceBright = StoneLightBright,
    surfaceDim = StoneLightDim,
    surfaceContainerLowest = StoneLightLowest,
    surfaceContainerLow = StoneLightLow,
    surfaceContainer = StoneLightMid,
    surfaceContainerHigh = StoneLightHigh,
    surfaceContainerHighest = StoneLightHighest
)

private val DarkColorScheme: ColorScheme = darkColorScheme(
    primary = Sage80,
    onPrimary = Sage20,
    primaryContainer = Sage30,
    onPrimaryContainer = Sage90,
    inversePrimary = Sage40,

    secondary = Clay80,
    onSecondary = Clay10,
    secondaryContainer = Clay30,
    onSecondaryContainer = Clay90,

    tertiary = Mist80,
    onTertiary = Mist10,
    tertiaryContainer = Mist30,
    onTertiaryContainer = Mist90,

    error = Terra80,
    onError = Terra10,
    errorContainer = Terra25,
    onErrorContainer = Terra90,

    background = StoneDarkBackground,
    onBackground = StoneDarkOnSurface,
    surface = StoneDarkBackground,
    onSurface = StoneDarkOnSurface,
    surfaceVariant = StoneDarkHigh,
    onSurfaceVariant = StoneDarkOnVariant,
    surfaceTint = Sage80,
    inverseSurface = StoneLightInverseOn,
    inverseOnSurface = StoneLightInverse,

    outline = StoneDarkOutline,
    outlineVariant = StoneDarkOutlineVariant,
    scrim = Color.Black,

    surfaceBright = StoneDarkBright,
    surfaceDim = StoneDarkDim,
    surfaceContainerLowest = StoneDarkLowest,
    surfaceContainerLow = StoneDarkLow,
    surfaceContainer = StoneDarkMid,
    surfaceContainerHigh = StoneDarkHigh,
    surfaceContainerHighest = StoneDarkHighest
)

/** Rounder across the board: containers feel like pebbles, not panels. */
val EmpatiShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

internal val LocalEmpatiColors = staticCompositionLocalOf { LightEmpatiColors }

/** Affective-state tones: `MaterialTheme.empati.calm.container`, etc. */
val MaterialTheme.empati: EmpatiColors
    @Composable
    @ReadOnlyComposable
    get() = LocalEmpatiColors.current

@Composable
fun EmpatiTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    reducedMotion: Boolean = rememberSystemReducedMotion(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalEmpatiColors provides if (darkTheme) DarkEmpatiColors else LightEmpatiColors,
        LocalReducedMotion provides reducedMotion
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = EmpatiTypography,
            shapes = EmpatiShapes,
            content = content
        )
    }
}
