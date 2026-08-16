package com.sharedhouse.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private fun highContrast(colors: SharedHouseColors, dark: Boolean) = colors.copy(
    onBackground = if (dark) Color.White else Color(0xFF101020),
    onSurface = if (dark) Color.White else Color(0xFF101020),
    onSurfaceVariant = if (dark) Color(0xFFD5E7DE) else Color(0xFF3E554A),
    outline = if (dark) Color(0xFFAEC9BD) else Color(0xFF4D665B),
    outlineVariant = if (dark) Color(0xFF78958A) else Color(0xFF81998E),
)

private val LocalColors = staticCompositionLocalOf { PremiumDarkColors }
private val LocalTypography = staticCompositionLocalOf { AtmosphericTypography }
private val LocalShapes = staticCompositionLocalOf { AtmosphericShapes }
private val LocalMotionEnabled = staticCompositionLocalOf { true }

object AtmosphereTheme {
    val colorScheme: SharedHouseColors
        @Composable @ReadOnlyComposable get() = LocalColors.current
    val typography: SharedHouseTypography
        @Composable @ReadOnlyComposable get() = LocalTypography.current
    val shapes: SharedHouseShapes
        @Composable @ReadOnlyComposable get() = LocalShapes.current
    val motionEnabled: Boolean
        @Composable @ReadOnlyComposable get() = LocalMotionEnabled.current
}

@Composable
fun SharedHouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    highContrast: Boolean = false,
    reducedMotion: Boolean = false,
    content: @Composable () -> Unit,
) {
    // The Horizon palette is deliberately fixed. Retain the setting parameter for source compatibility,
    // but never let a device-derived colour corrupt status semantics or the authored gradients.
    @Suppress("UNUSED_VARIABLE") val deviceAccentRequested = dynamicColor
    val branded = if (darkTheme) PremiumDarkColors else PremiumLightColors
    CompositionLocalProvider(
        LocalColors provides if (highContrast) highContrast(branded, darkTheme) else branded,
        LocalTypography provides AtmosphericTypography,
        LocalShapes provides AtmosphericShapes,
        LocalMotionEnabled provides !reducedMotion,
        content = content,
    )
}
