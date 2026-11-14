package com.example.empati.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.autofill.ContentType
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.empati.R
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.empati.data.RiskLevel
import com.example.empati.data.label
import com.example.empati.data.presentation.PresentationTone
import com.example.empati.ui.theme.DisplayFont
import com.example.empati.ui.theme.EmpatiElevation
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.theme.ToneColors
import com.example.empati.ui.theme.empati
import com.example.empati.ui.theme.softShadow
import kotlinx.coroutines.delay

// ═══════════════════════════════════════════════════════════════════════════
//  Surfaces & semantic mapping
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Tonal elevation ladder (no heavy borders, no drop shadows on resting content):
 *   canvas  <  inset  <  card
 * Light: card is the brightest layer (white), canvas the warmest. Dark: card is lifted off the canvas.
 */
object EmpatiSurfaces {
    /** Resting content container (cards, sheets). */
    @Composable
    @ReadOnlyComposable
    fun card(): Color =
        if (MaterialTheme.empati.isDark) MaterialTheme.colorScheme.surfaceContainerLow
        else MaterialTheme.colorScheme.surfaceContainerLowest

    /** A quieter layer sitting *inside* a card or directly on the canvas (fields, chips, notes). */
    @Composable
    @ReadOnlyComposable
    fun inset(): Color =
        if (MaterialTheme.empati.isDark) MaterialTheme.colorScheme.surfaceContainerHigh
        else MaterialTheme.colorScheme.surfaceContainerLow

    /** Recessed track (segmented control, progress track). */
    @Composable
    @ReadOnlyComposable
    fun sunken(): Color =
        if (MaterialTheme.empati.isDark) MaterialTheme.colorScheme.surfaceContainerLowest
        else MaterialTheme.colorScheme.surfaceContainer

    /** Decorative hairline: separates, never boxes in. */
    @Composable
    @ReadOnlyComposable
    fun hairline(): Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
}

/** Risk level → affective-state tone. */
@Composable
@ReadOnlyComposable
fun RiskLevel.tone(): ToneColors {
    val colors = MaterialTheme.empati
    return when (this) {
        RiskLevel.LOW -> colors.calm
        RiskLevel.MEDIUM -> colors.attentive
        RiskLevel.HIGH -> colors.concerned
        RiskLevel.VERY_HIGH -> colors.urgent
    }
}

/** Error / validation tone built from the Material error roles. */
@Composable
@ReadOnlyComposable
fun errorTone(): ToneColors = ToneColors(
    accent = MaterialTheme.colorScheme.error,
    container = MaterialTheme.colorScheme.errorContainer,
    onContainer = MaterialTheme.colorScheme.onErrorContainer
)

/** Presentation tone → affective-state tone. */
@Composable
@ReadOnlyComposable
fun PresentationTone.toneColors(): ToneColors {
    val colors = MaterialTheme.empati
    return when (this) {
        PresentationTone.POSITIVE -> colors.calm
        PresentationTone.ATTENTION -> colors.attentive
        PresentationTone.INCOMPLETE, PresentationTone.NEUTRAL -> colors.neutral
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Interaction modifiers
// ═══════════════════════════════════════════════════════════════════════════

/** Soft-material press: the element sinks slightly and springs back. Never shifts layout bounds. */
@Composable
fun Modifier.pressScale(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
    enabled: Boolean = true
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = EmpatiMotion.press(),
        label = "pressScale"
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Visible keyboard / D-pad focus bounds that fade in (WCAG 2.4.7). */
@Composable
fun Modifier.focusHalo(
    interactionSource: InteractionSource,
    shape: Shape,
    color: Color = MaterialTheme.colorScheme.primary
): Modifier {
    val focused by interactionSource.collectIsFocusedAsState()
    val alpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(EmpatiMotion.Quick),
        label = "focusHalo"
    )
    return if (alpha > 0.01f) this.border(2.5.dp, color.copy(alpha = alpha), shape) else this
}

/**
 * Staggered entrance: content eases up and fades in. Collapses to the final state when motion is reduced
 * (and in screenshot tests, where the theme forces reduced motion for determinism).
 */
@Composable
fun Reveal(
    modifier: Modifier = Modifier,
    index: Int = 0,
    content: @Composable () -> Unit
) {
    val reduced = LocalReducedMotion.current
    val progress = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            progress.snapTo(1f)
        } else {
            delay(index * 80L)
            progress.animateTo(1f, tween(EmpatiMotion.Reveal, easing = EmpatiMotion.EaseOutSoft))
        }
    }
    Box(
        modifier = modifier.graphicsLayer {
            alpha = progress.value
            translationY = (1f - progress.value) * 20.dp.toPx()
        }
    ) { content() }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Badges, chips, labels
// ═══════════════════════════════════════════════════════════════════════════

/** Soft status pill: accent dot + readable label. State is never conveyed by colour alone. */
@Composable
fun ToneBadge(label: String, tone: ToneColors, modifier: Modifier = Modifier) {
    Surface(
        shape = CircleShape,
        color = tone.container,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(tone.accent, CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                color = tone.onContainer,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun RiskBadge(level: RiskLevel, modifier: Modifier = Modifier) {
    ToneBadge(label = level.label, tone = level.tone(), modifier = modifier)
}

/** Quiet eyebrow label: sentence case, soft container. */
@Composable
fun SectionChip(
    text: String,
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Surface(
        shape = CircleShape,
        color = containerColor,
        modifier = modifier
    ) {
        Text(
            text = text,
            color = contentColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

/** Section heading inside a card or screen. Replaces ALL-CAPS monospace labels. */
@Composable
fun SectionLabel(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = modifier
    )
}

/** Rounded icon tile used as a leading visual in rows and cards. */
@Composable
fun SoftIconTile(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    tint: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    Box(
        modifier = modifier
            .size(size)
            .background(container, RoundedCornerShape(percent = 34)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val container by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else EmpatiSurfaces.inset(),
        animationSpec = EmpatiMotion.quick(),
        label = "chipContainer"
    )
    val content by animateColorAsState(
        targetValue = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant,
        animationSpec = EmpatiMotion.quick(),
        label = "chipContent"
    )
    val border by animateColorAsState(
        targetValue = if (selected) colors.primary.copy(alpha = 0.6f) else EmpatiSurfaces.hairline(),
        animationSpec = EmpatiMotion.quick(),
        label = "chipBorder"
    )
    Surface(
        shape = CircleShape,
        color = container,
        border = BorderStroke(1.dp, border),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
            .clip(CircleShape)
            .selectable(
                selected = selected,
                interactionSource = interactionSource,
                indication = androidx.compose.material3.ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = expandHorizontally(EmpatiMotion.quick()) + fadeIn(EmpatiMotion.quick()),
                exit = shrinkHorizontally(EmpatiMotion.quick()) + fadeOut(EmpatiMotion.quick())
            ) {
                Row {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = content,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            Text(
                text = label,
                color = content,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
            )
        }
    }
}

/** Pill-shaped segmented control with a gliding thumb (replaces boxy tab rows). */
@Composable
fun SegmentedSwitch(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val thumbTint = MaterialTheme.empati.shadowTint
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(CircleShape)
            .background(EmpatiSurfaces.sunken())
            .padding(4.dp)
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth()) {
            val segmentWidth = maxWidth / options.size
            val thumbOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = EmpatiMotion.expressive(),
                label = "segmentThumb"
            )
            Box(
                Modifier
                    .offset(x = thumbOffset)
                    .width(segmentWidth)
                    .height(46.dp)
                    .softShadow(2.dp, CircleShape, thumbTint)
                    .background(EmpatiSurfaces.card(), CircleShape)
            )
            Row(Modifier.fillMaxWidth()) {
                options.forEachIndexed { index, label ->
                    val isSelected = index == selectedIndex
                    val interactionSource = remember { MutableInteractionSource() }
                    val textColor by animateColorAsState(
                        targetValue = if (isSelected) MaterialTheme.colorScheme.onSurface
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = EmpatiMotion.quick(),
                        label = "segmentText"
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                            .clip(CircleShape)
                            .focusHalo(interactionSource, CircleShape)
                            .selectable(
                                selected = isSelected,
                                interactionSource = interactionSource,
                                indication = androidx.compose.material3.ripple(),
                                role = Role.Tab,
                                onClick = { onSelect(index) }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = textColor,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Buttons
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Primary action. Pill shape, ambient shadow (it floats above content), springy press,
 * inline loading state. While [loading] the click is swallowed and the state is announced.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = Icons.AutoMirrored.Rounded.ArrowForward,
    containerColor: Color = MaterialTheme.colorScheme.primary,
    contentColor: Color = MaterialTheme.colorScheme.onPrimary,
    loading: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val tint = MaterialTheme.empati.shadowTint
    val colors = MaterialTheme.colorScheme

    Button(
        onClick = { if (!loading) onClick() },
        enabled = enabled,
        interactionSource = interactionSource,
        shape = CircleShape,
        contentPadding = PaddingValues(horizontal = 28.dp, vertical = 16.dp),
        elevation = null,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor,
            disabledContainerColor = colors.surfaceContainerHigh,
            disabledContentColor = colors.onSurfaceVariant.copy(alpha = 0.6f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .pressScale(interactionSource, 0.975f, enabled && !loading)
            .then(
                if (enabled) Modifier.softShadow(EmpatiElevation.raised, CircleShape, tint)
                else Modifier
            )
            .focusHalo(interactionSource, CircleShape, colors.onSurface)
            .semantics { if (loading) stateDescription = "Sedang diproses" }
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium
        )
        if (loading) {
            Spacer(Modifier.width(10.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(18.dp),
                strokeWidth = 2.dp,
                color = contentColor,
                trackColor = contentColor.copy(alpha = 0.25f)
            )
        } else if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/** Quiet secondary action: no container, generous target, same press physics. */
@Composable
fun GhostButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaterialTheme.colorScheme.primary
) {
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        colors = ButtonDefaults.textButtonColors(contentColor = contentColor),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .pressScale(interactionSource, 0.98f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall
        )
    }
}

@Composable
fun BackIconButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = EmpatiSurfaces.card(),
        border = BorderStroke(1.dp, EmpatiSurfaces.hairline()),
        modifier = modifier
            .size(EmpatiSpace.touchTarget)
            .pressScale(interactionSource, 0.92f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Kembali",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Show / hide toggle for secret fields (accessible authentication). */
@Composable
fun VisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    subject: String,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onToggle, modifier = modifier) {
        Icon(
            imageVector = if (visible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
            contentDescription = if (visible) "Sembunyikan $subject" else "Tampilkan $subject",
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Cards & containers
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Pebble-shaped container. Depth comes from tone (card vs canvas) plus a hairline, not from a heavy border.
 * [elevated] adds an ambient shadow for hero content; [onClick] adds ripple, press sink and focus bounds.
 */
@Composable
fun TonalCard(
    modifier: Modifier = Modifier,
    containerColor: Color = EmpatiSurfaces.card(),
    borderColor: Color = EmpatiSurfaces.hairline(),
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = EmpatiSpace.cardPadding,
    elevated: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val shadowModifier = if (elevated) {
        Modifier.softShadow(EmpatiElevation.raised, shape, MaterialTheme.empati.shadowTint)
    } else Modifier

    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            interactionSource = interactionSource,
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier
                .then(shadowModifier)
                .pressScale(interactionSource, 0.985f)
                .focusHalo(interactionSource, shape)
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            modifier = modifier.then(shadowModifier)
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

/** Hairline separator. */
@Composable
fun SoftDivider(modifier: Modifier = Modifier) {
    HorizontalDivider(modifier = modifier, color = EmpatiSurfaces.hairline())
}

/**
 * Inline message (privacy reassurance, support, validation). Tone decides colour; the text always
 * carries the meaning. [liveRegion] announces changes to screen readers (for validation feedback).
 */
@Composable
fun SoftNotice(
    text: String,
    icon: ImageVector,
    tone: ToneColors,
    modifier: Modifier = Modifier,
    title: String? = null,
    liveRegion: Boolean = false
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tone.container,
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (liveRegion) Modifier.semantics { this.liveRegion = LiveRegionMode.Polite }
                else Modifier
            )
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier
                    .padding(top = 1.dp)
                    .size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                if (title != null) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = tone.onContainer
                    )
                    Spacer(Modifier.height(2.dp))
                }
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.onContainer
                )
            }
        }
    }
}

@Composable
fun LockNotice(modifier: Modifier = Modifier) {
    SoftNotice(
        text = "Jawaban Anda aman dan dirahasiakan di perangkat ini.",
        icon = Icons.Rounded.Lock,
        tone = MaterialTheme.empati.calm,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Text input
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Soft filled field. The label always sits inside the container (never lost while typing), the
 * indicator grows from the centre on focus, a halo marks the focus bounds, and errors announce themselves.
 * Paste and password managers are never blocked (accessible authentication).
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun EmpatiTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    supportingText: String? = null,
    errorText: String? = null,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    valueStyle: TextStyle = MaterialTheme.typography.bodyLarge,
    contentType: ContentType? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val focusRequester = remember { FocusRequester() }
    val hasError = errorText != null
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.medium

    val accent by animateColorAsState(
        targetValue = when {
            hasError -> colors.error
            focused -> colors.primary
            else -> colors.onSurfaceVariant
        },
        animationSpec = EmpatiMotion.quick(),
        label = "fieldAccent"
    )
    val growth by animateFloatAsState(
        targetValue = if (focused || hasError) 1f else 0f,
        animationSpec = EmpatiMotion.standard(),
        label = "fieldIndicator"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = EmpatiMotion.quick(),
        label = "fieldHalo"
    )
    val indicatorColor = if (hasError) colors.error else colors.primary

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(EmpatiSurfaces.inset())
                .border(2.dp, colors.primary.copy(alpha = 0.30f * haloAlpha), shape)
                .pointerInput(enabled) {
                    detectTapGestures { if (enabled) focusRequester.requestFocus() }
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = if (trailing != null) 4.dp else 16.dp)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(22.dp)
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(top = 12.dp, bottom = 14.dp)
                        // Merge the visible label into the field's node so screen readers announce "label, value".
                        .semantics(mergeDescendants = true) {}
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = accent
                    )
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        textStyle = valueStyle.copy(color = colors.onSurface),
                        cursorBrush = SolidColor(colors.primary),
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        visualTransformation = visualTransformation,
                        interactionSource = interactionSource,
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .semantics {
                                if (contentType != null) this.contentType = contentType
                                if (errorText != null) error(errorText)
                            },
                        decorationBox = { inner ->
                            Box(Modifier.padding(top = 2.dp)) {
                                if (value.isEmpty() && placeholder != null) {
                                    Text(
                                        text = placeholder,
                                        style = valueStyle,
                                        color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                inner()
                            }
                        }
                    )
                }
                trailing?.invoke()
            }
            // resting baseline + focus indicator that grows from the centre
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(colors.outline.copy(alpha = 0.55f))
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(growth)
                    .height(2.5.dp)
                    .background(indicatorColor)
            )
        }

        AnimatedVisibility(
            visible = hasError || supportingText != null,
            enter = fadeIn(EmpatiMotion.quick()) + expandVertically(EmpatiMotion.quick()),
            exit = fadeOut(EmpatiMotion.quick()) + shrinkVertically(EmpatiMotion.quick())
        ) {
            val message = errorText ?: supportingText.orEmpty()
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier
                    .padding(start = 16.dp, end = 16.dp, top = 8.dp)
                    .then(
                        if (hasError) Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        else Modifier
                    )
            ) {
                if (hasError) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = colors.error,
                        modifier = Modifier
                            .padding(top = 2.dp, end = 6.dp)
                            .size(14.dp)
                    )
                }
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hasError) colors.error else colors.onSurfaceVariant
                )
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Identity visuals
// ═══════════════════════════════════════════════════════════════════════════

/**
 * Logo resmi Empati (hati dan balon percakapan).
 * Mendukung varian tanpa background ([withBackground] = false, default untuk Splash & Auth)
 * maupun dengan background ([withBackground] = true, untuk Avatar & Badge).
 */
@Composable
fun EmpatiLogo(
    size: Dp,
    animated: Boolean = false,
    withBackground: Boolean = false,
    modifier: Modifier = Modifier
) {
    val reduced = LocalReducedMotion.current
    val scale = if (animated && !reduced) {
        val transition = rememberInfiniteTransition(label = "logoTransition")
        val drift by transition.animateFloat(
            initialValue = 0.96f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(
                animation = tween(3200, easing = EmpatiMotion.EaseInOutSoft),
                repeatMode = RepeatMode.Reverse
            ),
            label = "logoDrift"
        )
        drift
    } else {
        1f
    }

    val resId = if (withBackground) {
        R.drawable.srya_emora_logo_bg
    } else {
        R.drawable.srya_emora_logo_transparent
    }

    Image(
        painter = painterResource(id = resId),
        contentDescription = "Logo Empati",
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    )
}

@Composable
fun EmpatiAvatar(initials: String, size: Dp = 44.dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.22f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontFamily = DisplayFont,
            fontWeight = FontWeight.Medium,
            fontSize = with(androidx.compose.ui.platform.LocalDensity.current) { (size * 0.38f).toSp() }
        )
    }
}

/**
 * Affective-state orb: aura, progress ring and a calm core that carries one icon.
 * The ring draws itself on first composition (result reveal) and the aura breathes.
 * Use it to express *how the result feels*, never to display raw numbers.
 */
@Composable
fun StateOrb(
    tone: ToneColors,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    size: Dp = 132.dp
) {
    val reduced = LocalReducedMotion.current
    val reveal = remember { Animatable(if (reduced) 1f else 0f) }
    LaunchedEffect(reduced) {
        if (reduced) {
            reveal.snapTo(1f)
        } else {
            reveal.animateTo(1f, tween(EmpatiMotion.Reveal + 400, easing = EmpatiMotion.EaseOutSoft))
        }
    }
    val breathe by rememberInfiniteTransition(label = "orbBreath").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = EmpatiMotion.EaseInOutSoft),
            repeatMode = RepeatMode.Reverse
        ),
        label = "orbBreathValue"
    )
    val phase = if (reduced) 0.5f else breathe

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val c = center
            val half = this.size.minDimension / 2f
            val auraRadius = half * (0.94f + 0.06f * phase)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(tone.accent.copy(alpha = 0.30f * reveal.value), Color.Transparent),
                    center = c,
                    radius = auraRadius
                ),
                radius = auraRadius,
                center = c
            )
            val ringRadius = half * 0.66f
            val topLeft = Offset(c.x - ringRadius, c.y - ringRadius)
            val ringSize = Size(ringRadius * 2f, ringRadius * 2f)
            drawArc(
                color = tone.accent.copy(alpha = 0.20f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = 2.dp.toPx())
            )
            drawArc(
                color = tone.accent,
                startAngle = -90f,
                sweepAngle = 360f * reveal.value,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
            )
        }
        Box(
            modifier = Modifier
                .size(size * 0.44f)
                .graphicsLayer {
                    val s = 0.86f + 0.14f * reveal.value
                    scaleX = s
                    scaleY = s
                    alpha = reveal.value
                }
                .background(tone.container, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier.size(size * 0.2f)
            )
        }
    }
}

/** Three softly pulsing dots: "the companion is thinking". */
@Composable
fun BreathingDots(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "dots")
    Row(
        modifier = modifier.semantics { contentDescription = "Sedang menyusun jawaban" },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val level by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(640, easing = EmpatiMotion.EaseInOutSoft),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 170)
                ),
                label = "dot$index"
            )
            val shown = if (reduced) 0.7f else level
            Box(
                Modifier
                    .size(7.dp)
                    .graphicsLayer {
                        alpha = shown
                        scaleX = 0.75f + 0.25f * shown
                        scaleY = 0.75f + 0.25f * shown
                    }
                    .background(color, CircleShape)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Screen scaffolding
// ═══════════════════════════════════════════════════════════════════════════

enum class ScreenInsets { Safe, StatusBar, None }

/**
 * Standard page frame: canvas colour, generous gutters, inset handling and a readable max width
 * so content stays comfortable on tablets and in landscape.
 */
@Composable
fun ScreenFrame(
    modifier: Modifier = Modifier,
    insets: ScreenInsets = ScreenInsets.Safe,
    scrollable: Boolean = true,
    horizontalPadding: Dp = EmpatiSpace.gutter,
    content: @Composable ColumnScope.() -> Unit
) {
    val insetModifier = when (insets) {
        ScreenInsets.Safe -> Modifier.safeDrawingPadding()
        ScreenInsets.StatusBar -> Modifier.statusBarsPadding()
        ScreenInsets.None -> Modifier
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(insetModifier),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EmpatiSpace.readableWidth)
                .fillMaxWidth()
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(horizontal = horizontalPadding),
            content = content
        )
    }
}

/** Top row: optional back button and an optional trailing element, with a guaranteed 48dp rhythm. */
@Composable
fun ScreenTopBar(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = EmpatiSpace.touchTarget)
            .padding(top = EmpatiSpace.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (onBack != null) BackIconButton(onClick = onBack) else Spacer(Modifier.size(1.dp))
        trailing?.invoke()
    }
}

/** Eyebrow · expressive title · supporting line. The standard top-of-screen voice. */
@Composable
fun ScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    subtitle: String? = null,
    titleStyle: TextStyle = MaterialTheme.typography.headlineLarge
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (eyebrow != null) {
            Text(
                text = eyebrow,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(EmpatiSpace.xs))
        }
        Text(
            text = title,
            style = titleStyle,
            color = MaterialTheme.colorScheme.onBackground
        )
        if (subtitle != null) {
            Spacer(Modifier.height(EmpatiSpace.xs))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** "FAKTOR KARDIOVASKULAR · GAYA HIDUP" → "Faktor kardiovaskular · gaya hidup" (keeps the user-facing voice conversational). */
fun String.toSentenceCase(): String = lowercase()
    .split(" · ")
    .joinToString(" · ") { part -> part.replaceFirstChar { it.uppercase() } }

fun greetingForHour(hour: Int): String = when (hour) {
    in 5..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..18 -> "Selamat sore"
    else -> "Selamat malam"
}
