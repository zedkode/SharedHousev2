package com.sharedhouse.android.ui.startup

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sharedhouse.android.R
import com.sharedhouse.android.ui.atmosphere.Text
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.theme.PremiumPalette
import kotlin.math.cos
import kotlin.math.sin

enum class StartupCopyKind {
    RESOLVING_SESSION,
    AUTHENTICATED,
    GUEST,
}

internal fun resolveStartupCopyKind(
    isRestoringSession: Boolean,
    displayName: String?,
): StartupCopyKind = when {
    isRestoringSession -> StartupCopyKind.RESOLVING_SESSION
    !displayName.isNullOrBlank() -> StartupCopyKind.AUTHENTICATED
    else -> StartupCopyKind.GUEST
}

@Composable
fun SharedHouseStartupScreen(
    copyKind: StartupCopyKind,
    modifier: Modifier = Modifier,
    displayName: String? = null,
) {
    val motionEnabled = AtmosphereTheme.motionEnabled
    val infiniteTransition = rememberInfiniteTransition(label = "sharedhouse-startup")
    val logoScale = if (motionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = .97f,
            targetValue = 1.035f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_500),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "startup-logo-scale",
        ).value
    } else {
        1f
    }
    val glowAlpha = if (motionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = .18f,
            targetValue = .34f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1_500),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "startup-glow-alpha",
        ).value
    } else {
        .24f
    }
    val rotation = if (motionEnabled) {
        infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2_200, easing = LinearEasing),
            ),
            label = "startup-progress-rotation",
        ).value
    } else {
        0f
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.radialGradient(
                    colors = listOf(
                        PremiumPalette.HomeGlow,
                        PremiumPalette.Base,
                    ),
                    radius = 1_350f,
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 420.dp)
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(250.dp)
                        .alpha(glowAlpha),
                ) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PremiumPalette.HeroMiddle.copy(alpha = .8f),
                                PremiumPalette.HeroEnd.copy(alpha = .18f),
                                Color.Transparent,
                            ),
                            center = center,
                            radius = size.minDimension / 2f,
                        ),
                    )
                }
                Image(
                    painter = painterResource(R.drawable.sharedhouse_logo_master),
                    contentDescription = stringResource(R.string.startup_logo_description),
                    modifier = Modifier
                        .size(218.dp)
                        .scale(logoScale),
                )
            }

            Spacer(Modifier.height(18.dp))

            StartupCopy(
                copyKind = copyKind,
                displayName = displayName,
                motionEnabled = motionEnabled,
            )

            Spacer(Modifier.height(28.dp))

            StartupProgress(
                rotation = rotation,
                motionEnabled = motionEnabled,
            )
        }
    }
}

@Composable
private fun StartupCopy(
    copyKind: StartupCopyKind,
    displayName: String?,
    motionEnabled: Boolean,
) {
    AnimatedContent(
        targetState = copyKind,
        transitionSpec = {
            if (motionEnabled) {
                fadeIn(tween(320)).togetherWith(fadeOut(tween(220)))
            } else {
                fadeIn(tween(0)).togetherWith(fadeOut(tween(0)))
            }
        },
        label = "startup-copy",
    ) { currentKind ->
        val title = when (currentKind) {
            StartupCopyKind.RESOLVING_SESSION -> stringResource(R.string.startup_preparing_title)
            StartupCopyKind.AUTHENTICATED -> stringResource(
                R.string.startup_welcome_back,
                displayName.orEmpty().trim(),
            )
            StartupCopyKind.GUEST -> stringResource(R.string.startup_welcome_guest)
        }
        val supporting = stringResource(currentKind.supportingText)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                color = PremiumPalette.TextOnGradient,
                style = AtmosphereTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = supporting,
                color = PremiumPalette.TextOnGradientSecondary,
                style = AtmosphereTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun StartupProgress(
    rotation: Float,
    motionEnabled: Boolean,
) {
    val loadingDescription = stringResource(R.string.startup_loading_description)
    Canvas(
        modifier = Modifier
            .size(48.dp)
            .graphicsLayer { rotationZ = rotation }
            .semantics {
                contentDescription = loadingDescription
                progressBarRangeInfo = ProgressBarRangeInfo.Indeterminate
            },
    ) {
        val orbitRadius = size.minDimension * .32f
        val dotRadius = size.minDimension * .065f
        val centerPoint = center
        val colors = listOf(
            PremiumPalette.HeroStart,
            PremiumPalette.HeroMiddle,
            PremiumPalette.HeroEnd,
        )
        colors.forEachIndexed { index, color ->
            val angle = Math.toRadians((index * 120.0) - 90.0)
            drawCircle(
                color = color.copy(alpha = if (motionEnabled) 1f else .82f),
                radius = dotRadius,
                center = androidx.compose.ui.geometry.Offset(
                    x = centerPoint.x + (cos(angle) * orbitRadius).toFloat(),
                    y = centerPoint.y + (sin(angle) * orbitRadius).toFloat(),
                ),
            )
        }
    }
}

private val StartupCopyKind.supportingText: Int
    @StringRes get() = when (this) {
        StartupCopyKind.RESOLVING_SESSION -> R.string.startup_preparing_supporting
        StartupCopyKind.AUTHENTICATED -> R.string.startup_authenticated_supporting
        StartupCopyKind.GUEST -> R.string.startup_guest_supporting
    }
