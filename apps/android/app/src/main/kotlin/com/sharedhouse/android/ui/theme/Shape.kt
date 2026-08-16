package com.sharedhouse.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Horizon shapes favour a friendly architectural rhythm: compact controls, generous cards and a
 * recognisable asymmetric-feeling large surface without sacrificing touch targets.
 */
@Immutable
data class SharedHouseShapes(
    val extraSmall: Shape = RoundedCornerShape(10.dp),
    val small: Shape = RoundedCornerShape(14.dp),
    val medium: Shape = RoundedCornerShape(18.dp),
    val large: Shape = RoundedCornerShape(26.dp),
    val extraLarge: Shape = RoundedCornerShape(32.dp),
)

val AtmosphericShapes = SharedHouseShapes()
