package com.sharedhouse.android.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Immutable
data class SharedHouseTypography(
    val displayMedium: TextStyle,
    val displaySmall: TextStyle,
    val headlineLarge: TextStyle,
    val headlineMedium: TextStyle,
    val headlineSmall: TextStyle,
    val titleLarge: TextStyle,
    val titleMedium: TextStyle,
    val titleSmall: TextStyle,
    val bodyLarge: TextStyle,
    val bodyMedium: TextStyle,
    val bodySmall: TextStyle,
    val labelLarge: TextStyle,
    val labelMedium: TextStyle,
    val labelSmall: TextStyle,
)

private fun type(
    size: Int,
    lineHeight: Int,
    weight: FontWeight = FontWeight.Normal,
    family: FontFamily = FontFamily.SansSerif,
    letterSpacing: Float = 0f,
    tabular: Boolean = false,
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = letterSpacing.sp,
    fontFeatureSettings = if (tabular) "tnum" else null,
)

/**
 * A calm, system-font hierarchy inspired by compact native mobile interfaces. The largest styles
 * are reserved for the one value that matters; numeric styles retain tabular figures for balances.
 */
val AtmosphericTypography = SharedHouseTypography(
    displayMedium = type(40, 46, FontWeight.Bold, letterSpacing = -0.85f, tabular = true),
    displaySmall = type(34, 40, FontWeight.Bold, letterSpacing = -0.60f, tabular = true),
    headlineLarge = type(28, 34, FontWeight.Bold, letterSpacing = -0.38f, tabular = true),
    headlineMedium = type(24, 30, FontWeight.Bold, letterSpacing = -0.24f, tabular = true),
    headlineSmall = type(20, 26, FontWeight.SemiBold, letterSpacing = -0.12f, tabular = true),
    titleLarge = type(19, 25, FontWeight.SemiBold, letterSpacing = -0.08f),
    titleMedium = type(16, 22, FontWeight.SemiBold),
    titleSmall = type(14, 19, FontWeight.SemiBold),
    bodyLarge = type(16, 22, FontWeight.Normal),
    bodyMedium = type(14, 20, FontWeight.Normal),
    bodySmall = type(12, 17, FontWeight.Normal),
    labelLarge = type(14, 19, FontWeight.SemiBold),
    labelMedium = type(12, 16, FontWeight.Medium, letterSpacing = 0.12f),
    labelSmall = type(11, 14, FontWeight.Medium, letterSpacing = 0.16f),
)
