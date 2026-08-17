package com.sharedhouse.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * SharedHouse Cupertino palette.
 *
 * The system favours iOS-inspired grouped surfaces, a restrained system-blue action colour and
 * stable semantic statuses. It is intentionally functional rather than decorative: content and
 * hierarchy stay dominant in every household workflow.
 */
object PremiumPalette {
    val Base = Color(0xFF000000)
    val HomeGlow = Color(0xFF1C1C1E)
    val CardLevel1 = Color(0xFF1C1C1E)
    val CardLevel2 = Color(0xFF2C2C2E)
    val Border = Color(0xFF38383A)
    val ActiveBorder = Color(0xFF0A84FF)

    val HeroStart = Color(0xFF0A84FF)
    val HeroMiddle = Color(0xFF0A84FF)
    val HeroEnd = Color(0xFF0A84FF)
    val HeroAlternateStart = Color(0xFF5E5CE6)
    val HeroAlternateEnd = Color(0xFF0A84FF)
    val AmbientPink = Color(0xFFFF9F0A)
    val AccentPrimary = Color(0xFF0A84FF)
    val AccentSecondary = Color(0xFF64D2FF)
    val AccentTertiary = Color(0xFFBF5AF2)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFAEAEB2)
    val TextOnGradient = Color(0xFFFFFFFF)
    val TextOnGradientSecondary = Color.White.copy(alpha = .82f)

    val StatusNeutral = Color(0xFF8E8E93)
    val StatusPositive = Color(0xFF30D158)
    val StatusAttention = Color(0xFFFF9F0A)
    val StatusNegative = Color(0xFFFF453A)
    val StatusDisabled = Color(0xFF636366)
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
    surfaceContainerLow = Color(0xFF0B0B0D),
    surfaceContainerHigh = PremiumPalette.CardLevel2,
    surfaceContainerHighest = Color(0xFF3A3A3C),
    primary = PremiumPalette.AccentPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF003F7D),
    onPrimaryContainer = Color(0xFFD9ECFF),
    secondary = PremiumPalette.AccentSecondary,
    onSecondary = Color(0xFF00344B),
    secondaryContainer = Color(0xFF0B3C58),
    onSecondaryContainer = Color(0xFFCEFAFF),
    tertiary = PremiumPalette.AccentTertiary,
    tertiaryContainer = Color(0xFF502066),
    onTertiaryContainer = Color(0xFFF2D7FF),
    outline = Color(0xFF636366),
    outlineVariant = PremiumPalette.Border,
    error = PremiumPalette.StatusNegative,
    errorContainer = Color(0xFF5A1D1A),
    onError = Color.White,
    onErrorContainer = Color(0xFFFFDAD6),
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

/** Light mode uses the grouped system surfaces familiar from focused, content-first mobile apps. */
internal val PremiumLightColors = SharedHouseColors(
    background = Color(0xFFF2F2F7),
    onBackground = Color(0xFF000000),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF000000),
    surfaceVariant = Color(0xFFE5E5EA),
    onSurfaceVariant = Color(0xFF6C6C70),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F2F7),
    surfaceContainerHigh = Color(0xFFEFEFF4),
    surfaceContainerHighest = Color(0xFFE5E5EA),
    primary = Color(0xFF007AFF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9ECFF),
    onPrimaryContainer = Color(0xFF003A75),
    secondary = Color(0xFF007AFF),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE0F0FF),
    onSecondaryContainer = Color(0xFF00355E),
    tertiary = Color(0xFFAF52DE),
    tertiaryContainer = Color(0xFFF5D9FF),
    onTertiaryContainer = Color(0xFF5B197F),
    outline = Color(0xFF8E8E93),
    outlineVariant = Color(0xFFC6C6C8),
    error = Color(0xFFFF3B30),
    errorContainer = Color(0xFFFFE5E3),
    onError = Color.White,
    onErrorContainer = Color(0xFF8A1009),
    cardLevel1 = Color.White,
    cardLevel2 = Color(0xFFEFEFF4),
    heroStart = Color(0xFF007AFF),
    heroMiddle = Color(0xFF007AFF),
    heroEnd = Color(0xFF007AFF),
    accentBlue = Color(0xFF007AFF),
    accentPink = Color(0xFFAF52DE),
    statusNeutral = Color(0xFF8E8E93),
    statusPositive = Color(0xFF34C759),
    statusAttention = Color(0xFFFF9500),
    statusNegative = Color(0xFFFF3B30),
    statusDisabled = Color(0xFF8E8E93),
)
