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
 * A compact, humanist-feeling hierarchy. Display values are reserved for the one figure that
 * matters on a screen; all other content uses a quieter reading scale.
 */
val AtmosphericTypography = SharedHouseTypography(
    displayMedium = type(46, 50, FontWeight.ExtraBold, letterSpacing = -1.0f, tabular = true),
    displaySmall = type(38, 43, FontWeight.ExtraBold, letterSpacing = -0.75f, tabular = true),
    headlineLarge = type(31, 37, FontWeight.Bold, letterSpacing = -0.45f, tabular = true),
    headlineMedium = type(27, 33, FontWeight.Bold, letterSpacing = -0.32f, tabular = true),
    headlineSmall = type(23, 29, FontWeight.Bold, letterSpacing = -0.18f, tabular = true),
    titleLarge = type(19, 25, FontWeight.SemiBold, letterSpacing = -0.12f),
    titleMedium = type(16, 22, FontWeight.SemiBold),
    titleSmall = type(14, 20, FontWeight.SemiBold),
    bodyLarge = type(16, 24),
    bodyMedium = type(14, 21),
    bodySmall = type(12, 18),
    labelLarge = type(14, 20, FontWeight.SemiBold),
    labelMedium = type(12, 17, FontWeight.Medium, letterSpacing = 0.25f),
    labelSmall = type(11, 15, FontWeight.Medium, letterSpacing = 0.25f),
)
