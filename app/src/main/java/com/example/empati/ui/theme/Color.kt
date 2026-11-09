package com.example.empati.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * EMPATI · "Soft Daylight" colour architecture (v4)
 *
 * Three layers, strictly one-directional:
 *   1. Palettes  – raw tonal ramps (this file, top). Never referenced from UI code.
 *   2. Roles     – Material 3 ColorScheme built in Theme.kt (primary, surfaceContainer…).
 *   3. Semantics – EmpatiColors below: affective-state tones (calm / attentive / concerned /
 *                  urgent / neutral) exposed through `MaterialTheme.empati`.
 *
 * Hue story: Sage (calm, brand) · Clay (human warmth) · Mist (AI presence) · Stone (neutrals).
 * Every text/background pair used by the UI is >= 4.5:1; every boundary that identifies a
 * control (outline) is >= 3:1 against its surface.
 */

// ── Sage · primary ─────────────────────────────────────────────────────────────
internal val Sage10 = Color(0xFF05241B)
internal val Sage20 = Color(0xFF0A3A2C)
internal val Sage30 = Color(0xFF1F4F40)
internal val Sage40 = Color(0xFF2E6A57)
internal val Sage80 = Color(0xFF9CCFB8)
internal val Sage90 = Color(0xFFBAEDD6)
internal val Sage95 = Color(0xFFD6EEE3)
internal val Sage98 = Color(0xFFEAF6F0)

// ── Clay · secondary (human warmth) ───────────────────────────────────────────
internal val Clay10 = Color(0xFF3B1808)
internal val Clay30 = Color(0xFF6E3A25)
internal val Clay40 = Color(0xFF93482E)
internal val Clay80 = Color(0xFFF2B79C)
internal val Clay90 = Color(0xFFFFDBCB)
internal val Clay95 = Color(0xFFFBE4D8)

// ── Mist · tertiary (AI presence, support, information) ───────────────────────
internal val Mist10 = Color(0xFF1B1948)
internal val Mist30 = Color(0xFF42407F)
internal val Mist40 = Color(0xFF5A5896)
internal val Mist80 = Color(0xFFC6C4F4)
internal val Mist90 = Color(0xFFE3E1FA)
internal val Mist95 = Color(0xFFF0EFFD)

// ── Honey · "attentive" state ─────────────────────────────────────────────────
internal val Honey10 = Color(0xFF3D2800)
internal val Honey40 = Color(0xFF85570A)
internal val Honey60 = Color(0xFFD29A2E)
internal val Honey80 = Color(0xFFF3C56A)
internal val Honey90 = Color(0xFFFBE8BE)
internal val Honey25 = Color(0xFF5C3C00)

// ── Terracotta · "concerned" state / error ────────────────────────────────────
internal val Terra10 = Color(0xFF400A02)
internal val Terra25 = Color(0xFF6E2415)
internal val Terra40 = Color(0xFFA8402B)
internal val Terra80 = Color(0xFFFFB4A3)
internal val Terra90 = Color(0xFFFFDAD1)
internal val Terra95 = Color(0xFFFCE6E0)

// ── Berry · "urgent" state ────────────────────────────────────────────────────
internal val Berry10 = Color(0xFF3F0A22)
internal val Berry25 = Color(0xFF68183B)
internal val Berry40 = Color(0xFF8E2F55)
internal val Berry80 = Color(0xFFF2A9C2)
internal val Berry90 = Color(0xFFF9D9E4)

// ── Stone · neutrals (green-tinted warm greys) ────────────────────────────────
internal val StoneLightBackground = Color(0xFFF7F5F0)
internal val StoneLightBright = Color(0xFFFBFAF6)
internal val StoneLightDim = Color(0xFFDFDCD3)
internal val StoneLightLowest = Color(0xFFFFFFFF)
internal val StoneLightLow = Color(0xFFF2F0EA)
internal val StoneLightMid = Color(0xFFEDEAE3)
internal val StoneLightHigh = Color(0xFFE7E4DC)
internal val StoneLightHighest = Color(0xFFE1DED5)
internal val StoneLightOnSurface = Color(0xFF1B1C19)
internal val StoneLightOnVariant = Color(0xFF474B44)
internal val StoneLightOutline = Color(0xFF747970)
internal val StoneLightOutlineVariant = Color(0xFFD0D3C9)
internal val StoneLightInverse = Color(0xFF2F312D)
internal val StoneLightInverseOn = Color(0xFFF1F0EA)

internal val StoneDarkBackground = Color(0xFF121512)
internal val StoneDarkBright = Color(0xFF383B37)
internal val StoneDarkDim = Color(0xFF0E110E)
internal val StoneDarkLowest = Color(0xFF0C0F0C)
internal val StoneDarkLow = Color(0xFF181B18)
internal val StoneDarkMid = Color(0xFF1C1F1C)
internal val StoneDarkHigh = Color(0xFF262A26)
internal val StoneDarkHighest = Color(0xFF313531)
internal val StoneDarkOnSurface = Color(0xFFE3E3DB)
internal val StoneDarkOnVariant = Color(0xFFBEC3B9)
internal val StoneDarkOutline = Color(0xFF889084)
internal val StoneDarkOutlineVariant = Color(0xFF40443F)

// ── Semantic layer ────────────────────────────────────────────────────────────

/** One affective state: a vivid [accent] (dots, rings, icons), a soft [container] and readable [onContainer]. */
@Immutable
data class ToneColors(
    val accent: Color,
    val container: Color,
    val onContainer: Color
)

/**
 * Expressive, state-bearing colours that Material's fixed roles cannot express.
 *
 * - [calm]       low risk / positive / protective factors
 * - [attentive]  medium risk / worth monitoring
 * - [concerned]  high risk / needs attention
 * - [urgent]     very high risk / seek care soon
 * - [neutral]    incomplete or informational states
 * - [presence]   AI companion identity (chat, "thinking" states)
 * - [aura]       soft glow colour used behind state orbs (never as a page background)
 * - [shadowTint] ambient-shadow colour for floating elements
 */
@Immutable
data class EmpatiColors(
    val calm: ToneColors,
    val attentive: ToneColors,
    val concerned: ToneColors,
    val urgent: ToneColors,
    val neutral: ToneColors,
    val presence: ToneColors,
    val aura: Color,
    val shadowTint: Color,
    val isDark: Boolean
)

internal val LightEmpatiColors = EmpatiColors(
    calm = ToneColors(accent = Sage40, container = Sage95, onContainer = Sage20),
    attentive = ToneColors(accent = Honey60, container = Honey90, onContainer = Honey25),
    concerned = ToneColors(accent = Terra40, container = Terra95, onContainer = Terra25),
    urgent = ToneColors(accent = Berry40, container = Berry90, onContainer = Berry25),
    neutral = ToneColors(accent = StoneLightOutline, container = StoneLightHigh, onContainer = StoneLightOnVariant),
    presence = ToneColors(accent = Mist40, container = Mist90, onContainer = Mist10),
    aura = Sage80,
    shadowTint = Sage20,
    isDark = false
)

internal val DarkEmpatiColors = EmpatiColors(
    calm = ToneColors(accent = Sage80, container = Sage30, onContainer = Sage90),
    attentive = ToneColors(accent = Honey80, container = Color(0xFF4A3306), onContainer = Honey90),
    concerned = ToneColors(accent = Terra80, container = Color(0xFF5A2315), onContainer = Terra90),
    urgent = ToneColors(accent = Berry80, container = Color(0xFF5A1832), onContainer = Berry90),
    neutral = ToneColors(accent = StoneDarkOutline, container = StoneDarkHighest, onContainer = StoneDarkOnVariant),
    presence = ToneColors(accent = Mist80, container = Mist30, onContainer = Mist90),
    aura = Sage30,
    shadowTint = Color.Black,
    isDark = true
)
