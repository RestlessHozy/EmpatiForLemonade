package com.example.empati.ui.theme

import android.provider.Settings
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** 4dp base grid. Vertical rhythm tiers: 8 (inline) · 16 (group) · 24 (section) · 40 (chapter). */
object EmpatiSpace {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val huge = 40.dp
    val giant = 56.dp

    /** Horizontal page margin: generous on purpose. */
    val gutter = 24.dp

    /** Long-form content never stretches beyond this on tablets / landscape. */
    val readableWidth = 560.dp

    /** Minimum interactive size (Material / WCAG 2.2 target size). */
    val touchTarget = 48.dp

    val cardPadding = PaddingValues(24.dp)
    val cardPaddingCompact = PaddingValues(18.dp)
}

/** Icon size tokens: one size per hierarchy level. */
object EmpatiIcon {
    val sm = 16.dp
    val md = 20.dp
    val lg = 24.dp
    val xl = 32.dp
}

/** Elevation is mostly tonal. Real shadows are reserved for elements that float above content. */
object EmpatiElevation {
    val resting = 0.dp
    val raised = 3.dp
    val floating = 10.dp
}

/**
 * Motion language: unhurried, decelerating, never bouncy on text.
 * Entrances are longer than exits; state feedback is instant (<= 160ms).
 */
object EmpatiMotion {
    const val Instant = 90
    const val Quick = 160
    const val Standard = 280
    const val Expressive = 520
    const val Reveal = 720

    /** Gentle deceleration: things arrive and settle. */
    val EaseOutSoft = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)

    /** Symmetric, for breathing / looping motion. */
    val EaseInOutSoft = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)

    fun <T> quick(): FiniteAnimationSpec<T> = tween(Quick, easing = EaseOutSoft)
    fun <T> standard(): FiniteAnimationSpec<T> = tween(Standard, easing = EaseOutSoft)
    fun <T> expressive(): FiniteAnimationSpec<T> = tween(Expressive, easing = EaseOutSoft)

    /** Tactile press recovery (scale). Slight overshoot, high damping: feels like soft material. */
    fun <T> press(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Settling for size/position changes (progress, indicators). */
    fun <T> settle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessLow
    )
}

/**
 * When true, decorative motion (breathing, staggered reveal, pulsing) collapses to its end state.
 * Provided by [EmpatiTheme]; derived from the system "Remove animations" setting by default.
 */
val LocalReducedMotion = compositionLocalOf { false }

@Composable
fun rememberSystemReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE,
                1f
            ) == 0f
        }.getOrDefault(false)
    }
}

/**
 * Soft ambient shadow, tinted with the brand shadow colour instead of neutral black.
 * Only for floating elements (primary action, bottom bar, composer, bottom sheets).
 */
fun Modifier.softShadow(
    elevation: Dp,
    shape: Shape,
    tint: Color
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    clip = false,
    ambientColor = tint.copy(alpha = 0.16f),
    spotColor = tint.copy(alpha = 0.30f)
)
