package com.example.empati.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.empati.R

/*
 * EMPATI · "Soft Daylight" typography (v4)
 *
 *   DisplayFont  Fraunces (soft, optical-size aware serif)  – human moments: greetings, headlines,
 *                                                              result statements, the app name.
 *   BodyFont     Inter                                       – everything you read or tap: titles,
 *                                                              body, labels, buttons, chips.
 *   MetricFont   JetBrains Mono                              – RAW DATA ONLY: account identifiers,
 *                                                              source/citation codes. Never labels.
 *
 * Labels are sentence-case (no forced ALL-CAPS, no monospace) to keep the tone conversational.
 * Body copy uses a ~1.6 line-height ratio for breathing room.
 */

@OptIn(ExperimentalTextApi::class)
private fun variableFont(resId: Int, weight: FontWeight): Font = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight))
)

/** Fraunces: weight + high optical size (refined contrast) + SOFT axis (rounded, organic terminals). */
@OptIn(ExperimentalTextApi::class)
private fun displayFont(weight: FontWeight): Font = Font(
    resId = R.font.fraunces,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(weight.weight),
        FontVariation.Setting("opsz", 72f),
        FontVariation.Setting("SOFT", 100f),
        FontVariation.Setting("WONK", 0f)
    )
)

val DisplayFont = FontFamily(
    displayFont(FontWeight.Normal),
    displayFont(FontWeight.Medium),
    displayFont(FontWeight.SemiBold)
)

val BodyFont = FontFamily(
    variableFont(R.font.inter, FontWeight.Normal),
    variableFont(R.font.inter, FontWeight.Medium),
    variableFont(R.font.inter, FontWeight.SemiBold),
    variableFont(R.font.inter, FontWeight.Bold)
)

val MetricFont = FontFamily(
    variableFont(R.font.jetbrains_mono, FontWeight.Medium),
    variableFont(R.font.jetbrains_mono, FontWeight.SemiBold)
)

private fun display(size: TextUnit, line: TextUnit, tracking: TextUnit, weight: FontWeight) = TextStyle(
    fontFamily = DisplayFont,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = tracking
)

private fun ui(
    size: TextUnit,
    line: TextUnit,
    weight: FontWeight,
    tracking: TextUnit = 0.sp
) = TextStyle(
    fontFamily = BodyFont,
    fontWeight = weight,
    fontSize = size,
    lineHeight = line,
    letterSpacing = tracking
)

val EmpatiTypography = Typography(
    // Expressive voice
    displayLarge = display(48.sp, 54.sp, (-1.0).sp, FontWeight.Normal),
    displayMedium = display(40.sp, 46.sp, (-0.8).sp, FontWeight.Normal),
    displaySmall = display(34.sp, 40.sp, (-0.6).sp, FontWeight.Normal),
    headlineLarge = display(30.sp, 38.sp, (-0.4).sp, FontWeight.Medium),
    headlineMedium = display(26.sp, 34.sp, (-0.3).sp, FontWeight.Medium),
    headlineSmall = display(22.sp, 30.sp, (-0.2).sp, FontWeight.Medium),
    // Functional voice
    titleLarge = ui(20.sp, 28.sp, FontWeight.SemiBold, (-0.2).sp),
    titleMedium = ui(16.sp, 24.sp, FontWeight.SemiBold, 0.sp),
    titleSmall = ui(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    bodyLarge = ui(16.sp, 26.sp, FontWeight.Normal, 0.1.sp),
    bodyMedium = ui(14.sp, 22.sp, FontWeight.Normal, 0.1.sp),
    bodySmall = ui(12.sp, 18.sp, FontWeight.Normal, 0.2.sp),
    labelLarge = ui(14.sp, 20.sp, FontWeight.SemiBold, 0.1.sp),
    labelMedium = ui(12.sp, 16.sp, FontWeight.Medium, 0.2.sp),
    labelSmall = ui(11.sp, 16.sp, FontWeight.Medium, 0.3.sp)
)

/** Raw telemetry / identifier text (account IDs, source codes). Use sparingly. */
val EmpatiMetricStyle = TextStyle(
    fontFamily = MetricFont,
    fontWeight = FontWeight.Medium,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    letterSpacing = 0.2.sp
)

/** Step counters and other changing numerals: proportional Inter with tabular figures so digits never jitter. */
val EmpatiNumeralStyle = TextStyle(
    fontFamily = BodyFont,
    fontWeight = FontWeight.SemiBold,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontFeatureSettings = "tnum"
)
