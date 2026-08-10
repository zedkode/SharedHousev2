package com.sharedhouse.android.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val SharedHouseLightColors = lightColorScheme(
    primary = Color(0xFF2D5A43),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E8DC),
    onPrimaryContainer = Color(0xFF0F261B),
    secondary = Color(0xFFC05A3E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF7EBE6),
    onSecondaryContainer = Color(0xFF3B160C),
    tertiary = Color(0xFF5B7055),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE2EBE0),
    onTertiaryContainer = Color(0xFF182416),
    background = Color(0xFFEFF2EE),
    onBackground = Color(0xFF141F18),
    surface = Color(0xFFFBFDFB),
    onSurface = Color(0xFF141F18),
    surfaceVariant = Color(0xFFDFE6E0),
    onSurfaceVariant = Color(0xFF3F4E44),
    surfaceContainer = Color(0xFFF4F8F4),
    surfaceContainerLow = Color(0xFFEFF3EF),
    outline = Color(0xFFBCC7BF),
    outlineVariant = Color(0xFFD4DDD6),
    error = Color(0xFFBA1A1A),
)

private val SharedHouseDarkColors = darkColorScheme(
    primary = Color(0xFF88C49F),
    onPrimary = Color(0xFF092015),
    primaryContainer = Color(0xFF1E3C2C),
    onPrimaryContainer = Color(0xFFE4F3EB),
    secondary = Color(0xFFE48B73),
    onSecondary = Color(0xFF3D1309),
    secondaryContainer = Color(0xFF4A251B),
    onSecondaryContainer = Color(0xFFFCEAE5),
    tertiary = Color(0xFFA5BFA0),
    onTertiary = Color(0xFF172816),
    tertiaryContainer = Color(0xFF263925),
    onTertiaryContainer = Color(0xFFE2EBE0),
    background = Color(0xFF101613),
    onBackground = Color(0xFFE3EDE6),
    surface = Color(0xFF18221D),
    onSurface = Color(0xFFE3EDE6),
    surfaceVariant = Color(0xFF223028),
    onSurfaceVariant = Color(0xFFAEBDB2),
    surfaceContainer = Color(0xFF1D2A24),
    surfaceContainerLow = Color(0xFF141E1A),
    outline = Color(0xFF384A3F),
    outlineVariant = Color(0xFF2B3A30),
    error = Color(0xFFFFB4AB),
)

private val SharedHouseHighContrastLightColors = lightColorScheme(
    primary = Color(0xFF003B23),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA9F0C4),
    onPrimaryContainer = Color(0xFF001F10),
    secondary = Color(0xFF702008),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBD1),
    onSecondaryContainer = Color(0xFF3B0900),
    background = Color(0xFFF7FAF6),
    onBackground = Color(0xFF0B1710),
    surface = Color.White,
    onSurface = Color(0xFF0B1710),
    surfaceVariant = Color(0xFFDBE5DC),
    onSurfaceVariant = Color(0xFF223427),
    outline = Color(0xFF45584B),
    error = Color(0xFF8C0009),
)

private val SharedHouseHighContrastDarkColors = darkColorScheme(
    primary = Color(0xFFA9F0C4),
    onPrimary = Color(0xFF002113),
    primaryContainer = Color(0xFF005232),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFFFB5A0),
    onSecondary = Color(0xFF471304),
    background = Color(0xFF0A110D),
    onBackground = Color.White,
    surface = Color(0xFF121B16),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1E2D23),
    onSurfaceVariant = Color(0xFFE0EFE4),
    outline = Color(0xFFB5C9BD),
    error = Color(0xFFFFB4AB),
)

@Composable
fun SharedHouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    highContrast: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        highContrast && darkTheme -> SharedHouseHighContrastDarkColors
        highContrast -> SharedHouseHighContrastLightColors
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && darkTheme -> {
            dynamicDarkColorScheme(context)
        }
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            dynamicLightColorScheme(context)
        }
        darkTheme -> SharedHouseDarkColors
        else -> SharedHouseLightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SharedHouseTypography,
        shapes = SharedHouseShapes,
        content = content,
    )
}
