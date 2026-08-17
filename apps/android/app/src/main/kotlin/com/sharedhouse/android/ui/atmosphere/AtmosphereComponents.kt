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
                elevation = shadow,
                shape = shape,
                clip = false,
                ambientColor = Color.Black.copy(alpha = .14f),
                spotColor = Color.Black.copy(alpha = .18f),
            )
        } else Modifier
    )
    .clip(shape)
    .background(color)
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
                elevation = shadow,
                shape = shape,
                clip = false,
                ambientColor = PremiumPalette.HeroStart.copy(alpha = shadowAlpha),
                spotColor = Color.Black.copy(alpha = .18f),
            )
        } else Modifier,
    )
    .clip(shape)
    .background(brush)
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

/** Cupertino grouped background: quiet, neutral and deliberately free of decorative lighting. */
@Composable
fun AmbientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = Box(
    modifier = modifier.background(AtmosphereTheme.colorScheme.background),
    content = content,
)

/** Cupertino feature card: one focused system-blue surface reserved for the main next action. */
@Composable
fun PremiumHeroCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    val interactive = if (onClick == null) Modifier else Modifier.clickable(role = Role.Button, onClick = onClick)
    CompositionLocalProvider(LocalAtmosphereContentColor provides AtmosphereTheme.colorScheme.onPrimary) {
        Box(
            modifier = modifier
                .atmosphericSurface(
                    shape = shape,
                    color = AtmosphereTheme.colorScheme.primary,
                    border = BorderStroke(1.dp, Color.White.copy(alpha = .16f)),
                    shadow = 2.dp,
                )
                .then(interactive),
        ) {
            Column(modifier = Modifier.padding(22.dp), content = content)
        }
    }
}

/** A compact system icon tile used for cards, empty states and fast actions. */
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
    val shape = RoundedCornerShape(12.dp)
    val base = if (hero) Color.White.copy(alpha = .20f) else AtmosphereTheme.colorScheme.primaryContainer
    Box(
        modifier = modifier
            .size(badgeSize)
            .clip(shape)
            .background(base)
            .border(1.dp, Color.White.copy(alpha = if (hero) .18f else .06f), shape),
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

    @Composable fun cardBorder(containerColor: Color): BorderStroke = BorderStroke(
        .75.dp,
        if (containerColor == AtmosphereTheme.colorScheme.cardLevel1) {
            AtmosphereTheme.colorScheme.outlineVariant.copy(alpha = .62f)
        } else {
            AtmosphereTheme.colorScheme.outlineVariant.copy(alpha = .78f)
        },
    )

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
        colors.containerColor == AtmosphereTheme.colorScheme.cardLevel2 -> 1.dp
        else -> 0.dp
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
        colors.containerColor == AtmosphereTheme.colorScheme.cardLevel2 -> 1.dp
        else -> 0.dp
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
        targetValue = if (pressed && enabled) .98f else 1f,
        animationSpec = if (motion) tween(120) else snap(),
        label = "button press scale",
    )
    val container = if (enabled) colors.containerColor else colors.containerColor.copy(alpha = .42f)
    val contentColor = if (enabled) colors.contentColor else colors.contentColor.copy(alpha = .58f)
    CompositionLocalProvider(LocalAtmosphereContentColor provides contentColor) {
        Row(
            modifier = modifier
                .height(50.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .atmosphericSurface(
                    shape = shape,
                    color = container,
                    border = border,
                    shadow = if (container == AtmosphereTheme.colorScheme.primary && enabled && !pressed) 1.dp else 0.dp,
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
    shape: Shape = RoundedCornerShape(14.dp),
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
) = BaseButton(onClick, modifier, enabled, ButtonColors(AtmosphereTheme.colorScheme.primaryContainer, AtmosphereTheme.colorScheme.onPrimaryContainer), BorderStroke(.75.dp, Color.Transparent), RoundedCornerShape(14.dp), contentPadding, content)

@Composable fun OutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 20.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, ButtonColors(Color.Transparent, AtmosphereTheme.colorScheme.primary), BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant), RoundedCornerShape(14.dp), contentPadding, content)

@Composable fun TextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    content: @Composable RowScope.() -> Unit,
) = BaseButton(onClick, modifier, enabled, ButtonColors(Color.Transparent, AtmosphereTheme.colorScheme.primary), null, RoundedCornerShape(14.dp), contentPadding, content)

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
            .clip(CircleShape)
            .background(AtmosphereTheme.colorScheme.surfaceVariant)
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
) = Surface(onClick, modifier.size(56.dp), shape = CircleShape, color = AtmosphereTheme.colorScheme.primary, contentColor = AtmosphereTheme.colorScheme.onPrimary, shadowElevation = 3.dp) {
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
    val contentColor by animateColorAsState(
        targetValue = if (selected) AtmosphereTheme.colorScheme.onPrimaryContainer else AtmosphereTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (motion) tween(140) else snap(),
        label = "chip content",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) AtmosphereTheme.colorScheme.surface else AtmosphereTheme.colorScheme.surfaceContainerHigh,
        animationSpec = if (motion) tween(140) else snap(),
        label = "chip container",
    )
    val shape = RoundedCornerShape(12.dp)
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides contentColor,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.Bold else FontWeight.Medium,
    ) {
        Row(
            modifier
                .clip(shape)
                .background(containerColor)
                .border(.75.dp, if (selected) AtmosphereTheme.colorScheme.outlineVariant else Color.Transparent, shape)
                .semantics { this.selected = selected }
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
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
            Modifier.fillMaxWidth().atmosphericSurface(AtmosphereTheme.shapes.medium, AtmosphereTheme.colorScheme.surfaceContainerHigh, BorderStroke(1.dp, borderColor), 0.dp).padding(horizontal = 16.dp, vertical = 14.dp),
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
            .background(containerColor),
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
                .atmosphericSurface(AtmosphereTheme.shapes.extraLarge, AtmosphereTheme.colorScheme.surfaceContainer, BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant), 8.dp)
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
                    .atmosphericSurface(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp), AtmosphereTheme.colorScheme.surfaceContainer, BorderStroke(.75.dp, AtmosphereTheme.colorScheme.outlineVariant), 8.dp)
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
        return RoundedCornerShape(10.dp)
    }
}
@Composable fun SingleChoiceSegmentedButtonRow(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) = Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(6.dp),
    content = content,
)
@Composable fun RowScope.SegmentedButton(selected: Boolean, onClick: () -> Unit, shape: Shape, modifier: Modifier = Modifier, label: @Composable () -> Unit) {
    val motion = AtmosphereTheme.motionEnabled
    val foreground by animateColorAsState(
        targetValue = if (selected) AtmosphereTheme.colorScheme.onSurface else AtmosphereTheme.colorScheme.onSurfaceVariant,
        animationSpec = if (motion) tween(120) else snap(),
        label = "segment content",
    )
    val container by animateColorAsState(
        targetValue = if (selected) AtmosphereTheme.colorScheme.surface else AtmosphereTheme.colorScheme.surfaceContainerHigh,
        animationSpec = if (motion) tween(120) else snap(),
        label = "segment container",
    )
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides foreground,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.SemiBold else FontWeight.Medium,
    ) {
        Box(
            modifier
                .weight(1f)
                .clip(shape)
                .background(container)
                .border(.75.dp, if (selected) AtmosphereTheme.colorScheme.outlineVariant else Color.Transparent, shape)
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
    val tabShape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
    Row(
        modifier
            .fillMaxWidth()
            .shadow(1.dp, tabShape, clip = false, ambientColor = Color.Black.copy(alpha = .12f), spotColor = Color.Black.copy(alpha = .12f))
            .clip(tabShape)
            .background(containerColor)
            .border(.75.dp, AtmosphereTheme.colorScheme.outlineVariant.copy(alpha = .72f), tabShape)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
@Immutable data class NavigationItemColors(val indicatorColor: Color)
object NavigationBarItemDefaults { @Composable fun colors(indicatorColor: Color = AtmosphereTheme.colorScheme.primaryContainer) = NavigationItemColors(indicatorColor) }
@Composable fun RowScope.NavigationBarItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: (@Composable () -> Unit)? = null, colors: NavigationItemColors = NavigationBarItemDefaults.colors()) {
    @Suppress("UNUSED_VARIABLE") val retainedIndicatorColorApi = colors.indicatorColor
    val motion = AtmosphereTheme.motionEnabled
    val foreground by animateColorAsState(
        targetValue = if (selected) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.statusNeutral,
        animationSpec = if (motion) tween(140) else snap(),
        label = "navigation content",
    )
    val indicator by animateColorAsState(
        targetValue = Color.Transparent,
        animationSpec = if (motion) tween(140) else snap(),
        label = "navigation indicator",
    )
    val shape = RoundedCornerShape(10.dp)
    CompositionLocalProvider(
        LocalAtmosphereContentColor provides foreground,
        LocalAtmosphereFontWeight provides if (selected) FontWeight.Bold else FontWeight.Medium,
    ) {
        Column(
            modifier
                .weight(1f)
                .clip(shape)
                .background(indicator)
                .semantics { this.selected = selected }
                .clickable(enabled = enabled, role = Role.Tab, onClick = onClick)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.height(24.dp), contentAlignment = Alignment.Center) { icon() }
            label?.invoke()
        }
    }
}
@Composable fun NavigationRail(modifier: Modifier = Modifier, containerColor: Color = AtmosphereTheme.colorScheme.surfaceContainer, content: @Composable ColumnScope.() -> Unit) = Column(modifier.background(containerColor).border(.75.dp, AtmosphereTheme.colorScheme.outlineVariant).padding(horizontal = 8.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
@Composable fun ColumnScope.NavigationRailItem(selected: Boolean, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, label: (@Composable () -> Unit)? = null) {
    val foreground = if (selected) AtmosphereTheme.colorScheme.primary else AtmosphereTheme.colorScheme.onSurfaceVariant
    CompositionLocalProvider(LocalAtmosphereContentColor provides foreground) {
        Column(modifier.width(76.dp).clip(AtmosphereTheme.shapes.medium).background(if (selected) AtmosphereTheme.colorScheme.primaryContainer else Color.Transparent).semantics { this.selected = selected }.clickable(enabled = enabled, onClick = onClick).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) { icon(); label?.invoke() }
    }
}
