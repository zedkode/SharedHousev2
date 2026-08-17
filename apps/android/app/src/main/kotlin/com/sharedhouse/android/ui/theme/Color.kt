package com.sharedhouse.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * SharedHouse Nova palette.
 *
 * A crisp product system built for dense household coordination: ink surfaces keep data calm,
 * electric indigo makes the primary path unmistakable, and lilac accents add energy without
 * turning the interface into a gradient-led dashboard. Semantic colors remain stable in both
 * themes so money, task and account states never depend on decorative color.
 */
object PremiumPalette {
    val Base = Color(0xFF0B0D14)
    val HomeGlow = Color(0xFF161B33)
    val CardLevel1 = Color(0xFF151927)
    val CardLevel2 = Color(0xFF1D2234)
    val Border = Color(0xFF2B3148)
    val ActiveBorder = Color(0xFF6676FF)

    val HeroStart = Color(0xFF5B5CEB)
    val HeroMiddle = Color(0xFF6D5DFB)
    val HeroEnd = Color(0xFF8A7DFF)
    val HeroAlternateStart = Color(0xFF1565D8)
    val HeroAlternateEnd = Color(0xFF6676FF)
    val AmbientPink = Color(0xFFFFA6D7)
    val AccentPrimary = Color(0xFF8E8CFF)
    val AccentSecondary = Color(0xFF55B7FF)
    val AccentTertiary = Color(0xFFF3B7FF)

    val TextPrimary = Color(0xFFF7F7FC)
    val TextSecondary = Color(0xFFB8BECE)
    val TextOnGradient = Color(0xFFFFFFFF)
    val TextOnGradientSecondary = Color.White.copy(alpha = .82f)

    val StatusNeutral = Color(0xFF9FA8BC)
    val StatusPositive = Color(0xFF4DD8A2)
    val StatusAttention = Color(0xFFFFC269)
    val StatusNegative = Color(0xFFFF809D)
    val StatusDisabled = Color(0xFF6F778B)
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
    surfaceContainer = Color(0xFF111521),
    surfaceContainerLow = Color(0xFF0F121C),
    surfaceContainerHigh = PremiumPalette.CardLevel2,
    surfaceContainerHighest = Color(0xFF272D41),
    primary = PremiumPalette.AccentPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF292762),
    onPrimaryContainer = Color(0xFFE5E4FF),
    secondary = PremiumPalette.AccentSecondary,
    onSecondary = Color(0xFF001E35),
    secondaryContainer = Color(0xFF123858),
    onSecondaryContainer = Color(0xFFD9EFFF),
    tertiary = PremiumPalette.AccentTertiary,
    tertiaryContainer = Color(0xFF4A244F),
    onTertiaryContainer = Color(0xFFFFD7F8),
    outline = PremiumPalette.ActiveBorder,
    outlineVariant = PremiumPalette.Border,
    error = PremiumPalette.StatusNegative,
    errorContainer = Color(0xFF4B1F31),
    onError = Color(0xFFFFFFFF),
    onErrorContainer = Color(0xFFFFD9E2),
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

/** Light mode uses soft mineral surfaces and saturated controls rather than a washed-out dark theme. */
internal val PremiumLightColors = SharedHouseColors(
    background = Color(0xFFF8F8FC),
    onBackground = Color(0xFF171923),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171923),
    surfaceVariant = Color(0xFFF0F1F8),
    onSurfaceVariant = Color(0xFF5E6476),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFDFDFF),
    surfaceContainerHigh = Color(0xFFF0F1F8),
    surfaceContainerHighest = Color(0xFFE4E6F0),
    primary = Color(0xFF5548D9),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE8E5FF),
    onPrimaryContainer = Color(0xFF2D247D),
    secondary = Color(0xFF1677C5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDECFF),
    onSecondaryContainer = Color(0xFF003257),
    tertiary = Color(0xFF9B3FAB),
    tertiaryContainer = Color(0xFFFFD8F7),
    onTertiaryContainer = Color(0xFF57205F),
    outline = Color(0xFF7B8299),
    outlineVariant = Color(0xFFD5D8E6),
    error = Color(0xFFBE3154),
    errorContainer = Color(0xFFFFE0E7),
    onError = Color.White,
    onErrorContainer = Color(0xFF680A28),
    cardLevel1 = Color(0xFFFFFFFF),
    cardLevel2 = Color(0xFFF0F1F8),
    heroStart = PremiumPalette.HeroStart,
    heroMiddle = PremiumPalette.HeroMiddle,
    heroEnd = PremiumPalette.HeroEnd,
    accentBlue = PremiumPalette.AccentSecondary,
    accentPink = PremiumPalette.AccentTertiary,
    statusNeutral = Color(0xFF60677A),
    statusPositive = Color(0xFF087853),
    statusAttention = Color(0xFF8A5800),
    statusNegative = Color(0xFFBE3154),
    statusDisabled = Color(0xFF7B8299),
)
