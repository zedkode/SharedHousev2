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

/** Sans-only premium type scale. Screen headings are 28-32sp; hero amounts are 40-48sp. */
val AtmosphericTypography = SharedHouseTypography(
    displayMedium = type(48, 52, FontWeight.ExtraBold, letterSpacing = -1.1f, tabular = true),
    displaySmall = type(40, 45, FontWeight.ExtraBold, letterSpacing = -0.85f, tabular = true),
    headlineLarge = type(32, 38, FontWeight.Bold, letterSpacing = -0.55f, tabular = true),
    headlineMedium = type(28, 34, FontWeight.Bold, letterSpacing = -0.4f, tabular = true),
    headlineSmall = type(24, 30, FontWeight.Bold, letterSpacing = -0.25f, tabular = true),
    titleLarge = type(18, 24, FontWeight.SemiBold, letterSpacing = -0.1f),
    titleMedium = type(16, 22, FontWeight.SemiBold),
    titleSmall = type(15, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 20),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 17, FontWeight.Medium, letterSpacing = .3f),
    labelSmall = type(11, 15, FontWeight.Medium, letterSpacing = .3f),
)
