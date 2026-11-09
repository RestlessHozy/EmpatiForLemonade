package com.example.empati.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.FactCheck
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.empati.EmpatiViewModel
import com.example.empati.Screen
import com.example.empati.data.RiskLevel
import com.example.empati.data.UserProfile
import com.example.empati.data.initials
import com.example.empati.data.label
import com.example.empati.data.presentation.UserRiskSummary
import com.example.empati.data.statusLabel
import com.example.empati.ui.components.EmpatiAvatar
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.ScreenInsets
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.SoftIconTile
import com.example.empati.ui.components.StateOrb
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.ToneBadge
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.greetingForHour
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.components.toSentenceCase
import com.example.empati.ui.components.tone
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.ToneColors
import com.example.empati.ui.theme.empati
import java.util.Calendar

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry point (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val lastResult = vm.userHistory().firstOrNull()

    HomeScreenContent(
        profile = vm.profile,
        lastResult = lastResult,
        hour = hour,
        onStartScreening = { vm.navigateTo(Screen.Consent) },
        onOpenHistory = { vm.selectTab(Screen.History) },
        onOpenChat = { vm.selectTab(Screen.Chat) },
        onOpenProfile = { vm.selectTab(Screen.Profile) },
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HomeScreenContent(
    profile: UserProfile,
    lastResult: UserRiskSummary?,
    hour: Int,
    onStartScreening: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenChat: () -> Unit,
    onOpenProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenFrame(modifier = modifier, insets = ScreenInsets.StatusBar) {
        Spacer(Modifier.height(EmpatiSpace.md))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 0) {
            HomeHeader(profile = profile, hour = hour, onOpenProfile = onOpenProfile)
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 1) {
            if (lastResult == null) {
                EmptyStateHero(onStart = onStartScreening)
            } else {
                LastResultHero(
                    result = lastResult,
                    onRetake = onStartScreening,
                    onOpenHistory = onOpenHistory
                )
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xxl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 2) {
            ServicesSection(onStartScreening = onStartScreening, onOpenChat = onOpenChat)
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(modifier = Modifier.fillMaxWidth(), index = 3) {
            SupportStrip(onClick = onOpenChat)
        }

        Spacer(Modifier.height(EmpatiSpace.huge))
    }
}

// ── Header ──────────────────────────────────────────────────────────────────

@Composable
private fun HomeHeader(profile: UserProfile, hour: Int, onOpenProfile: () -> Unit) {
    val firstName = profile.name.trim().substringBefore(' ').ifBlank { profile.name }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.md)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "${greetingForHour(hour)},",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(EmpatiSpace.xxs))
            Text(
                text = firstName,
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(EmpatiSpace.xs))
            Text(
                text = "${profile.jabatan} · ${profile.instansi}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        ProfileAvatarButton(profile = profile, onClick = onOpenProfile)
    }
}

@Composable
private fun ProfileAvatarButton(profile: UserProfile, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .size(56.dp)
            .pressScale(interactionSource, 0.94f)
            .focusHalo(interactionSource, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Buka profil",
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.clearAndSetSemantics {
                contentDescription = "Profil ${profile.name}"
            }
        ) {
            EmpatiAvatar(initials = profile.initials(), size = 52.dp)
        }
    }
}

// ── Hero ────────────────────────────────────────────────────────────────────

private fun riskDescription(level: RiskLevel): String = when (level) {
    RiskLevel.LOW ->
        "Kondisi tekanan darah Anda saat ini terjaga dengan baik. Pertahankan kebiasaan sehat dan cek tensi secara berkala."
    RiskLevel.MEDIUM ->
        "Ada beberapa kebiasaan atau kondisi harian yang mulai perlu diperhatikan. Jaga pola makan, istirahat, dan kelola pikiran."
    RiskLevel.HIGH ->
        "Terdapat beberapa faktor risiko yang patut mendapat perhatian. Sebaiknya jadwalkan pemeriksaan tensi di fasilitas kesehatan."
    RiskLevel.VERY_HIGH ->
        "Tanda risiko Anda tergolong tinggi. Sangat disarankan segera memeriksakan tekanan darah dan berkonsultasi dengan dokter."
}

/** "Rendah · Batas Wajar" → "Batas wajar" (the qualitative half of the status label). */
private fun RiskLevel.statusPhrase(): String =
    statusLabel.substringAfter(" · ", missingDelimiterValue = statusLabel).toSentenceCase()

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LastResultHero(
    result: UserRiskSummary,
    onRetake: () -> Unit,
    onOpenHistory: () -> Unit
) {
    val tone = result.level.tone()
    TonalCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StateOrb(
                tone = tone,
                icon = if (result.level == RiskLevel.LOW) Icons.Rounded.Spa else Icons.Rounded.MonitorHeart,
                size = 88.dp
            )
            Spacer(Modifier.width(EmpatiSpace.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Skrining terakhir",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = result.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(EmpatiSpace.sm))
                ToneBadge(label = result.level.statusPhrase(), tone = tone)
            }
        }

        Spacer(Modifier.height(EmpatiSpace.lg))

        Text(
            text = "Risiko ${result.level.label.lowercase()}",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(EmpatiSpace.xs))
        Text(
            text = riskDescription(result.level),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(EmpatiSpace.xl))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
            verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
        ) {
            QuietAction(
                text = "Skrining ulang",
                onClick = onRetake,
                leadingIcon = Icons.Rounded.Refresh,
                container = EmpatiSurfaces.inset(),
                content = MaterialTheme.colorScheme.onSurface
            )
            QuietAction(
                text = "Lihat riwayat",
                onClick = onOpenHistory,
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
                container = Color.Transparent,
                content = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun EmptyStateHero(onStart: () -> Unit) {
    TonalCard(elevated = true, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            SectionChip(text = "Skrining mandiri")
            StateOrb(
                tone = MaterialTheme.empati.calm,
                icon = Icons.Rounded.MonitorHeart,
                size = 80.dp
            )
        }
        Spacer(Modifier.height(EmpatiSpace.md))
        Text(
            text = "Kenali kondisi kesehatan Anda lebih dini",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        Text(
            text = "Kenali kondisi kesehatan dan ketenangan pikiran Anda melalui kuesioner singkat yang rahasia dan nyaman.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(EmpatiSpace.xl))
        PrimaryButton(text = "Mulai skrining sekarang", onClick = onStart)
    }
}

/** Quiet pill action: tonal or text-only, 48dp target, soft press. */
@Composable
private fun QuietAction(
    text: String,
    onClick: () -> Unit,
    container: Color,
    content: Color,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = container,
        contentColor = content,
        modifier = modifier
            .heightIn(min = EmpatiSpace.touchTarget)
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = content
            )
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

// ── Services ────────────────────────────────────────────────────────────────

@Composable
private fun ServicesSection(onStartScreening: () -> Unit, onOpenChat: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Layanan utama",
            style = MaterialTheme.typography.titleLarge,
            color = colors.onBackground
        )
        Spacer(Modifier.height(EmpatiSpace.md))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.md)
        ) {
            FeatureCard(
                title = "Mulai skrining",
                subtitle = "Cek kesehatan & batin",
                icon = Icons.AutoMirrored.Rounded.FactCheck,
                tone = ToneColors(
                    accent = colors.primary,
                    container = colors.primaryContainer,
                    onContainer = colors.onPrimaryContainer
                ),
                onClick = onStartScreening,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
            FeatureCard(
                title = "Tanya SRYA Emora",
                subtitle = "Teman cerita & tanya",
                icon = Icons.Rounded.ChatBubbleOutline,
                tone = MaterialTheme.empati.presence,
                onClick = onOpenChat,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            )
        }
    }
}

@Composable
private fun FeatureCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    tone: ToneColors,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TonalCard(
        onClick = onClick,
        contentPadding = EmpatiSpace.cardPaddingCompact,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            SoftIconTile(
                icon = icon,
                size = 48.dp,
                container = tone.container,
                tint = tone.onContainer
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = tone.accent,
                modifier = Modifier
                    .padding(top = EmpatiSpace.xxs)
                    .size(18.dp)
            )
        }
        Spacer(Modifier.height(EmpatiSpace.lg))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(EmpatiSpace.xxs))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ── Support ─────────────────────────────────────────────────────────────────

@Composable
private fun SupportStrip(onClick: () -> Unit) {
    val tone = MaterialTheme.empati.presence
    val shape = MaterialTheme.shapes.medium
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = shape,
        color = tone.container,
        contentColor = tone.onContainer,
        modifier = Modifier
            .fillMaxWidth()
            .pressScale(interactionSource, 0.985f)
            .focusHalo(interactionSource, shape)
    ) {
        Row(
            modifier = Modifier.padding(EmpatiSpace.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SoftIconTile(
                icon = Icons.Rounded.SupportAgent,
                size = 44.dp,
                container = EmpatiSurfaces.card(),
                tint = tone.accent
            )
            Spacer(Modifier.width(EmpatiSpace.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = tone.onContainer,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ruang aman dan rujukan SAPA 129",
                        style = MaterialTheme.typography.labelLarge,
                        color = tone.onContainer
                    )
                }
                Spacer(Modifier.height(EmpatiSpace.xxs))
                Text(
                    text = "Privasi Anda terlindungi enkripsi satu arah. Butuh teman bicara atau bantuan? Hubungi SAPA 129 atau ngobrol dengan SRYA Emora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = tone.onContainer
                )
            }
            Spacer(Modifier.width(EmpatiSpace.sm))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
