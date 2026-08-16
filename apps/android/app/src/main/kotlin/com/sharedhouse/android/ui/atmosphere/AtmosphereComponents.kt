package com.sharedhouse.android.ui.atmosphere

import android.widget.DatePicker as AndroidDatePicker
import android.widget.TimePicker as AndroidTimePicker
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.sharedhouse.android.ui.theme.AtmosphereTheme
import com.sharedhouse.android.ui.theme.PremiumPalette
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

private val LocalAtmosphereContentColor = compositionLocalOf { Color.Unspecified }
private val LocalAtmosphereFontWeight = compositionLocalOf<FontWeight?> { null }

/**
 * SharedHouse's custom Compose Foundation primitives. They deliberately own the visual behaviour
 * that was previously inherited from a third-party component theme, so screens cannot silently
 * drift away from the product design system.
 */

@Composable
fun Text(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    textDecoration: TextDecoration? = null,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    style: TextStyle = AtmosphereTheme.typography.bodyMedium,
) {
    val resolved = style.merge(
        color = if (color == Color.Unspecified) {
            LocalAtmosphereContentColor.current.takeIf { it != Color.Unspecified }
                ?: AtmosphereTheme.colorScheme.onSurface
        } else color,
        fontSize = fontSize,
        fontWeight = fontWeight ?: LocalAtmosphereFontWeight.current,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textDecoration = textDecoration,
        textAlign = textAlign ?: TextAlign.Unspecified,
        lineHeight = lineHeight,
    )
    BasicText(text, modifier, resolved, overflow = overflow, softWrap = softWrap, maxLines = maxLines, minLines = minLines)
}

@Composable
fun Text(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = AtmosphereTheme.typography.bodyMedium,
) = BasicText(
    text,
    modifier,
    style.merge(
        color = if (color == Color.Unspecified) {
            LocalAtmosphereContentColor.current.takeIf { it != Color.Unspecified }
                ?: AtmosphereTheme.colorScheme.onSurface
        } else color,
        fontWeight = LocalAtmosphereFontWeight.current,
    ),
)

@Composable
fun Icon(
    imageVector: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = Color.Unspecified,
) {
    val resolvedTint = if (tint == Color.Unspecified) {
        LocalAtmosphereContentColor.current.takeIf { it != Color.Unspecified }
            ?: AtmosphereTheme.colorScheme.onSurface
    } else tint
    androidx.compose.foundation.Image(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(resolvedTint),
        contentScale = ContentScale.Fit,
    )
}

private fun Modifier.atmosphericSurface(
    shape: Shape,
    color: Color,
    border: BorderStroke?,
    shadow: Dp,
): Modifier = this
    .then(
        if (shadow > 0.dp) {
            Modifier.shadow(
                elevation = shadow + 8.dp,
                shape = shape,
                clip = false,
                ambientColor = PremiumPalette.HomeGlow.copy(alpha = .30f),
                spotColor = PremiumPalette.HeroMiddle.copy(alpha = .16f),
            )
        } else Modifier
    )
    .then(
        if (shadow > 0.dp) {
            Modifier.shadow(
                elevation = 3.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = .22f),
                spotColor = Color.Black.copy(alpha = .22f),
            )
        } else Modifier
    )
    .clip(shape)
    .drawWithCache {
        val body = Brush.verticalGradient(
            listOf(
                lerp(color, Color.White, if (color.alpha > .02f) .035f else 0f),
                lerp(color, Color.Black, if (color.alpha > .02f) .035f else 0f),
            ),
        )
        val glass = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = .08f), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(size.width * .18f, 0f),
            radius = size.maxDimension * .72f,
        )
        onDrawBehind {
            drawRect(body)
            if (color.alpha > .02f) drawRect(glass)
            drawLine(
                color = Color.White.copy(alpha = if (color.alpha > .02f) .15f else .07f),
                start = androidx.compose.ui.geometry.Offset(size.width * .14f, 1.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width * .54f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
    .then(if (border != null) Modifier.border(border, shape) else Modifier)

private fun Modifier.premiumGradientSurface(
    shape: Shape,
    brush: Brush,
    border: BorderStroke? = null,
    shadow: Dp = 0.dp,
    shadowAlpha: Float = .35f,
): Modifier = this
    .then(
        if (shadow > 0.dp) {
            Modifier.shadow(
                elevation = shadow + 12.dp,
                shape = shape,
                clip = false,
                ambientColor = PremiumPalette.HeroStart.copy(alpha = shadowAlpha),
                spotColor = PremiumPalette.HeroStart.copy(alpha = shadowAlpha),
            )
        } else Modifier,
    )
    .then(
        if (shadow > 0.dp) {
            Modifier.shadow(
                elevation = 4.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = .22f),
                spotColor = Color.Black.copy(alpha = .22f),
            )
        } else Modifier,
    )
    .clip(shape)
    .background(brush)
    .drawWithCache {
        val glass = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = .18f), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(size.width * .08f, 0f),
            radius = size.maxDimension * .72f,
        )
        onDrawBehind {
            drawRect(glass)
            drawLine(
                color = Color.White.copy(alpha = .30f),
                start = androidx.compose.ui.geometry.Offset(size.width * .10f, 1.dp.toPx()),
                end = androidx.compose.ui.geometry.Offset(size.width * .64f, 1.dp.toPx()),
                strokeWidth = 1.dp.toPx(),
            )
        }
    }
    .then(if (border != null) Modifier.border(border, shape) else Modifier)

@Composable
fun Surface(
    modifier: Modifier = Modifier,
    shape: Shape = AtmosphereTheme.shapes.medium,
    color: Color = AtmosphereTheme.colorScheme.surface,
    contentColor: Color = AtmosphereTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val noTonalElevation = tonalElevation
    CompositionLocalProvider(LocalAtmosphereContentColor provides contentColor) {
        Box(modifier.atmosphericSurface(shape, color, border, shadowElevation)) { content() }
    }
}

@Composable
fun Surface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AtmosphereTheme.shapes.medium,
    color: Color = AtmosphereTheme.colorScheme.surface,
    contentColor: Color = AtmosphereTheme.colorScheme.onSurface,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    border: BorderStroke? = null,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .985f else 1f,
        animationSpec = if (AtmosphereTheme.motionEnabled) tween(120) else snap(),
        label = "surface press scale",
    )
    Surface(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        color = color,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = if (pressed) 2.dp else shadowElevation,
        border = border,
        content = content,
    )
}

/**
 * Horizon Home background: a deep evergreen base with quiet teal and amber light pools.
 * Other screens retain the same tonal family without competing with their data.
 */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val dark = AtmosphereTheme.colorScheme.background == PremiumPalette.Base
    val base = AtmosphereTheme.colorScheme.background
    val glow = if (dark) PremiumPalette.HomeGlow else AtmosphereTheme.colorScheme.primary.copy(alpha = .10f)
    Box(
        modifier = modifier
            .background(base)
            .drawWithCache {
                val radius = size.maxDimension * .92f
                val topWash = Brush.radialGradient(
                    colors = listOf(glow.copy(alpha = if (dark) .45f else .10f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * .16f, size.height * .02f),
                    radius = radius,
                )
                val lowerWash = Brush.radialGradient(
                    colors = listOf(PremiumPalette.AmbientPink.copy(alpha = if (dark) .08f else .05f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .72f),
                    radius = size.maxDimension * .72f,
                )
                onDrawBehind {
                    drawRect(topWash)
                    drawRect(lowerWash)
                }
            },
        content = content,
    )
}

/** One-per-screen Horizon hero: a calm teal-to-sky gradient for the one primary outcome. */
@Composable
fun PremiumHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    val clickable = if (onClick == null) {
        Modifier
    } else {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    }
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides PremiumPalette.TextOnGradient,
    ) {
        Box(
            modifier = modifier
                .premiumGradientSurface(
                    shape = shape,
                    brush = Brush.linearGradient(
                        colorStops = arrayOf(
                            0f to PremiumPalette.HeroStart,
                            .55f to PremiumPalette.HeroMiddle,
                            1f to PremiumPalette.HeroEnd,
                        ),
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .08f)),
                    shadow = 18.dp,
                    shadowAlpha = .28f,
                )
                .then(clickable)
        ) {
            Canvas(Modifier.matchParentSize()) {
                drawCircle(
                    color = Color.White.copy(alpha = .07f),
                    radius = size.minDimension * .52f,
                    center = androidx.compose.ui.geometry.Offset(size.width * 1.02f, size.height * .10f),
                )
                drawCircle(
                    color = PremiumPalette.AccentSecondary.copy(alpha = .13f),
                    radius = size.minDimension * .30f,
                    center = androidx.compose.ui.geometry.Offset(size.width * .86f, size.height * .88f),
                )
            }
            Column(
                modifier = Modifier.padding(22.dp),
                content = content,
            )
        }
    }
}

/** A physical, independently-lit icon badge for cards and empty states. */
@Composable
fun DepthIconBadge(
    icon: ImageVector,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    tint: Color = if (hero) Color.White else AtmosphereTheme.colorScheme.primary,
    badgeSize: Dp = 44.dp,
    iconSize: Dp = 22.dp,
) {
    val shape = CircleShape
    val base = if (hero) Color.White.copy(alpha = .18f) else AtmosphereTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .size(badgeSize)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = .24f),
                spotColor = PremiumPalette.HeroMiddle.copy(alpha = .20f),
            )
            .clip(shape)
            .drawWithCache {
                val body = Brush.radialGradient(
                    colors = listOf(
                        lerp(base, Color.White, if (hero) .22f else .10f),
                        lerp(base, Color.Black, .14f),
                    ),
                    center = androidx.compose.ui.geometry.Offset(size.width * .42f, size.height * .28f),
                    radius = size.maxDimension * .68f,
                )
                val shine = Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = .24f), Color.Transparent),
                    endY = size.height * .55f,
                )
                onDrawBehind {
                    drawCircle(body)
                    drawCircle(shine)
                }
            }
            .border(1.dp, Color.White.copy(alpha = if (hero) .24f else .10f), shape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription, Modifier.size(iconSize), tint = tint)
    }
}

@Immutable data class CardColors(val containerColor: Color, val contentColor: Color)
@Immutable data class CardElevation(val defaultElevation: Dp, val pressedElevation: Dp)

object CardDefaults {
    @Composable fun cardColors(
        containerColor: Color = AtmosphereTheme.colorScheme.cardLevel1,
        contentColor: Color = AtmosphereTheme.colorScheme.onSurface,
        disabledContainerColor: Color = containerColor.copy(alpha = .55f),
        disabledContentColor: Color = contentColor.copy(alpha = .55f),
    ) = CardColors(containerColor, contentColor)

    @Composable fun levelTwoCardColors(
        containerColor: Color = AtmosphereTheme.colorScheme.cardLevel2,
        contentColor: Color = AtmosphereTheme.colorScheme.onSurface,
    ) = CardColors(containerColor, contentColor)

    @Composable fun cardBorder(containerColor: Color): BorderStroke = if (
        containerColor == AtmosphereTheme.colorScheme.cardLevel2 ||
        containerColor == AtmosphereTheme.colorScheme.surfaceContainerHigh ||
        containerColor == AtmosphereTheme.colorScheme.surfaceVariant
    ) {
        BorderStroke(
            1.dp,
            Brush.linearGradient(
                listOf(PremiumPalette.ActiveBorder, PremiumPalette.Border),
            ),
        )
    } else {
        BorderStroke(1.dp, AtmosphereTheme.colorScheme.outlineVariant)
    }

    fun cardElevation(defaultElevation: Dp = 0.dp, pressedElevation: Dp = defaultElevation + 2.dp) =
        CardElevation(defaultElevation, pressedElevation)
}

@Composable
fun Card(
    modifier: Modifier = Modifier,
    shape: Shape = AtmosphereTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = CardDefaults.cardBorder(colors.containerColor),
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedElevation = when {
        elevation.defaultElevation > 0.dp -> elevation.defaultElevation
        colors.containerColor == AtmosphereTheme.colorScheme.cardLevel2 -> 8.dp
        else -> 3.dp
    }
    Surface(modifier, shape, colors.containerColor, colors.contentColor, shadowElevation = resolvedElevation, border = border) {
        Column(content = content)
    }
}

@Composable
fun Card(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = AtmosphereTheme.shapes.large,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = CardDefaults.cardBorder(colors.containerColor),
    content: @Composable ColumnScope.() -> Unit,
) {
    val resolvedElevation = when {
        elevation.defaultElevation > 0.dp -> elevation.defaultElevation
        colors.containerColor == AtmosphereTheme.colorScheme.cardLevel2 -> 8.dp
        else -> 3.dp
    }
    Surface(onClick, modifier, enabled, shape, colors.containerColor, colors.contentColor, shadowElevation = resolvedElevation, border = border) {
        Column(content = content)
    }
}

@Immutable data class ButtonColors(val containerColor: Color, val contentColor: Color)
object ButtonDefaults {
    @Composable fun buttonColors(
        containerColor: Color = AtmosphereTheme.colorScheme.primary,
        contentColor: Color = AtmosphereTheme.colorScheme.onPrimary,
        disabledContainerColor: Color = AtmosphereTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
        disabledContentColor: Color = AtmosphereTheme.colorScheme.onSurfaceVariant.copy(alpha = .55f),
    ) = ButtonColors(containerColor, contentColor)
}

@Composable
private fun BaseButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    colors: ButtonColors,
    border: BorderStroke?,
    shape: Shape,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val motion = AtmosphereTheme.motionEnabled
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .97f else 1f,
        animationSpec = if (motion) tween(120) else snap(),
        label = "button press scale",
    )
    val container = if (enabled) colors.containerColor else colors.containerColor.copy(alpha = .42f)
    val contentColor = if (enabled) colors.contentColor else colors.contentColor.copy(alpha = .58f)
    CompositionLocalProvider(LocalAtmosphereContentColor provides contentColor) {
        Row(
            modifier = modifier
                .height(52.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (container == AtmosphereTheme.colorScheme.primary && enabled) {
                        Modifier.premiumGradientSurface(
                            shape = shape,
                            brush = Brush.linearGradient(
                                colorStops = arrayOf(
                                    0f to PremiumPalette.HeroStart,
                                    .55f to PremiumPalette.HeroMiddle,
                                    1f to PremiumPalette.HeroEnd,
                                ),
                            ),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = if (pressed) .12f else .22f)),
                            shadow = if (pressed) 2.dp else 10.dp,
                            shadowAlpha = if (pressed) .18f else .38f,
                        )
                    } else {
                        Modifier.atmosphericSurface(shape, container, border, if (enabled && !pressed) 5.dp else 1.dp)
                    },
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                )
                .padding(contentPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically,
        ) { content() }
    }
}

@Composable fun Button(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = RoundedCornerShape(18.dp),
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, colors, null, shape, contentPadding, content)

@Composable fun FilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, ButtonColors(AtmosphereTheme.colorScheme.surfaceVariant, AtmosphereTheme.colorScheme.onSurface), BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), RoundedCornerShape(18.dp), contentPadding, content)

@Composable fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, ButtonColors(Color.Transparent, AtmosphereTheme.colorScheme.onSurface), BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), RoundedCornerShape(18.dp), contentPadding, content)

@Composable fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, ButtonColors(Color.Transparent, AtmosphereTheme.colorScheme.primary), null, RoundedCornerShape(18.dp), contentPadding, content)

@Composable fun IconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) .94f else 1f,
        animationSpec = if (AtmosphereTheme.motionEnabled) tween(120) else snap(),
        label = "icon button press scale",
    )
    Box(
        modifier
            .size(48.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable fun FloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) = Surface(onClick, modifier.size(58.dp), shape = RoundedCornerShape(18.dp), color = AtmosphereTheme.colorScheme.primary, contentColor = AtmosphereTheme.colorScheme.onPrimary, shadowElevation = 10.dp) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
}

@Composable fun ExtendedFloatingActionButton(
    onClick: () -> Unit,
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) = Button(onClick, modifier) { icon(); text() }

@Composable private fun Chip(
    selected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
    leadingIcon: (@Composable () -> Unit)?,
    label: @Composable () -> Unit,
) {
    val motion = AtmosphereTheme.motionEnabled
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (motion) spring(
            dampingRatio = .76f,
            stiffness = Spring.StiffnessMediumLow,
        ) else snap(),
        label = "chip gradient",
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) Color.White else AtmosphereTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (motion) spring() else snap(),
        label = "chip content",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else AtmosphereTheme.colorScheme.outlineVariant,
        animationSpec = if (motion) spring() else snap(),
        label = "chip border",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.025f else 1f,
        animationSpec = if (motion) spring(
            dampingRatio = .7f,
            stiffness = Spring.StiffnessMediumLow,
        ) else snap(),
        label = "chip scale",
    )
    val shape = CircleShape
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides contentColor,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.Bold else FontWeight.Medium,
    ) {
        Row(
            modifier
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (selected) Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = PremiumPalette.HeroStart.copy(alpha = .30f),
                        spotColor = PremiumPalette.HeroStart.copy(alpha = .30f),
                    ) else Modifier,
                )
                .clip(shape)
                .background(AtmosphereTheme.colorScheme.cardLevel1)
                .drawWithCache {
                    val activeBrush = Brush.horizontalGradient(
                        listOf(PremiumPalette.HeroStart, PremiumPalette.HeroMiddle),
                    )
                    val idleBrush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .045f), Color.Transparent),
                    )
                    onDrawBehind {
                        drawRect(idleBrush)
                        drawRect(activeBrush, alpha = selectedProgress)
                        drawLine(
                            Color.White.copy(alpha = .18f * selectedProgress),
                            androidx.compose.ui.geometry.Offset(size.width * .16f, 1.dp.toPx()),
                            androidx.compose.ui.geometry.Offset(size.width * .70f, 1.dp.toPx()),
                            1.dp.toPx(),
                        )
                    }
                }
                .border(1.dp, borderColor, shape)
                .semantics { this.selected = selected }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 15.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
        ) { leadingIcon?.invoke(); label() }
    }
}

@Composable fun AssistChip(onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null) = Chip(false, onClick, enabled, modifier, leadingIcon, label)
@Composable fun FilterChip(selected: Boolean, onClick: () -> Unit, label: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, leadingIcon: (@Composable () -> Unit)? = null) = Chip(selected, onClick, enabled, modifier, leadingIcon, label)

@Composable
fun OutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = AtmosphereTheme.typography.bodyLarge,
    label: (@Composable () -> Unit)? = null,
    placeholder: (@Composable () -> Unit)? = null,
    leadingIcon: (@Composable () -> Unit)? = null,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
) {
    val borderColor = when { isError -> AtmosphereTheme.colorScheme.error; else -> AtmosphereTheme.colorScheme.outline }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        label?.let { Box(Modifier.padding(horizontal = 4.dp)) { it() } }
        Row(
            Modifier.fillMaxWidth().atmosphericSurface(AtmosphereTheme.shapes.medium, AtmosphereTheme.colorScheme.surfaceContainerHigh, BorderStroke(1.dp, borderColor), 2.dp).padding(horizontal = 14.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            leadingIcon?.invoke()
            Box(Modifier.weight(1f)) {
                if (value.isEmpty()) placeholder?.invoke()
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = enabled,
                    readOnly = readOnly,
                    textStyle = textStyle.merge(color = AtmosphereTheme.colorScheme.onSurface),
                    cursorBrush = SolidColor(AtmosphereTheme.colorScheme.primary),
                    visualTransformation = visualTransformation,
                    keyboardOptions = keyboardOptions,
                    keyboardActions = keyboardActions,
                    singleLine = singleLine,
                    maxLines = maxLines,
                    minLines = minLines,
                )
            }
            trailingIcon?.invoke()
        }
        supportingText?.let { Box(Modifier.padding(horizontal = 4.dp)) { it() } }
    }
}

@Composable fun HorizontalDivider(modifier: Modifier = Modifier, thickness: Dp = 1.dp, color: Color = AtmosphereTheme.colorScheme.outlineVariant) = Spacer(modifier.fillMaxWidth().height(thickness).background(color))

@Composable fun CircularProgressIndicator(modifier: Modifier = Modifier, color: Color = AtmosphereTheme.colorScheme.primary, strokeWidth: Dp = 3.dp) {
    Canvas(modifier.size(34.dp)) { drawArc(color, -90f, 270f, false, style = androidx.compose.ui.graphics.drawscope.Stroke(strokeWidth.toPx())) }
}

@Composable fun LinearProgressIndicator(modifier: Modifier = Modifier, color: Color = AtmosphereTheme.colorScheme.primary, trackColor: Color = AtmosphereTheme.colorScheme.cardLevel1) {
    val progressBrush = if (color == AtmosphereTheme.colorScheme.primary) {
        Brush.horizontalGradient(listOf(PremiumPalette.HeroStart, PremiumPalette.HeroMiddle, PremiumPalette.HeroEnd))
    } else Brush.horizontalGradient(listOf(color, color))
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(trackColor)) {
        Box(Modifier.fillMaxWidth(.66f).height(6.dp).background(progressBrush))
    }
}

@Composable fun LinearProgressIndicator(progress: () -> Float, modifier: Modifier = Modifier, color: Color = AtmosphereTheme.colorScheme.primary, trackColor: Color = AtmosphereTheme.colorScheme.cardLevel1) {
    val fraction = progress().coerceIn(0f, 1f)
    val progressBrush = if (color == AtmosphereTheme.colorScheme.primary) {
        Brush.horizontalGradient(listOf(PremiumPalette.HeroStart, PremiumPalette.HeroMiddle, PremiumPalette.HeroEnd))
    } else Brush.horizontalGradient(listOf(color, color))
    Box(modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(trackColor)) {
        Box(Modifier.fillMaxWidth(fraction).height(6.dp).background(progressBrush))
    }
}

@Composable fun Checkbox(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) = ToggleGlyph(checked, onCheckedChange, modifier, enabled, false)
@Composable fun RadioButton(selected: Boolean, onClick: (() -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) = ToggleGlyph(selected, { onClick?.invoke() }, modifier, enabled, true)

@Composable private fun ToggleGlyph(active: Boolean, onChange: ((Boolean) -> Unit)?, modifier: Modifier, enabled: Boolean, round: Boolean) {
    val shape = if (round) CircleShape else RoundedCornerShape(7.dp)
    Box(modifier.size(26.dp).atmosphericSurface(shape, if (active) AtmosphereTheme.colorScheme.primary else Color.Transparent, BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), 0.dp).clickable(enabled = enabled && onChange != null) { onChange?.invoke(!active) }, contentAlignment = Alignment.Center) {
        if (active) Text(if (round) "•" else "✓", color = AtmosphereTheme.colorScheme.onPrimary, fontSize = 17.sp)
    }
}

@Composable fun Switch(checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Box(modifier.width(52.dp).height(30.dp).clip(CircleShape).background(if (checked) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.surfaceVariant).clickable(enabled = enabled && onCheckedChange != null) { onCheckedChange?.invoke(!checked) }.padding(4.dp)) {
        Box(Modifier.size(22.dp).align(if (checked) Alignment.CenterEnd else Alignment.CenterStart).background(if (checked) AtmosphereTheme.colorScheme.onPrimary else AtmosphereTheme.colorScheme.onSurfaceVariant, CircleShape))
    }
}

@Composable
fun TopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(48.dp), contentAlignment = Alignment.CenterStart) { navigationIcon() }
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) { title() }
        Row(content = actions)
    }
}

@Composable
fun Scaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    containerColor: Color = AtmosphereTheme.colorScheme.background,
    contentWindowInsets: WindowInsets = WindowInsets(0, 0, 0, 0),
    content: @Composable (PaddingValues) -> Unit,
) {
    @Suppress("UNUSED_VARIABLE") val handledInsets = contentWindowInsets
    Box(
        modifier
            .fillMaxSize()
            .background(containerColor)
            .drawWithCache {
                val upperBlob = Brush.radialGradient(
                    listOf(PremiumPalette.HeroStart.copy(alpha = .10f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * .12f, size.height * .08f),
                    radius = size.maxDimension * .52f,
                )
                val lowerBlob = Brush.radialGradient(
                    listOf(PremiumPalette.AccentSecondary.copy(alpha = .075f), Color.Transparent),
                    center = androidx.compose.ui.geometry.Offset(size.width * .94f, size.height * .72f),
                    radius = size.maxDimension * .48f,
                )
                onDrawBehind {
                    drawRect(upperBlob)
                    drawRect(lowerBlob)
                }
            },
    ) {
        Column(Modifier.fillMaxSize()) {
            topBar()
            Box(Modifier.weight(1f).fillMaxWidth()) { content(PaddingValues(0.dp)) }
            bottomBar()
        }
        Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp)) { snackbarHost() }
        Box(Modifier.align(Alignment.BottomEnd).padding(20.dp)) { floatingActionButton() }
    }
}

@Composable fun AlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    title: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
) = Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .58f))
            .clickable(onClick = onDismissRequest)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier
                .fillMaxWidth(.9f)
                .widthIn(max = 560.dp)
                .atmosphericSurface(AtmosphereTheme.shapes.extraLarge, AtmosphereTheme.colorScheme.surfaceContainer, BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), 24.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                )
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            CompositionLocalProvider(LocalAtmosphereContentColor provides AtmosphereTheme.colorScheme.onSurface) {
                icon?.invoke(); title?.invoke(); text?.invoke(); Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End), verticalAlignment = Alignment.CenterVertically) { dismissButton?.invoke(); confirmButton() }
            }
        }
    }
}

@Stable class SheetState
@Composable fun rememberModalBottomSheetState(skipPartiallyExpanded: Boolean = false): SheetState { @Suppress("UNUSED_VARIABLE") val ignored = skipPartiallyExpanded; return remember { SheetState() } }

@Composable fun ModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    content: @Composable ColumnScope.() -> Unit,
) = Dialog(onDismissRequest, DialogProperties(usePlatformDefaultWidth = false)) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .58f))
            .clickable(onClick = onDismissRequest)
            .windowInsetsPadding(WindowInsets.safeDrawing),
        contentAlignment = Alignment.BottomCenter,
    ) {
        CompositionLocalProvider(LocalAtmosphereContentColor provides AtmosphereTheme.colorScheme.onSurface) {
            Column(
                modifier
                    .fillMaxWidth()
                    .atmosphericSurface(RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp), AtmosphereTheme.colorScheme.surfaceContainer, BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), 24.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {},
                    )
                    .padding(20.dp),
                content = content,
            )
        }
    }
}

@Composable fun DropdownMenu(expanded: Boolean, onDismissRequest: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    if (expanded) Popup(onDismissRequest = onDismissRequest, properties = PopupProperties(focusable = true)) {
        Column(modifier.widthIn(min = 220.dp, max = 340.dp).atmosphericSurface(AtmosphereTheme.shapes.large, AtmosphereTheme.colorScheme.surfaceContainerHighest, BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), 12.dp).padding(8.dp), content = content)
    }
}

@Composable fun DropdownMenuItem(text: @Composable () -> Unit, onClick: () -> Unit, modifier: Modifier = Modifier, leadingIcon: (@Composable () -> Unit)? = null) {
    Row(modifier.fillMaxWidth().clip(AtmosphereTheme.shapes.medium).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 13.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) { leadingIcon?.invoke(); text() }
}

@Stable class SnackbarHostState { var currentSnackbarData by mutableStateOf<String?>(null); suspend fun showSnackbar(message: String): SnackbarResult { currentSnackbarData = message; delay(3200); currentSnackbarData = null; return SnackbarResult.Dismissed } }
enum class SnackbarResult { Dismissed, ActionPerformed }
@Composable fun SnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) { hostState.currentSnackbarData?.let { Text(it, modifier.atmosphericSurface(RoundedCornerShape(24.dp), AtmosphereTheme.colorScheme.surfaceContainerHighest, BorderStroke(1.dp, AtmosphereTheme.colorScheme.outline), 8.dp).padding(horizontal = 18.dp, vertical = 12.dp)) } }

@Stable class DatePickerState(initialSelectedDateMillis: Long?) { var selectedDateMillis by mutableStateOf(initialSelectedDateMillis) }
@Composable fun rememberDatePickerState(initialSelectedDateMillis: Long? = null) = remember(initialSelectedDateMillis) { DatePickerState(initialSelectedDateMillis) }
@Composable fun DatePicker(state: DatePickerState, modifier: Modifier = Modifier) {
    val initial = remember(state) { java.time.Instant.ofEpochMilli(state.selectedDateMillis ?: System.currentTimeMillis()).atZone(java.time.ZoneOffset.UTC).toLocalDate() }
    AndroidView(
        factory = { context -> AndroidDatePicker(context).apply { init(initial.year, initial.monthValue - 1, initial.dayOfMonth) { _, year, month, day -> state.selectedDateMillis = java.time.LocalDate.of(year, month + 1, day).atStartOfDay().toInstant(java.time.ZoneOffset.UTC).toEpochMilli() } } },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable fun DatePickerDialog(onDismissRequest: () -> Unit, confirmButton: @Composable () -> Unit, dismissButton: (@Composable () -> Unit)? = null, content: @Composable () -> Unit) = AlertDialog(onDismissRequest, confirmButton, dismissButton = dismissButton, text = content)

@Stable class TimePickerState(initialHour: Int, initialMinute: Int) { var hour by mutableIntStateOf(initialHour); var minute by mutableIntStateOf(initialMinute) }
@Composable fun rememberTimePickerState(initialHour: Int = 0, initialMinute: Int = 0, is24Hour: Boolean = true): TimePickerState { @Suppress("UNUSED_VARIABLE") val format = is24Hour; return remember(initialHour, initialMinute) { TimePickerState(initialHour, initialMinute) } }
@Composable fun TimePicker(state: TimePickerState, modifier: Modifier = Modifier) = AndroidView(factory = { context -> AndroidTimePicker(context).apply { setIs24HourView(true); hour = state.hour; minute = state.minute; setOnTimeChangedListener { _, h, m -> state.hour = h; state.minute = m } } }, modifier = modifier)

object SegmentedButtonDefaults {
    @Composable fun itemShape(index: Int, count: Int): Shape {
        @Suppress("UNUSED_VARIABLE") val stablePosition = index to count
        return CircleShape
    }
}
@Composable fun SingleChoiceSegmentedButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    content = content,
)
@Composable fun RowScope.SegmentedButton(selected: Boolean, onClick: () -> Unit, shape: Shape, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    val motion = AtmosphereTheme.motionEnabled
    val selectedProgress by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = if (motion) spring(
            dampingRatio = .76f,
            stiffness = Spring.StiffnessMediumLow,
        ) else snap(),
        label = "segment gradient",
    )
    val foreground by animateColorAsState(
        targetValue = if (selected) Color.White else AtmosphereTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (motion) spring() else snap(),
        label = "segment content",
    )
    val borderColor by animateColorAsState(
        targetValue = if (selected) Color.Transparent else AtmosphereTheme.colorScheme.outlineVariant,
        animationSpec = if (motion) spring() else snap(),
        label = "segment border",
    )
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.02f else 1f,
        animationSpec = if (motion) spring(
            dampingRatio = .72f,
            stiffness = Spring.StiffnessMediumLow,
        ) else snap(),
        label = "segment scale",
    )
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides foreground,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.Bold else FontWeight.Medium,
    ) {
        Box(
            modifier
                .weight(1f)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .then(
                    if (selected) Modifier.shadow(
                        elevation = 14.dp,
                        shape = shape,
                        clip = false,
                        ambientColor = PremiumPalette.HeroStart.copy(alpha = .30f),
                        spotColor = PremiumPalette.HeroStart.copy(alpha = .30f),
                    ) else Modifier,
                )
                .clip(shape)
                .background(AtmosphereTheme.colorScheme.cardLevel1)
                .drawWithCache {
                    val activeBrush = Brush.horizontalGradient(
                        listOf(PremiumPalette.HeroStart, PremiumPalette.HeroMiddle),
                    )
                    val idleBrush = Brush.verticalGradient(
                        listOf(Color.White.copy(alpha = .045f), Color.Transparent),
                    )
                    onDrawBehind {
                        drawRect(idleBrush)
                        drawRect(activeBrush, alpha = selectedProgress)
                        drawLine(
                            Color.White.copy(alpha = .18f * selectedProgress),
                            androidx.compose.ui.geometry.Offset(size.width * .18f, 1.dp.toPx()),
                            androidx.compose.ui.geometry.Offset(size.width * .72f, 1.dp.toPx()),
                            1.dp.toPx(),
                        )
                    }
                }
                .border(1.dp, borderColor, shape)
                .semantics { this.selected = selected }
                .clickable(onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) { label() }
    }
}

@Stable
private class NavigationIndicatorState {
    var targetCenterPx by mutableFloatStateOf(Float.NaN)
    var initialized by mutableStateOf(false)

    fun report(centerPx: Float) {
        if (centerPx.isFinite() && centerPx != targetCenterPx) targetCenterPx = centerPx
    }
}

private val LocalNavigationIndicatorState = compositionLocalOf<NavigationIndicatorState?> { null }

@Composable fun NavigationBar(modifier: Modifier = Modifier, containerColor: Color = AtmosphereTheme.colorScheme.surfaceContainer, tonalElevation: Dp = 0.dp, content: @Composable RowScope.() -> Unit) {
    @Suppress("UNUSED_VARIABLE") val noTonalElevation = tonalElevation
    val motion = AtmosphereTheme.motionEnabled
    val indicatorState = remember { NavigationIndicatorState() }
    val indicatorCenter = remember { Animatable(0f) }
    val density = LocalDensity.current
    val dockShape = RoundedCornerShape(28.dp)
    val activeShape = RoundedCornerShape(18.dp)
    val dark = AtmosphereTheme.colorScheme.background == PremiumPalette.Base

    LaunchedEffect(indicatorState.targetCenterPx, motion) {
        val target = indicatorState.targetCenterPx
        if (!target.isFinite()) return@LaunchedEffect
        if (!indicatorState.initialized || !motion) {
            indicatorCenter.snapTo(target)
            indicatorState.initialized = true
        } else {
            indicatorCenter.animateTo(
                targetValue = target,
                animationSpec = spring(
                    dampingRatio = .72f,
                    stiffness = Spring.StiffnessMediumLow,
                ),
            )
        }
    }

    Box(
        modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp)
            .shadow(
                elevation = 14.dp,
                shape = dockShape,
                clip = false,
                ambientColor = PremiumPalette.HomeGlow.copy(alpha = .22f),
                spotColor = Color.Black.copy(alpha = .26f),
            )
            .clip(dockShape)
            .background(containerColor.copy(alpha = if (dark) .94f else .97f))
            .background(
                Brush.verticalGradient(
                    listOf(Color.White.copy(alpha = if (dark) .06f else .16f), Color.Transparent),
                ),
            )
            .border(
                width = 1.dp,
                color = if (dark) Color.White.copy(alpha = .08f) else AtmosphereTheme.colorScheme.outlineVariant,
                shape = dockShape,
            )
            .padding(horizontal = 6.dp, vertical = 6.dp),
    ) {
        if (indicatorState.initialized) {
            val halfPillPx = with(density) { 26.dp.toPx() }
            Box(
                Modifier
                    .offset {
                        IntOffset(
                            x = (indicatorCenter.value - halfPillPx).roundToInt(),
                            y = 0,
                        )
                    }
                    .width(52.dp)
                    .height(38.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = activeShape,
                        clip = false,
                        ambientColor = PremiumPalette.HeroMiddle.copy(alpha = .28f),
                        spotColor = PremiumPalette.HeroMiddle.copy(alpha = .28f),
                    )
                    .clip(activeShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(PremiumPalette.HeroStart, PremiumPalette.HeroEnd),
                        ),
                    ),
            )
        }
        CompositionLocalProvider(LocalNavigationIndicatorState provides indicatorState) {
            Row(
                modifier = Modifier.fillMaxWidth().heightIn(min = 58.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
                content = content,
            )
        }
    }
}
@Immutable data class NavigationItemColors(val indicatorColor: Color)
object NavigationBarItemDefaults { @Composable fun colors(indicatorColor: Color = AtmosphereTheme.colorScheme.primaryContainer) = NavigationItemColors(indicatorColor) }
@Composable fun RowScope.NavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: (@Composable () -> Unit)? = null, colors: NavigationItemColors = NavigationBarItemDefaults.colors()) {
    @Suppress("UNUSED_VARIABLE") val retainedIndicatorColorApi = colors.indicatorColor
    val motion = AtmosphereTheme.motionEnabled
    val coordinator = LocalNavigationIndicatorState.current
    var centerInParent by remember { mutableFloatStateOf(Float.NaN) }
    val iconScale = remember { Animatable(1f) }
    val foreground by animateColorAsState(
        targetValue = if (selected) Color.White else AtmosphereTheme.colorScheme.statusNeutral,
        animationSpec = if (motion) spring() else snap(),
        label = "navigation content",
    )

    LaunchedEffect(selected, centerInParent) {
        if (selected && centerInParent.isFinite()) coordinator?.report(centerInParent)
    }
    LaunchedEffect(selected, motion) {
        if (selected && motion) {
            iconScale.snapTo(1f)
            iconScale.animateTo(
                1.15f,
                spring(dampingRatio = .58f, stiffness = Spring.StiffnessMedium),
            )
            iconScale.animateTo(
                1f,
                spring(dampingRatio = .72f, stiffness = Spring.StiffnessMediumLow),
            )
        } else {
            iconScale.snapTo(1f)
        }
    }

    CompositionLocalProvider(
        LocalAtmosphereContentColor provides foreground,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.Bold else FontWeight.Normal,
    ) {
        Column(
            modifier
                .weight(1f)
                .heightIn(min = 58.dp)
                .onGloballyPositioned { coordinates ->
                    centerInParent = coordinates.boundsInParent().center.x
                    if (selected) coordinator?.report(centerInParent)
                }
                .semantics { this.selected = selected }
                .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(
                modifier = Modifier
                    .height(38.dp)
                    .graphicsLayer {
                        scaleX = iconScale.value
                        scaleY = iconScale.value
                    },
                contentAlignment = Alignment.Center,
            ) { icon() }
            label?.invoke()
        }
    }
}
@Composable fun NavigationRail(modifier: Modifier = Modifier, containerColor: Color = AtmosphereTheme.colorScheme.surfaceContainer, content: @Composable ColumnScope.() -> Unit) = Column(modifier.background(containerColor).padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
@Composable fun ColumnScope.NavigationRailItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: (@Composable () -> Unit)? = null) {
    val foreground = if (selected) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalAtmosphereContentColor provides foreground) {
        Column(modifier.width(76.dp).clip(AtmosphereTheme.shapes.medium).background(if (selected) AtmosphereTheme.colorScheme.primaryContainer else Color.Transparent).semantics { this.selected = selected }.clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { icon(); label?.invoke() }
    }
}
