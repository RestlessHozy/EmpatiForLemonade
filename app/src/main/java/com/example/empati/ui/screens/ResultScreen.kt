package com.example.empati.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.ripple
import com.example.empati.EmpatiViewModel
import com.example.empati.data.presentation.PresentationTone
import com.example.empati.data.presentation.UserIntegratedPresentation
import com.example.empati.data.presentation.UserModulePresentation
import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.GhostButton
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.Reveal
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.ScreenTopBar
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.SectionLabel
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftIconTile
import com.example.empati.ui.components.SoftNotice
import com.example.empati.ui.components.StateOrb
import com.example.empati.ui.components.TonalCard
import com.example.empati.ui.components.ToneBadge
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.components.toSentenceCase
import com.example.empati.ui.components.toneColors
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.ToneColors
import com.example.empati.ui.theme.empati

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry points (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun ModuleResultScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val presentation = vm.currentModulePresentation() ?: return
    ModuleResultContent(
        presentation = presentation,
        onBack = vm::onBack,
        onProceed = if (presentation.isHypertension) vm::proceedToGbv else vm::goToIntegrated,
        onAskAi = vm::askAboutResult,
        modifier = modifier
    )
}

@Composable
fun IntegratedResultScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val presentation = vm.integratedPresentation() ?: return
    IntegratedResultContent(
        presentation = presentation,
        onBack = vm::onBack,
        onSave = vm::saveDualResult,
        onAskAi = vm::askAboutResult,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ModuleResultContent(
    presentation: UserModulePresentation,
    onBack: () -> Unit,
    onProceed: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedTag: String? = null
) {
    val findings = if (presentation.isHypertension) presentation.factorsToNote
    else presentation.identifiedCategories
    var selectedTag by remember { mutableStateOf(initialSelectedTag) }
    val tone = presentation.tone.toneColors()

    ScreenFrame(modifier = modifier) {
        ScreenTopBar(
            onBack = onBack,
            trailing = {
                SectionChip(
                    text = if (presentation.isHypertension) "Modul hipertensi" else "Modul kesejahteraan",
                    containerColor = if (presentation.isHypertension) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.empati.presence.container,
                    contentColor = if (presentation.isHypertension) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.empati.presence.onContainer
                )
            }
        )

        Spacer(Modifier.height(EmpatiSpace.lg))

        // Hero: how this result *feels*, before the details.
        Reveal(index = 0) {
            ResultHero(
                tone = tone,
                icon = if (presentation.isHypertension) Icons.Rounded.MonitorHeart else Icons.Rounded.Shield,
                statusLabel = presentation.statusLabel,
                title = presentation.title,
                body = presentation.conclusion
            )
        }

        if (findings.isNotEmpty()) {
            Spacer(Modifier.height(EmpatiSpace.xl))
            Reveal(index = 1) {
                TonalCard(contentPadding = EmpatiSpace.cardPaddingCompact, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel(
                        text = if (presentation.isHypertension) "Yang perlu diperhatikan"
                        else "Yang teridentifikasi"
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Ketuk untuk memahami lebih lanjut.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(EmpatiSpace.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                        verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
                    ) {
                        findings.forEach { item ->
                            FindingChip(text = item, onClick = { selectedTag = item })
                        }
                    }
                }
            }
        }

        if (presentation.protectiveFactors.isNotEmpty()) {
            Spacer(Modifier.height(EmpatiSpace.md))
            Reveal(index = 2) {
                StrengthsCard(items = presentation.protectiveFactors)
            }
        }

        if (presentation.supportNote != null) {
            Spacer(Modifier.height(EmpatiSpace.md))
            Reveal(index = 3) {
                SoftNotice(
                    text = presentation.supportNote,
                    icon = Icons.Rounded.SupportAgent,
                    tone = MaterialTheme.empati.presence
                )
            }
        }

        Spacer(Modifier.height(EmpatiSpace.md))
        Reveal(index = 4) {
            NextStepsCard(title = "Langkah selanjutnya", items = presentation.recommendations)
        }

        if (!presentation.isComplete) {
            Spacer(Modifier.height(EmpatiSpace.md))
            SoftNotice(
                text = "Sebagian pertanyaan belum terjawab. Lengkapi agar hasilnya lebih tepat.",
                icon = Icons.Rounded.Info,
                tone = MaterialTheme.empati.neutral
            )
        }

        Spacer(Modifier.height(EmpatiSpace.xl))
        Reveal(index = 5) {
            PrimaryButton(
                text = if (presentation.isHypertension) "Lanjut ke modul kesejahteraan"
                else "Lihat ringkasan terpadu",
                onClick = onProceed
            )
        }

        Spacer(Modifier.height(EmpatiSpace.md))
        Disclaimer(text = presentation.disclaimer)
        Spacer(Modifier.height(EmpatiSpace.xl))
    }

    selectedTag?.let { tag ->
        FindingDetailSheet(
            tag = tag,
            onAskAi = {
                selectedTag = null
                onAskAi()
            },
            onDismiss = { selectedTag = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntegratedResultContent(
    presentation: UserIntegratedPresentation,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onAskAi: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedTag: String? = null
) {
    var selectedTag by remember { mutableStateOf(initialSelectedTag) }
    val tone = overallTone(presentation).let { overall ->
        when (overall) {
            PresentationTone.POSITIVE -> MaterialTheme.empati.calm
            PresentationTone.ATTENTION -> MaterialTheme.empati.attentive
            else -> MaterialTheme.empati.neutral
        }
    }

    ScreenFrame(modifier = modifier) {
        ScreenTopBar(
            onBack = onBack,
            trailing = { SectionChip(text = "Ringkasan akhir") }
        )

        Spacer(Modifier.height(EmpatiSpace.lg))

        Reveal(index = 0) {
            ResultHero(
                tone = tone,
                icon = Icons.Rounded.Spa,
                statusLabel = null,
                title = presentation.categoryLabel,
                body = presentation.summary
            )
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        Reveal(index = 1) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.sm)
            ) {
                ModuleStatusTile(
                    name = "Hipertensi",
                    icon = Icons.Rounded.MonitorHeart,
                    statusLabel = presentation.hypertension.statusLabel,
                    tone = presentation.hypertension.tone.toneColors(),
                    modifier = Modifier.weight(1f)
                )
                ModuleStatusTile(
                    name = "Kesejahteraan",
                    icon = Icons.Rounded.Shield,
                    statusLabel = presentation.gbv.statusLabel,
                    tone = presentation.gbv.tone.toneColors(),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(Modifier.height(EmpatiSpace.md))
        Reveal(index = 2) {
            SoftNotice(
                text = presentation.relationshipExplanation,
                icon = Icons.Rounded.Info,
                tone = MaterialTheme.empati.presence,
                title = "Bagaimana keduanya berkaitan"
            )
        }

        if (presentation.factors.isNotEmpty()) {
            Spacer(Modifier.height(EmpatiSpace.md))
            Reveal(index = 3) {
                TonalCard(contentPadding = EmpatiSpace.cardPaddingCompact, modifier = Modifier.fillMaxWidth()) {
                    SectionLabel("Faktor kunci")
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Ketuk untuk memahami lebih lanjut.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(EmpatiSpace.sm))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                        verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
                    ) {
                        presentation.factors.forEach { factor ->
                            FindingChip(text = factor, onClick = { selectedTag = factor })
                        }
                    }
                }
            }
        }

        if (presentation.protectiveFactors.isNotEmpty()) {
            Spacer(Modifier.height(EmpatiSpace.md))
            Reveal(index = 4) { StrengthsCard(items = presentation.protectiveFactors) }
        }

        Spacer(Modifier.height(EmpatiSpace.md))
        Reveal(index = 5) {
            NextStepsCard(title = "Rekomendasi utama", items = presentation.recommendations)
        }

        Spacer(Modifier.height(EmpatiSpace.xl))
        Reveal(index = 6) {
            Column {
                PrimaryButton(
                    text = "Simpan ke riwayat",
                    onClick = onSave,
                    trailingIcon = Icons.Rounded.BookmarkAdded
                )
                Spacer(Modifier.height(EmpatiSpace.xxs))
                GhostButton(text = "Tanyakan ke SRYA Emora", onClick = onAskAi)
            }
        }

        Spacer(Modifier.height(EmpatiSpace.sm))
        Disclaimer(text = presentation.disclaimer)
        Spacer(Modifier.height(EmpatiSpace.xl))
    }

    selectedTag?.let { tag ->
        FindingDetailSheet(
            tag = tag,
            onAskAi = {
                selectedTag = null
                onAskAi()
            },
            onDismiss = { selectedTag = null }
        )
    }
}

private fun overallTone(presentation: UserIntegratedPresentation): PresentationTone = when {
    presentation.isInconclusive -> PresentationTone.NEUTRAL
    presentation.hypertension.tone == PresentationTone.ATTENTION ||
        presentation.gbv.tone == PresentationTone.ATTENTION -> PresentationTone.ATTENTION
    presentation.hypertension.tone == PresentationTone.POSITIVE &&
        presentation.gbv.tone == PresentationTone.POSITIVE -> PresentationTone.POSITIVE
    else -> PresentationTone.NEUTRAL
}

// ═══════════════════════════════════════════════════════════════════════════
//  Building blocks
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ResultHero(
    tone: ToneColors,
    icon: ImageVector,
    statusLabel: String?,
    title: String,
    body: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StateOrb(tone = tone, icon = icon, size = 148.dp)
        Spacer(Modifier.height(EmpatiSpace.sm))
        if (statusLabel != null) {
            ToneBadge(label = statusLabel, tone = tone)
            Spacer(Modifier.height(EmpatiSpace.md))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        Text(
            text = body,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = EmpatiSpace.xs)
        )
    }
}

@Composable
private fun ModuleStatusTile(
    name: String,
    icon: ImageVector,
    statusLabel: String,
    tone: ToneColors,
    modifier: Modifier = Modifier
) {
    TonalCard(
        contentPadding = PaddingValues(EmpatiSpace.md),
        modifier = modifier
    ) {
        SoftIconTile(
            icon = icon,
            size = 40.dp,
            container = tone.container,
            tint = tone.onContainer
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        Text(
            text = name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(EmpatiSpace.xxs))
        Row(verticalAlignment = Alignment.Top) {
            Box(
                Modifier
                    .padding(top = 5.dp)
                    .size(8.dp)
                    .background(tone.accent, CircleShape)
            )
            Spacer(Modifier.width(EmpatiSpace.xs))
            Text(
                text = statusLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Tappable factor pill: opens a plain-language explanation (progressive disclosure). */
@Composable
private fun FindingChip(text: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        shape = CircleShape,
        color = EmpatiSurfaces.inset(),
        border = BorderStroke(1.dp, EmpatiSurfaces.hairline()),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.Button,
                onClickLabel = "Buka penjelasan $text",
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 14.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(6.dp))
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StrengthsCard(items: List<String>) {
    val calm = MaterialTheme.empati.calm
    Surface(
        shape = MaterialTheme.shapes.large,
        color = calm.container.copy(alpha = if (MaterialTheme.empati.isDark) 0.55f else 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(EmpatiSpace.cardPaddingCompact)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Spa,
                    contentDescription = null,
                    tint = calm.onContainer,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(EmpatiSpace.xs))
                Text(
                    text = "Yang sudah baik",
                    style = MaterialTheme.typography.labelLarge,
                    color = calm.onContainer
                )
            }
            Spacer(Modifier.height(EmpatiSpace.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
            ) {
                items.take(5).forEach { factor ->
                    Surface(shape = CircleShape, color = EmpatiSurfaces.card()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 8.dp, bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = null,
                                tint = calm.accent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = factor,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NextStepsCard(title: String, items: List<String>) {
    val calm = MaterialTheme.empati.calm
    TonalCard(contentPadding = EmpatiSpace.cardPaddingCompact, modifier = Modifier.fillMaxWidth()) {
        SectionLabel(title)
        Spacer(Modifier.height(EmpatiSpace.sm))
        items.forEachIndexed { index, item ->
            if (index > 0) {
                Spacer(Modifier.height(EmpatiSpace.sm))
                SoftDivider()
                Spacer(Modifier.height(EmpatiSpace.sm))
            }
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(24.dp)
                        .background(calm.container, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = calm.onContainer,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(EmpatiSpace.sm))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun Disclaimer(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = EmpatiSpace.md)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FindingDetailSheet(
    tag: String,
    onAskAi: () -> Unit,
    onDismiss: () -> Unit
) {
    val detail = remember(tag) { UserResultPresenter.explainClinicalTag(tag) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = EmpatiSurfaces.card(),
        shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = EmpatiSpace.xl)
                .padding(bottom = EmpatiSpace.xl)
        ) {
            SectionChip(
                text = detail.categoryBadge.toSentenceCase(),
                containerColor = MaterialTheme.empati.presence.container,
                contentColor = MaterialTheme.empati.presence.onContainer
            )
            Spacer(Modifier.height(EmpatiSpace.md))
            Text(
                text = detail.tag,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(EmpatiSpace.sm))
            Text(
                text = detail.explanation,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(EmpatiSpace.md))
            SoftNotice(
                text = detail.linkageReminder,
                icon = Icons.Rounded.Info,
                tone = MaterialTheme.empati.neutral
            )
            Spacer(Modifier.height(EmpatiSpace.xl))
            PrimaryButton(text = "Mengerti", onClick = onDismiss, trailingIcon = null)
            Spacer(Modifier.height(EmpatiSpace.xxs))
            GhostButton(text = "Diskusikan dengan SRYA Emora", onClick = onAskAi)
        }
    }
}
