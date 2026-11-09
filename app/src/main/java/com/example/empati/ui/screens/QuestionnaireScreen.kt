package com.example.empati.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.empati.EmpatiViewModel
import com.example.empati.data.cf.AnswerValue
import com.example.empati.data.cf.CfQuestion
import com.example.empati.data.cf.QuestionKind
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.cf.likertLabels
import com.example.empati.ui.components.BackIconButton
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.LockNotice
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.ScreenFrame
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.components.toSentenceCase
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiNumeralStyle
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.theme.empati

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry point (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
fun QuestionnaireScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val module = vm.activeModule
    val questions = vm.currentQuestions()
    val index = vm.dualIndex
    val question = vm.currentQuestion()
    val answer = vm.answerFor(question.id)

    QuestionnaireContent(
        module = module,
        index = index,
        total = questions.size,
        question = question,
        answer = answer,
        onBack = vm::previousDualQuestion,
        onAnswer = { value -> vm.answerDual(question.id, value) },
        onNext = vm::nextDualQuestion,
        modifier = modifier
    )
}

private data class QuestionStepState(
    val module: ScreeningModule,
    val index: Int,
    val question: CfQuestion
)

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content
// ═══════════════════════════════════════════════════════════════════════════

/**
 * One question at a time. [index] is zero-based; [answer] belongs to [question]; [onAnswer] records an
 * answer for the *current* question. The primary action only appears once the question is answered.
 */
@Composable
fun QuestionnaireContent(
    module: ScreeningModule,
    index: Int,
    total: Int,
    question: CfQuestion,
    answer: AnswerValue,
    onBack: () -> Unit,
    onAnswer: (AnswerValue) -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reduced = LocalReducedMotion.current
    val accent = moduleAccent(module)
    val rawProgress = if (total > 0) ((index + 1) / total.toFloat()).coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = rawProgress,
        animationSpec = EmpatiMotion.settle(),
        label = "questionProgress"
    )
    val isAnswered = answer != AnswerValue.Unanswered
    val isLast = index >= total - 1
    val stepState = QuestionStepState(module = module, index = index, question = question)

    // The outgoing page keeps showing the answer it had, instead of flashing the incoming one's.
    val seenAnswers = remember { HashMap<String, AnswerValue>() }
    seenAnswers[question.id] = answer

    ScreenFrame(modifier = modifier, scrollable = false) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = EmpatiSpace.xs)
        ) {
            BackIconButton(onClick = onBack)
            Spacer(Modifier.width(EmpatiSpace.md))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = moduleIndicator(module),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${index + 1} dari $total",
                        style = EmpatiNumeralStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(EmpatiSpace.sm))
                StepProgressBar(progress = animatedProgress, accent = accent)
            }
        }

        Spacer(Modifier.height(EmpatiSpace.xl))

        AnimatedContent(
            targetState = stepState,
            modifier = Modifier.weight(1f),
            transitionSpec = {
                val forward = targetState.module != initialState.module ||
                    targetState.index >= initialState.index
                val transition = if (reduced) {
                    fadeIn(EmpatiMotion.quick()) togetherWith fadeOut(EmpatiMotion.quick())
                } else if (forward) {
                    (slideInHorizontally(
                        animationSpec = EmpatiMotion.standard(),
                        initialOffsetX = { fullWidth -> fullWidth / 8 }
                    ) + fadeIn(EmpatiMotion.standard())) togetherWith
                        (slideOutHorizontally(
                            animationSpec = EmpatiMotion.quick(),
                            targetOffsetX = { fullWidth -> -fullWidth / 10 }
                        ) + fadeOut(EmpatiMotion.quick()))
                } else {
                    (slideInHorizontally(
                        animationSpec = EmpatiMotion.standard(),
                        initialOffsetX = { fullWidth -> -fullWidth / 8 }
                    ) + fadeIn(EmpatiMotion.standard())) togetherWith
                        (slideOutHorizontally(
                            animationSpec = EmpatiMotion.quick(),
                            targetOffsetX = { fullWidth -> fullWidth / 10 }
                        ) + fadeOut(EmpatiMotion.quick()))
                }
                transition.using(SizeTransform(clip = false))
            },
            label = "questionTransition"
        ) { state ->
            val isCurrent = state.question.id == question.id
            val shownAnswer = if (isCurrent) answer
            else seenAnswers[state.question.id] ?: AnswerValue.Unanswered
            QuestionPage(
                module = state.module,
                question = state.question,
                answer = shownAnswer,
                accent = accent,
                enabled = isCurrent,
                onAnswer = onAnswer
            )
        }

        // Reserved height: the layout never jumps when the action appears.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(88.dp),
            contentAlignment = Alignment.Center
        ) {
            // Fully qualified: the enclosing ColumnScope extension overload is not callable from inside Box.
            androidx.compose.animation.AnimatedVisibility(
                visible = isAnswered,
                enter = if (reduced) fadeIn(EmpatiMotion.quick())
                else fadeIn(EmpatiMotion.standard()) +
                    slideInVertically(
                        animationSpec = EmpatiMotion.standard(),
                        initialOffsetY = { fullHeight -> fullHeight / 3 }
                    ),
                exit = if (reduced) fadeOut(EmpatiMotion.quick())
                else fadeOut(EmpatiMotion.quick()) +
                    slideOutVertically(
                        animationSpec = EmpatiMotion.quick(),
                        targetOffsetY = { fullHeight -> fullHeight / 3 }
                    )
            ) {
                PrimaryButton(
                    text = if (isLast) "Lihat ringkasan modul" else "Selanjutnya",
                    onClick = onNext
                )
            }
        }
    }
}

@Composable
private fun QuestionPage(
    module: ScreeningModule,
    question: CfQuestion,
    answer: AnswerValue,
    accent: Color,
    enabled: Boolean,
    onAnswer: (AnswerValue) -> Unit
) {
    val selectedDichotomy = (answer as? AnswerValue.Dichotomy)?.value
    val selectedLikert = (answer as? AnswerValue.Likert)?.value

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = question.category.toSentenceCase(),
            style = MaterialTheme.typography.labelLarge,
            color = accent
        )
        Spacer(Modifier.height(EmpatiSpace.sm))
        Text(
            text = question.text,
            style = MaterialTheme.typography.headlineSmall.copy(lineHeight = 33.sp),
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.semantics { heading() }
        )
        if (question.sensitive) {
            Spacer(Modifier.height(EmpatiSpace.md))
            LockNotice()
        }
        Spacer(Modifier.height(EmpatiSpace.xl))

        if (question.kind == QuestionKind.DICHOTOMY) {
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(EmpatiSpace.sm)
            ) {
                OptionRow(
                    label = "Ya",
                    subtitle = "Sesuai dengan kondisi saya",
                    selected = selectedDichotomy == true,
                    enabled = enabled,
                    onClick = { onAnswer(AnswerValue.Dichotomy(true)) }
                )
                OptionRow(
                    label = "Tidak",
                    subtitle = "Tidak sesuai dengan kondisi saya",
                    selected = selectedDichotomy == false,
                    enabled = enabled,
                    onClick = { onAnswer(AnswerValue.Dichotomy(false)) }
                )
            }
        } else {
            val labels = likertLabels(module)
            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                labels.forEachIndexed { i, label ->
                    OptionRow(
                        label = label,
                        subtitle = null,
                        selected = selectedLikert == (i + 1),
                        enabled = enabled,
                        onClick = { onAnswer(AnswerValue.Likert(i + 1)) }
                    )
                }
            }
        }
        Spacer(Modifier.height(EmpatiSpace.md))
    }
}

@Composable
private fun moduleAccent(module: ScreeningModule): Color =
    if (module == ScreeningModule.HYPERTENSION) MaterialTheme.colorScheme.primary
    else MaterialTheme.empati.presence.accent

private fun moduleIndicator(module: ScreeningModule): String = when (module) {
    ScreeningModule.HYPERTENSION -> "Modul hipertensi"
    ScreeningModule.GBV -> "Modul kesejahteraan & KBG"
}

/** Slim, rounded step progress. Track is recessed, fill is the module accent. */
@Composable
private fun StepProgressBar(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val fraction = progress.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(EmpatiSurfaces.sunken())
            .semantics(mergeDescendants = true) {
                progressBarRangeInfo = ProgressBarRangeInfo(fraction, 0f..1f)
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction)
                .clip(CircleShape)
                .background(accent)
        )
    }
}

/**
 * Soft selectable pebble. The whole row is the touch target and a single radio in its group;
 * selection is shown by tone, a 2dp ring and a check disc (never by colour alone).
 */
@Composable
private fun OptionRow(
    label: String,
    subtitle: String?,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val shape = MaterialTheme.shapes.large

    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.primaryContainer else EmpatiSurfaces.card(),
        animationSpec = EmpatiMotion.quick(),
        label = "optionContainer"
    )
    val ringColor by animateColorAsState(
        targetValue = if (selected) colors.primary else EmpatiSurfaces.hairline(),
        animationSpec = EmpatiMotion.quick(),
        label = "optionRing"
    )
    val ringWidth by animateDpAsState(
        targetValue = if (selected) 2.dp else 1.dp,
        animationSpec = EmpatiMotion.quick(),
        label = "optionRingWidth"
    )
    val contentColor = if (selected) colors.onPrimaryContainer else colors.onSurface

    Surface(
        shape = shape,
        color = containerColor,
        border = BorderStroke(ringWidth, ringColor),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .pressScale(interactionSource, 0.985f)
            .focusHalo(interactionSource, shape)
            .clip(shape)
            .selectable(
                selected = selected,
                enabled = enabled,
                interactionSource = interactionSource,
                indication = ripple(),
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = EmpatiSpace.lg, vertical = EmpatiSpace.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = EmpatiSpace.xxs)
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = contentColor
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (selected) colors.onPrimaryContainer else colors.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(EmpatiSpace.md))
            CheckDisc(selected = selected)
        }
    }
}

@Composable
private fun CheckDisc(selected: Boolean) {
    val colors = MaterialTheme.colorScheme
    val fill by animateColorAsState(
        targetValue = if (selected) colors.primary else Color.Transparent,
        animationSpec = EmpatiMotion.quick(),
        label = "checkFill"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) colors.primary else colors.outline,
        animationSpec = EmpatiMotion.quick(),
        label = "checkOutline"
    )
    Box(
        modifier = Modifier
            .size(28.dp)
            .background(fill, CircleShape)
            .border(1.5.dp, outline, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(EmpatiMotion.quick(), initialScale = 0.5f) + fadeIn(EmpatiMotion.quick()),
            exit = scaleOut(EmpatiMotion.quick(), targetScale = 0.5f) + fadeOut(EmpatiMotion.quick())
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = colors.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
