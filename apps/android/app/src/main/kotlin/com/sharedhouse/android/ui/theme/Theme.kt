package com.sharedhouse.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private fun highContrast(colors: SharedHouseColors, dark: Boolean) = colors.copy(
    onBackground = if (dark) Color.White else Color(0xFF11121A),
    onSurface = if (dark) Color.White else Color(0xFF11121A),
    onSurfaceVariant = if (dark) Color(0xFFE0E3F2) else Color(0xFF3F4558),
    outline = if (dark) Color(0xFFC8CFFF) else Color(0xFF505873),
    outlineVariant = if (dark) Color(0xFF969FCA) else Color(0xFF737C99),
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
    // The Nova palette is deliberately fixed. Retain the setting parameter for source compatibility,
    // but never let a device-derived colour alter finance, task or account status semantics.
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
