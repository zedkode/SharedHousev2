package com.sharedhouse.android.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Nova uses disciplined geometry: compact controls, calm panels and only one generous radius for
 * signature content. This keeps finance, tasks and household management visibly connected without
 * the over-rounded look of the former interface.
 */
@Immutable
data class SharedHouseShapes(
    val extraSmall: Shape = RoundedCornerShape(8.dp),
    val small: Shape = RoundedCornerShape(12.dp),
    val medium: Shape = RoundedCornerShape(16.dp),
    val large: Shape = RoundedCornerShape(20.dp),
    val extraLarge: Shape = RoundedCornerShape(28.dp),
)

val AtmosphericShapes = SharedHouseShapes()
