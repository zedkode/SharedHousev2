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
 * Nova typography pairs sharp display rhythm with pragmatic reading styles. Numeric styles retain
 * tabular figures so household balances and schedules remain stable while values update.
 */
val AtmosphericTypography = SharedHouseTypography(
    displayMedium = type(44, 50, FontWeight.Black, letterSpacing = -1.15f, tabular = true),
    displaySmall = type(36, 42, FontWeight.ExtraBold, letterSpacing = -0.85f, tabular = true),
    headlineLarge = type(30, 36, FontWeight.ExtraBold, letterSpacing = -0.55f, tabular = true),
    headlineMedium = type(25, 31, FontWeight.Bold, letterSpacing = -0.35f, tabular = true),
    headlineSmall = type(21, 27, FontWeight.Bold, letterSpacing = -0.18f, tabular = true),
    titleLarge = type(19, 25, FontWeight.Bold, letterSpacing = -0.10f),
    titleMedium = type(16, 22, FontWeight.SemiBold),
    titleSmall = type(14, 19, FontWeight.SemiBold),
    bodyLarge = type(16, 24, FontWeight.Normal),
    bodyMedium = type(14, 21, FontWeight.Normal),
    bodySmall = type(12, 18, FontWeight.Normal),
    labelLarge = type(14, 20, FontWeight.Bold, letterSpacing = 0.05f),
    labelMedium = type(12, 17, FontWeight.SemiBold, letterSpacing = 0.18f),
    labelSmall = type(11, 15, FontWeight.SemiBold, letterSpacing = 0.22f),
)
