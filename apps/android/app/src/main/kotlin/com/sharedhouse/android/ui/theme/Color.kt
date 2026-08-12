package com.sharedhouse.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Exact SharedHouse v2 premium palette. Keep these values stable across all surfaces. */
object PremiumPalette {
    val Base = Color(0xFF0B0C16)
    val HomeGlow = Color(0xFF1A1233)
    val CardLevel1 = Color(0xFF15162B)
    val CardLevel2 = Color(0xFF1B1D3A)
    val Border = Color(0xFF2A2B45)
    val ActiveBorder = Color(0xFF3D2E6B)

    val HeroStart = Color(0xFF7C3AED)
    val HeroMiddle = Color(0xFFA855F7)
    val HeroEnd = Color(0xFFEC4899)
    val HeroAlternateStart = Color(0xFF3B82F6)
    val HeroAlternateEnd = Color(0xFF8B5CF6)
    val AmbientPink = Color(0xFFDB2777)
    val AccentPrimary = Color(0xFF8B5CF6)
    val AccentSecondary = Color(0xFF3B82F6)
    val AccentTertiary = Color(0xFFEC4899)

    val TextPrimary = Color(0xFFF5F5FA)
    val TextSecondary = Color(0xFF9599B8)
    val TextOnGradient = Color(0xFFFFFFFF)
    val TextOnGradientSecondary = Color.White.copy(alpha = .75f)

    val StatusNeutral = Color(0xFF6B7094)
    val StatusPositive = Color(0xFF22C55E)
    val StatusAttention = Color(0xFFF59E0B)
    val StatusNegative = Color(0xFFF43F5E)
    val StatusDisabled = Color(0xFF4B4F6B)
}

@Immutable
data class SharedHouseColors(
    val background: Color,
    val onBackground: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val surfaceContainer: Color,
    val surfaceContainerLow: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val error: Color,
    val errorContainer: Color,
    val onError: Color,
    val onErrorContainer: Color,
    val cardLevel1: Color,
    val cardLevel2: Color,
    val heroStart: Color,
    val heroMiddle: Color,
    val heroEnd: Color,
    val accentBlue: Color,
    val accentPink: Color,
    val statusNeutral: Color,
    val statusPositive: Color,
    val statusAttention: Color,
    val statusNegative: Color,
    val statusDisabled: Color,
)

internal val PremiumDarkColors = SharedHouseColors(
    background = PremiumPalette.Base,
    onBackground = PremiumPalette.TextPrimary,
    surface = PremiumPalette.CardLevel1,
    onSurface = PremiumPalette.TextPrimary,
    surfaceVariant = PremiumPalette.CardLevel2,
    onSurfaceVariant = PremiumPalette.TextSecondary,
    surfaceContainer = PremiumPalette.CardLevel1,
    surfaceContainerLow = Color(0xFF10111F),
    surfaceContainerHigh = PremiumPalette.CardLevel2,
    surfaceContainerHighest = Color(0xFF222442),
    primary = PremiumPalette.AccentPrimary,
    onPrimary = PremiumPalette.TextOnGradient,
    primaryContainer = Color(0xFF2B1D52),
    onPrimaryContainer = PremiumPalette.TextOnGradient,
    secondary = PremiumPalette.AccentSecondary,
    onSecondary = PremiumPalette.TextOnGradient,
    secondaryContainer = Color(0xFF14254B),
    onSecondaryContainer = Color(0xFFDDEAFF),
    tertiary = PremiumPalette.StatusAttention,
    tertiaryContainer = Color(0xFF3B2A0A),
    onTertiaryContainer = Color(0xFFFFE7AD),
    outline = PremiumPalette.ActiveBorder,
    outlineVariant = PremiumPalette.Border,
    error = PremiumPalette.StatusNegative,
    errorContainer = Color(0xFF3B1320),
    onError = PremiumPalette.TextOnGradient,
    onErrorContainer = Color(0xFFFFD9E0),
    cardLevel1 = PremiumPalette.CardLevel1,
    cardLevel2 = PremiumPalette.CardLevel2,
    heroStart = PremiumPalette.HeroStart,
    heroMiddle = PremiumPalette.HeroMiddle,
    heroEnd = PremiumPalette.HeroEnd,
    accentBlue = PremiumPalette.AccentSecondary,
    accentPink = PremiumPalette.AccentTertiary,
    statusNeutral = PremiumPalette.StatusNeutral,
    statusPositive = PremiumPalette.StatusPositive,
    statusAttention = PremiumPalette.StatusAttention,
    statusNegative = PremiumPalette.StatusNegative,
    statusDisabled = PremiumPalette.StatusDisabled,
)

/** A cool, coherent accessibility fallback; dark remains the authored v2 experience. */
internal val PremiumLightColors = SharedHouseColors(
    background = Color(0xFFF7F7FC),
    onBackground = Color(0xFF17172B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17172B),
    surfaceVariant = Color(0xFFF0EFFE),
    onSurfaceVariant = Color(0xFF5F6380),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFF),
    surfaceContainerHigh = Color(0xFFF0EFFE),
    surfaceContainerHighest = Color(0xFFE8E7F7),
    primary = PremiumPalette.AccentPrimary,
    onPrimary = PremiumPalette.TextOnGradient,
    primaryContainer = Color(0xFFE9E1FF),
    onPrimaryContainer = Color(0xFF342060),
    secondary = PremiumPalette.AccentSecondary,
    onSecondary = PremiumPalette.TextOnGradient,
    secondaryContainer = Color(0xFFDDEAFF),
    onSecondaryContainer = Color(0xFF15366C),
    tertiary = Color(0xFFB86D00),
    tertiaryContainer = Color(0xFFFFEBC4),
    onTertiaryContainer = Color(0xFF4D3100),
    outline = Color(0xFF766D9B),
    outlineVariant = Color(0xFFD9D7E8),
    error = Color(0xFFD82D4E),
    errorContainer = Color(0xFFFFE0E6),
    onError = PremiumPalette.TextOnGradient,
    onErrorContainer = Color(0xFF681526),
    cardLevel1 = Color(0xFFFFFFFF),
    cardLevel2 = Color(0xFFF0EFFE),
    heroStart = PremiumPalette.HeroStart,
    heroMiddle = PremiumPalette.HeroMiddle,
    heroEnd = PremiumPalette.HeroEnd,
    accentBlue = PremiumPalette.AccentSecondary,
    accentPink = PremiumPalette.AccentTertiary,
    statusNeutral = Color(0xFF5A5F82),
    statusPositive = Color(0xFF16883F),
    statusAttention = Color(0xFF9B5C00),
    statusNegative = Color(0xFFD82D4E),
    statusDisabled = Color(0xFF777B93),
)
