package com.sharedhouse.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * SharedHouse Horizon palette.
 *
 * The palette replaces the previous neon-violet treatment with a calm architectural system:
 * deep evergreen for focus, teal for action, sky for orientation and warm amber for attention.
 * It remains deliberately fixed so a device palette can never alter financial or status meaning.
 */
object PremiumPalette {
    val Base = Color(0xFF0D1714)
    val HomeGlow = Color(0xFF123D35)
    val CardLevel1 = Color(0xFF14211D)
    val CardLevel2 = Color(0xFF1B2B26)
    val Border = Color(0xFF2B3D36)
    val ActiveBorder = Color(0xFF3F7564)

    val HeroStart = Color(0xFF0F766E)
    val HeroMiddle = Color(0xFF14B8A6)
    val HeroEnd = Color(0xFF38BDF8)
    val HeroAlternateStart = Color(0xFF2563EB)
    val HeroAlternateEnd = Color(0xFF22C55E)
    val AmbientPink = Color(0xFFF59E0B)
    val AccentPrimary = Color(0xFF2DD4BF)
    val AccentSecondary = Color(0xFF38BDF8)
    val AccentTertiary = Color(0xFFFB7185)

    val TextPrimary = Color(0xFFF2FBF6)
    val TextSecondary = Color(0xFFA8BCB3)
    val TextOnGradient = Color(0xFFFFFFFF)
    val TextOnGradientSecondary = Color.White.copy(alpha = .78f)

    val StatusNeutral = Color(0xFF91A69D)
    val StatusPositive = Color(0xFF34D399)
    val StatusAttention = Color(0xFFFBBF24)
    val StatusNegative = Color(0xFFFB7185)
    val StatusDisabled = Color(0xFF60756C)
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
    surfaceContainerLow = Color(0xFF101C18),
    surfaceContainerHigh = PremiumPalette.CardLevel2,
    surfaceContainerHighest = Color(0xFF24352F),
    primary = PremiumPalette.AccentPrimary,
    onPrimary = Color(0xFF062C26),
    primaryContainer = Color(0xFF16473D),
    onPrimaryContainer = Color(0xFFD6FFF4),
    secondary = PremiumPalette.AccentSecondary,
    onSecondary = Color(0xFF06283B),
    secondaryContainer = Color(0xFF113D51),
    onSecondaryContainer = Color(0xFFD5F4FF),
    tertiary = PremiumPalette.StatusAttention,
    tertiaryContainer = Color(0xFF4A3308),
    onTertiaryContainer = Color(0xFFFFE9B2),
    outline = PremiumPalette.ActiveBorder,
    outlineVariant = PremiumPalette.Border,
    error = PremiumPalette.StatusNegative,
    errorContainer = Color(0xFF4A1E2A),
    onError = Color(0xFF3A0712),
    onErrorContainer = Color(0xFFFFD9E1),
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

/** Light mode is intentionally matte and paper-like rather than a washed-out copy of dark mode. */
internal val PremiumLightColors = SharedHouseColors(
    background = Color(0xFFF4F7F4),
    onBackground = Color(0xFF17231E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF17231E),
    surfaceVariant = Color(0xFFE8F0EC),
    onSurfaceVariant = Color(0xFF52675D),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF9FBF9),
    surfaceContainerHigh = Color(0xFFE8F0EC),
    surfaceContainerHighest = Color(0xFFDCE8E2),
    primary = Color(0xFF087B70),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF8EE),
    onPrimaryContainer = Color(0xFF03473F),
    secondary = Color(0xFF1678A2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD7F1FD),
    onSecondaryContainer = Color(0xFF083E58),
    tertiary = Color(0xFF9A6700),
    tertiaryContainer = Color(0xFFFFEABB),
    onTertiaryContainer = Color(0xFF4C3200),
    outline = Color(0xFF71877C),
    outlineVariant = Color(0xFFC9D9D1),
    error = Color(0xFFBE3151),
    errorContainer = Color(0xFFFFE0E7),
    onError = Color.White,
    onErrorContainer = Color(0xFF650B25),
    cardLevel1 = Color(0xFFFFFFFF),
    cardLevel2 = Color(0xFFE8F0EC),
    heroStart = PremiumPalette.HeroStart,
    heroMiddle = PremiumPalette.HeroMiddle,
    heroEnd = PremiumPalette.HeroEnd,
    accentBlue = PremiumPalette.AccentSecondary,
    accentPink = PremiumPalette.AccentTertiary,
    statusNeutral = Color(0xFF546B60),
    statusPositive = Color(0xFF087B53),
    statusAttention = Color(0xFF8A5A00),
    statusNegative = Color(0xFFBE3151),
    statusDisabled = Color(0xFF71877C),
)
