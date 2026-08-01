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
    primary = Color(0xFF3E654F),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EBDD),
    onPrimaryContainer = Color(0xFF17231D),
    background = Color(0xFFF2F5F2),
    onBackground = Color(0xFF17231D),
    surface = Color.White,
    onSurface = Color(0xFF17231D),
    surfaceVariant = Color(0xFFE7ECE8),
    onSurfaceVariant = Color(0xFF435248),
    outline = Color(0xFFC8D3CA),
    secondary = Color(0xFF435248),
    onSecondary = Color.White,
    error = Color(0xFFBA1A1A),
)

private val SharedHouseDarkColors = darkColorScheme(
    primary = Color(0xFF9AC9AC),
    onPrimary = Color(0xFF102118),
    primaryContainer = Color(0xFF294C38),
    onPrimaryContainer = Color(0xFFECF4EE),
    background = Color(0xFF111914),
    onBackground = Color(0xFFECF4EE),
    surface = Color(0xFF19241D),
    onSurface = Color(0xFFECF4EE),
    surfaceVariant = Color(0xFF243129),
    onSurfaceVariant = Color(0xFFB4C6BA),
    outline = Color(0xFF35453B),
    secondary = Color(0xFFB4C6BA),
    onSecondary = Color(0xFF111914),
    error = Color(0xFFFFB4AB),
)

private val SharedHouseHighContrastLightColors = lightColorScheme(
    primary = Color(0xFF003D24),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB7F5CC),
    onPrimaryContainer = Color(0xFF001E10),
    secondary = Color(0xFF20382A),
    onSecondary = Color.White,
    background = Color(0xFFFCFDF8),
    onBackground = Color(0xFF0B1710),
    surface = Color.White,
    onSurface = Color(0xFF0B1710),
    surfaceVariant = Color(0xFFE3EBE4),
    onSurfaceVariant = Color(0xFF263C2E),
    outline = Color(0xFF4B5E51),
    error = Color(0xFF8C0009),
)

private val SharedHouseHighContrastDarkColors = darkColorScheme(
    primary = Color(0xFFB7F5CC),
    onPrimary = Color(0xFF002114),
    primaryContainer = Color(0xFF00653D),
    onPrimaryContainer = Color.White,
    secondary = Color(0xFFD3E8D8),
    onSecondary = Color(0xFF0D2216),
    background = Color(0xFF08100B),
    onBackground = Color.White,
    surface = Color(0xFF101A14),
    onSurface = Color.White,
    surfaceVariant = Color(0xFF1B2A20),
    onSurfaceVariant = Color(0xFFE2F2E6),
    outline = Color(0xFFB9CCBF),
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
