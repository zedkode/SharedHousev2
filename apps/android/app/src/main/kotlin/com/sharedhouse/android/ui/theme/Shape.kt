package com.sharedhouse.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Cupertino-inspired geometry: gently rounded panels, compact controls and a single large radius
 * for feature cards. The values preserve comfortable Android touch targets while keeping the visual
 * rhythm calm and content-led.
 */
@Immutable
data class SharedHouseShapes(
    val extraSmall: Shape = RoundedCornerShape(8.dp),
    val small: Shape = RoundedCornerShape(10.dp),
    val medium: Shape = RoundedCornerShape(14.dp),
    val large: Shape = RoundedCornerShape(18.dp),
    val extraLarge: Shape = RoundedCornerShape(24.dp),
)

val AtmosphericShapes = SharedHouseShapes()
