package com.example.empati.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Assignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.empati.EmpatiViewModel
import com.example.empati.Screen
import com.example.empati.data.RiskLevel
import com.example.empati.data.label
import com.example.empati.data.presentation.UserRiskSummary
import com.example.empati.data.statusLabel
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.ScreenHeader
import com.example.empati.ui.components.ScreenInsets
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.StateOrb
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.ToneBadge
import com.example.empati.ui.components.toSentenceCase
import com.example.empati.ui.components.tone
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.empati

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry point (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun HistoryScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    HistoryScreenContent(
        items = vm.userHistory(),
        onStart = { vm.navigateTo(Screen.Consent) },
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content
// ═══════════════════════════════════════════════════════════════════════════

/** [items] are ordered newest first; the first one is gently emphasised. */
@Composable
fun HistoryScreenContent(
    items: List<UserRiskSummary>,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenFrame(modifier = modifier, insets = ScreenInsets.StatusBar) {
        Spacer(Modifier.height(EmpatiSpace.md))
        Reveal(modifier = Modifier.fillMaxWidth(), index = 0) {
            ScreenHeader(
                title = "Riwayat skrining",
                eyebrow = if (items.isEmpty()) null else "${items.size} catatan",
                subtitle = "Catatan perjalanan kesehatan yang pernah Anda lakukan"
            )
        }
        Spacer(Modifier.height(EmpatiSpace.xl))

        if (items.isEmpty()) {
            Reveal(modifier = Modifier.fillMaxWidth(), index = 1) {
                EmptyHistory(onStart = onStart)
            }
        } else {
            items.forEachIndexed { index, item ->
                Reveal(modifier = Modifier.fillMaxWidth(), index = (index + 1).coerceAtMost(6)) {
                    TimelineEntry(
                        result = item,
                        isLatest = index == 0,
                        isFirst = index == 0,
                        isLast = index == items.lastIndex
                    )
                }
            }
        }

        Spacer(Modifier.height(EmpatiSpace.huge))
    }
}

private fun summaryNote(level: RiskLevel): String = when (level) {
    RiskLevel.LOW -> "Indikasi risiko hipertensi rendah (batas wajar)"
    RiskLevel.MEDIUM -> "Beberapa faktor risiko perlu pemantauan berkala"
    RiskLevel.HIGH -> "Perlu perhatian — disarankan pemeriksaan tekanan darah"
    RiskLevel.VERY_HIGH -> "Perlu perhatian — disarankan segera berkonsultasi ke dokter"
}

private val RailWidth = 24.dp
private val RailDotY = 30.dp

/** One entry on the timeline: a hairline rail with a tone-coloured dot, and the entry card beside it. */
@Composable
private fun TimelineEntry(
    result: UserRiskSummary,
    isLatest: Boolean,
    isFirst: Boolean,
    isLast: Boolean
) {
    val tone = result.level.tone()
    val railColor = EmpatiSurfaces.hairline()
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(RailWidth)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2f
                    val dotY = RailDotY.toPx()
                    val top = if (isFirst) dotY else 0f
                    val bottom = if (isLast) dotY else size.height
                    if (bottom > top) {
                        drawLine(
                            color = railColor,
                            start = Offset(x, top),
                            end = Offset(x, bottom),
                            strokeWidth = 2.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                    if (isLatest) {
                        drawCircle(
                            color = tone.accent.copy(alpha = 0.22f),
                            radius = 11.dp.toPx(),
                            center = Offset(x, dotY)
                        )
                    }
                    drawCircle(
                        color = tone.accent,
                        radius = if (isLatest) 6.dp.toPx() else 4.5.dp.toPx(),
                        center = Offset(x, dotY)
                    )
                }
        )
        Spacer(Modifier.width(EmpatiSpace.sm))
        HistoryCard(
            result = result,
            isLatest = isLatest,
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else EmpatiSpace.md)
        )
    }
}

@Composable
private fun HistoryCard(
    result: UserRiskSummary,
    isLatest: Boolean,
    modifier: Modifier = Modifier
) {
    val tone = result.level.tone()
    val textColor = if (isLatest) tone.onContainer else MaterialTheme.colorScheme.onSurface
    val quietColor = if (isLatest) tone.onContainer else MaterialTheme.colorScheme.onSurfaceVariant
    // On the emphasised card the badge sits on a lifted surface so it stays visible against the tinted container.
    val badgeTone = if (isLatest) tone.copy(container = EmpatiSurfaces.card()) else tone

    TonalCard(
        modifier = modifier.fillMaxWidth(),
        containerColor = if (isLatest) tone.container else EmpatiSurfaces.card(),
        borderColor = if (isLatest) Color.Transparent else EmpatiSurfaces.hairline(),
        contentPadding = EmpatiSpace.cardPaddingCompact
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Risiko ${result.level.label.lowercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = textColor
                )
                Text(
                    text = result.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = quietColor
                )
            }
            if (isLatest) {
                Spacer(Modifier.width(EmpatiSpace.xs))
                SectionChip(
                    text = "Terbaru",
                    containerColor = EmpatiSurfaces.card(),
                    contentColor = tone.onContainer
                )
            }
        }
        Spacer(Modifier.height(EmpatiSpace.sm))
        ToneBadge(
            label = result.level.statusLabel
                .substringAfter(" · ", missingDelimiterValue = result.level.statusLabel)
                .toSentenceCase(),
            tone = badgeTone
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        Text(
            text = summaryNote(result.level),
            style = MaterialTheme.typography.bodyMedium,
            color = quietColor
        )
    }
}

@Composable
private fun EmptyHistory(onStart: () -> Unit) {
    TonalCard(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = EmpatiSpace.xl, vertical = EmpatiSpace.xxl)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            StateOrb(
                tone = MaterialTheme.empati.calm,
                icon = Icons.AutoMirrored.Rounded.Assignment,
                size = 128.dp
            )
            Spacer(Modifier.height(EmpatiSpace.md))
            Text(
                text = "Belum ada catatan skrining",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(EmpatiSpace.sm))
            Text(
                text = "Setiap kali Anda menyelesaikan skrining, rangkuman hasilnya akan tersimpan rapi di sini agar mudah Anda pantau dari waktu ke waktu.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(EmpatiSpace.xl))
            PrimaryButton(
                text = "Mulai skrining pertama",
                onClick = onStart
            )
        }
    }
}
