package com.sharedhouse.android.ui.chat

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Small, screen-local extension of the SharedHouse rounded-line icon family. */
internal object ChatIcons {
    val Back: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Back") {
            moveTo(19.5f, 12.0f)
            horizontalLineTo(4.5f)
            moveTo(10.2f, 5.9f)
            lineTo(4.1f, 12.0f)
            lineTo(10.2f, 18.1f)
        }
    }

    val Send: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Send") {
            moveTo(3.2f, 4.1f)
            lineTo(20.9f, 12.0f)
            lineTo(3.2f, 19.9f)
            lineTo(6.25f, 12.0f)
            lineTo(3.2f, 4.1f)
            close()
            moveTo(6.25f, 12.0f)
            horizontalLineTo(14.4f)
        }
    }

    val Live: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Live") {
            dot(12.0f, 12.0f, 1.0f)
            moveTo(8.65f, 8.65f)
            curveTo(6.8f, 10.5f, 6.8f, 13.5f, 8.65f, 15.35f)
            moveTo(15.35f, 8.65f)
            curveTo(17.2f, 10.5f, 17.2f, 13.5f, 15.35f, 15.35f)
            moveTo(5.75f, 5.75f)
            curveTo(2.3f, 9.2f, 2.3f, 14.8f, 5.75f, 18.25f)
            moveTo(18.25f, 5.75f)
            curveTo(21.7f, 9.2f, 21.7f, 14.8f, 18.25f, 18.25f)
        }
    }

    val Offline: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Offline") {
            moveTo(6.2f, 17.9f)
            horizontalLineTo(17.5f)
            curveTo(19.7f, 17.9f, 21.25f, 16.4f, 21.25f, 14.35f)
            curveTo(21.25f, 12.4f, 19.85f, 10.95f, 17.95f, 10.8f)
            curveTo(17.4f, 7.65f, 14.9f, 5.55f, 11.75f, 5.55f)
            curveTo(9.8f, 5.55f, 8.1f, 6.35f, 6.95f, 7.75f)
            curveTo(4.55f, 7.9f, 2.75f, 9.9f, 2.75f, 12.35f)
            curveTo(2.75f, 13.55f, 3.2f, 14.65f, 3.95f, 15.45f)
            moveTo(4.0f, 4.0f)
            lineTo(20.0f, 20.0f)
        }
    }

    val Retry: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Retry") {
            moveTo(7.25f, 7.25f)
            horizontalLineTo(3.6f)
            verticalLineTo(3.6f)
            moveTo(4.0f, 7.0f)
            curveTo(5.8f, 4.65f, 8.55f, 3.35f, 11.55f, 3.45f)
            curveTo(16.15f, 3.6f, 19.9f, 7.25f, 20.25f, 11.8f)
            curveTo(20.6f, 16.35f, 17.4f, 20.25f, 12.9f, 20.6f)
            curveTo(9.15f, 20.9f, 5.75f, 18.9f, 4.25f, 15.55f)
        }
    }

    val Lock: ImageVector by lazy {
        chatIcon("SharedHouse.Chat.Lock") {
            roundedRect(4.65f, 10.1f, 19.35f, 20.4f, 2.35f)
            moveTo(8.0f, 10.1f)
            verticalLineTo(7.65f)
            curveTo(8.0f, 5.25f, 9.65f, 3.6f, 12.0f, 3.6f)
            curveTo(14.35f, 3.6f, 16.0f, 5.25f, 16.0f, 7.65f)
            verticalLineTo(10.1f)
            dot(12.0f, 15.25f, 0.75f)
        }
    }
}

private val ChatIconStroke = SolidColor(Color.Black)

private fun chatIcon(
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
        stroke = ChatIconStroke,
        strokeLineWidth = 1.8f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
        pathBuilder = pathBuilder,
    )
}.build()

private fun PathBuilder.dot(
    centerX: Float,
    centerY: Float,
    radius: Float,
) {
    val control = radius * 0.5522848f
    moveTo(centerX + radius, centerY)
    curveTo(centerX + radius, centerY + control, centerX + control, centerY + radius, centerX, centerY + radius)
    curveTo(centerX - control, centerY + radius, centerX - radius, centerY + control, centerX - radius, centerY)
    curveTo(centerX - radius, centerY - control, centerX - control, centerY - radius, centerX, centerY - radius)
    curveTo(centerX + control, centerY - radius, centerX + radius, centerY - control, centerX + radius, centerY)
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
