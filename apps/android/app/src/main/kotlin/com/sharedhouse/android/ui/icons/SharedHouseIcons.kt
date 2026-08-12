package com.sharedhouse.android.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * SharedHouse's monochrome icon family.
 *
 * Every glyph uses the same 24 x 24 grid and rounded 1.8 dp line treatment. The
 * paths intentionally carry no semantic colour so callers can provide contrast
 * through SharedHouse's own icon primitive tint.
 */
object SharedHouseIcons {
    val Home: ImageVector by lazy {
        roundedIcon("SharedHouse.Home") {
            moveTo(3.4f, 10.7f)
            lineTo(11.2f, 4.45f)
            curveTo(11.65f, 4.08f, 12.35f, 4.08f, 12.8f, 4.45f)
            lineTo(20.6f, 10.7f)

            moveTo(5.35f, 9.3f)
            verticalLineTo(18.2f)
            curveTo(5.35f, 19.25f, 6.1f, 20.0f, 7.15f, 20.0f)
            horizontalLineTo(16.85f)
            curveTo(17.9f, 20.0f, 18.65f, 19.25f, 18.65f, 18.2f)
            verticalLineTo(9.3f)

            moveTo(9.55f, 20.0f)
            verticalLineTo(15.35f)
            curveTo(9.55f, 14.55f, 10.15f, 13.95f, 10.95f, 13.95f)
            horizontalLineTo(13.05f)
            curveTo(13.85f, 13.95f, 14.45f, 14.55f, 14.45f, 15.35f)
            verticalLineTo(20.0f)
        }
    }

    val Calendar: ImageVector by lazy {
        roundedIcon("SharedHouse.Calendar") {
            roundedRect(3.25f, 5.25f, 20.75f, 20.25f, 2.25f)

            moveTo(3.25f, 9.35f)
            horizontalLineTo(20.75f)
            moveTo(7.25f, 3.75f)
            verticalLineTo(6.75f)
            moveTo(16.75f, 3.75f)
            verticalLineTo(6.75f)

            circle(8.0f, 13.15f, 0.65f)
            circle(12.0f, 13.15f, 0.65f)
            circle(16.0f, 13.15f, 0.65f)
            moveTo(7.4f, 17.0f)
            horizontalLineTo(13.2f)
        }
    }

    val Money: ImageVector by lazy {
        roundedIcon("SharedHouse.Money") {
            roundedRect(3.0f, 6.4f, 21.0f, 19.35f, 2.35f)

            moveTo(5.1f, 6.4f)
            verticalLineTo(5.9f)
            curveTo(5.1f, 4.7f, 6.0f, 3.8f, 7.2f, 3.8f)
            horizontalLineTo(16.9f)
            curveTo(18.0f, 3.8f, 18.85f, 4.6f, 18.85f, 5.7f)

            moveTo(20.95f, 10.2f)
            horizontalLineTo(15.75f)
            curveTo(14.25f, 10.2f, 13.2f, 11.25f, 13.2f, 12.75f)
            curveTo(13.2f, 14.25f, 14.25f, 15.3f, 15.75f, 15.3f)
            horizontalLineTo(20.95f)
            circle(16.4f, 12.75f, 0.55f)
        }
    }

    val Tasks: ImageVector by lazy {
        roundedIcon("SharedHouse.Tasks") {
            roundedRect(5.0f, 5.4f, 19.0f, 20.75f, 2.1f)
            roundedRect(8.25f, 3.0f, 15.75f, 7.0f, 1.45f)

            moveTo(8.0f, 11.7f)
            lineTo(9.35f, 13.0f)
            lineTo(11.45f, 10.55f)
            moveTo(13.25f, 12.0f)
            horizontalLineTo(16.2f)

            moveTo(8.0f, 16.85f)
            lineTo(9.35f, 18.15f)
            lineTo(11.45f, 15.7f)
            moveTo(13.25f, 17.15f)
            horizontalLineTo(16.2f)
        }
    }

    val House: ImageVector by lazy {
        roundedIcon("SharedHouse.House") {
            moveTo(3.35f, 10.4f)
            lineTo(11.2f, 4.2f)
            curveTo(11.65f, 3.85f, 12.35f, 3.85f, 12.8f, 4.2f)
            lineTo(20.65f, 10.4f)

            moveTo(5.25f, 9.05f)
            verticalLineTo(19.95f)
            horizontalLineTo(18.75f)
            verticalLineTo(9.05f)

            circle(12.0f, 12.0f, 1.35f)
            circle(8.55f, 13.0f, 0.85f)
            circle(15.45f, 13.0f, 0.85f)

            moveTo(8.45f, 18.15f)
            curveTo(8.75f, 16.1f, 10.0f, 15.05f, 12.0f, 15.05f)
            curveTo(14.0f, 15.05f, 15.25f, 16.1f, 15.55f, 18.15f)
            moveTo(6.7f, 17.8f)
            curveTo(6.95f, 16.45f, 7.65f, 15.75f, 8.8f, 15.65f)
            moveTo(17.3f, 17.8f)
            curveTo(17.05f, 16.45f, 16.35f, 15.75f, 15.2f, 15.65f)
        }
    }

    val Chat: ImageVector by lazy {
        roundedIcon("SharedHouse.Chat") {
            roundedRect(3.0f, 4.0f, 20.25f, 16.55f, 2.7f)

            moveTo(8.1f, 16.55f)
            lineTo(5.15f, 20.25f)
            lineTo(5.55f, 16.35f)

            circle(8.0f, 10.25f, 0.7f)
            circle(11.65f, 10.25f, 0.7f)
            circle(15.3f, 10.25f, 0.7f)
        }
    }

    val Rent: ImageVector by lazy {
        roundedIcon("SharedHouse.Rent") {
            roundedRect(5.15f, 3.0f, 18.85f, 21.0f, 2.0f)

            moveTo(8.1f, 9.2f)
            lineTo(11.55f, 6.45f)
            curveTo(11.8f, 6.25f, 12.2f, 6.25f, 12.45f, 6.45f)
            lineTo(15.9f, 9.2f)
            moveTo(9.0f, 8.55f)
            verticalLineTo(12.55f)
            horizontalLineTo(15.0f)
            verticalLineTo(8.55f)
            moveTo(11.05f, 12.55f)
            verticalLineTo(10.25f)
            horizontalLineTo(12.95f)
            verticalLineTo(12.55f)

            moveTo(8.2f, 16.2f)
            horizontalLineTo(15.8f)
            moveTo(8.2f, 18.5f)
            horizontalLineTo(12.8f)
        }
    }

    val Maintenance: ImageVector by lazy {
        roundedIcon("SharedHouse.Maintenance") {
            moveTo(13.65f, 7.25f)
            curveTo(14.45f, 4.75f, 17.25f, 3.3f, 19.75f, 4.3f)
            lineTo(17.05f, 7.0f)
            lineTo(17.5f, 8.5f)
            lineTo(19.0f, 8.95f)
            lineTo(21.7f, 6.25f)
            curveTo(22.7f, 8.75f, 21.25f, 11.55f, 18.75f, 12.35f)
            lineTo(10.05f, 21.05f)
            curveTo(9.05f, 22.05f, 7.45f, 22.05f, 6.45f, 21.05f)
            curveTo(5.45f, 20.05f, 5.45f, 18.45f, 6.45f, 17.45f)
            lineTo(15.15f, 8.75f)

            moveTo(4.0f, 4.0f)
            lineTo(8.45f, 5.05f)
            lineTo(10.1f, 8.05f)
            lineTo(8.0f, 10.15f)
            lineTo(5.0f, 8.5f)
            close()
            moveTo(9.0f, 9.15f)
            lineTo(12.0f, 12.15f)

            circle(8.25f, 19.25f, 0.65f)
        }
    }

    val Utilities: ImageVector by lazy {
        roundedIcon("SharedHouse.Utilities") {
            roundedRect(3.25f, 3.25f, 20.75f, 20.75f, 3.0f)

            moveTo(6.7f, 15.65f)
            curveTo(7.25f, 11.25f, 9.0f, 9.1f, 12.0f, 9.1f)
            curveTo(15.0f, 9.1f, 16.75f, 11.25f, 17.3f, 15.65f)
            moveTo(12.0f, 13.45f)
            lineTo(15.1f, 10.6f)
            circle(12.0f, 13.45f, 0.7f)

            moveTo(7.0f, 17.85f)
            horizontalLineTo(17.0f)
            moveTo(8.0f, 6.25f)
            horizontalLineTo(9.1f)
            moveTo(11.45f, 6.25f)
            horizontalLineTo(12.55f)
            moveTo(15.0f, 6.25f)
            horizontalLineTo(16.1f)
        }
    }

    val Cleaning: ImageVector by lazy {
        roundedIcon("SharedHouse.Cleaning") {
            moveTo(16.8f, 3.1f)
            lineTo(10.55f, 12.6f)

            moveTo(8.4f, 11.35f)
            lineTo(12.65f, 14.15f)
            lineTo(10.65f, 20.75f)
            curveTo(8.45f, 20.55f, 6.55f, 19.3f, 5.25f, 17.25f)
            lineTo(8.4f, 11.35f)
            close()
            moveTo(6.7f, 14.55f)
            curveTo(8.2f, 16.05f, 9.9f, 17.05f, 11.55f, 17.35f)

            moveTo(18.4f, 6.45f)
            verticalLineTo(9.45f)
            moveTo(16.9f, 7.95f)
            horizontalLineTo(19.9f)

            moveTo(18.25f, 13.25f)
            verticalLineTo(15.25f)
            moveTo(17.25f, 14.25f)
            horizontalLineTo(19.25f)
        }
    }

    val Approved: ImageVector by lazy {
        roundedIcon("SharedHouse.Approved") {
            circle(12.0f, 12.0f, 8.35f)
            moveTo(7.8f, 12.15f)
            lineTo(10.6f, 14.95f)
            lineTo(16.45f, 8.85f)
        }
    }

    val Pending: ImageVector by lazy {
        roundedIcon("SharedHouse.Pending") {
            circle(12.0f, 12.0f, 8.35f)
            moveTo(12.0f, 7.15f)
            verticalLineTo(12.1f)
            lineTo(15.45f, 14.15f)
        }
    }

    val Reversed: ImageVector by lazy {
        roundedIcon("SharedHouse.Reversed") {
            moveTo(3.65f, 3.7f)
            verticalLineTo(7.55f)
            horizontalLineTo(7.5f)

            moveTo(4.05f, 7.3f)
            curveTo(5.8f, 4.85f, 8.45f, 3.5f, 11.5f, 3.55f)
            curveTo(16.2f, 3.65f, 20.05f, 7.4f, 20.4f, 12.05f)
            curveTo(20.75f, 16.7f, 17.45f, 20.65f, 12.85f, 20.95f)
            curveTo(9.05f, 21.2f, 5.65f, 19.15f, 4.15f, 15.75f)
        }
    }

    val People: ImageVector by lazy {
        roundedIcon("SharedHouse.People") {
            circle(12.0f, 7.2f, 2.05f)
            circle(6.85f, 9.1f, 1.45f)
            circle(17.15f, 9.1f, 1.45f)

            moveTo(7.65f, 19.65f)
            curveTo(7.95f, 15.65f, 9.45f, 13.65f, 12.0f, 13.65f)
            curveTo(14.55f, 13.65f, 16.05f, 15.65f, 16.35f, 19.65f)
            moveTo(3.25f, 18.25f)
            curveTo(3.55f, 15.25f, 4.75f, 13.75f, 6.75f, 13.75f)
            curveTo(7.45f, 13.75f, 8.05f, 13.95f, 8.55f, 14.3f)
            moveTo(20.75f, 18.25f)
            curveTo(20.45f, 15.25f, 19.25f, 13.75f, 17.25f, 13.75f)
            curveTo(16.55f, 13.75f, 15.95f, 13.95f, 15.45f, 14.3f)
        }
    }

    val Add: ImageVector by lazy {
        roundedIcon("SharedHouse.Add") {
            moveTo(12.0f, 4.5f)
            verticalLineTo(19.5f)
            moveTo(4.5f, 12.0f)
            horizontalLineTo(19.5f)
        }
    }

    val More: ImageVector by lazy {
        roundedIcon("SharedHouse.More") {
            circle(12.0f, 5.8f, 0.7f)
            circle(12.0f, 12.0f, 0.7f)
            circle(12.0f, 18.2f, 0.7f)
        }
    }
}

private val IconStroke = SolidColor(Color.Black)

private fun roundedIcon(
    name: String,
    pathBuilder: PathBuilder.() -> Unit,
): ImageVector = ImageVector.Builder(
    name = name,
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24.0f,
    viewportHeight = 24.0f,
).apply {
    path(
        fill = null,
        stroke = IconStroke,
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}.build()

private fun PathBuilder.circle(
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    val control = radius * 0.5522848f
    moveTo(centerX + radius, centerY)
    curveTo(
        centerX + radius,
        centerY + control,
        centerX + control,
        centerY + radius,
        centerX,
        centerY + radius,
    )
    curveTo(
        centerX - control,
        centerY + radius,
        centerX - radius,
        centerY + control,
        centerX - radius,
        centerY,
    )
    curveTo(
        centerX - radius,
        centerY - control,
        centerX - control,
        centerY - radius,
        centerX,
        centerY - radius,
    )
    curveTo(
        centerX + control,
        centerY - radius,
        centerX + radius,
        centerY - control,
        centerX + radius,
        centerY,
    )
    close()
}

private fun PathBuilder.roundedRect(
    left: Float,
    top: Float,
    right: Float,
    bottom: Float,
    radius: Float,
) {
    val control = radius * 0.5522848f
    moveTo(left + radius, top)
    horizontalLineTo(right - radius)
    curveTo(right - radius + control, top, right, top + radius - control, right, top + radius)
    verticalLineTo(bottom - radius)
    curveTo(right, bottom - radius + control, right - radius + control, bottom, right - radius, bottom)
    horizontalLineTo(left + radius)
    curveTo(left + radius - control, bottom, left, bottom - radius + control, left, bottom - radius)
    verticalLineTo(top + radius)
    curveTo(left, top + radius - control, left + radius - control, top, left + radius, top)
    close()
}
