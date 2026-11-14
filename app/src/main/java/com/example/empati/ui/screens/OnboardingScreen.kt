package com.example.empati.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.SectionChip
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.theme.ToneColors
import com.example.empati.ui.theme.empati
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

/** Number of onboarding pages. Use with `rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })`. */
const val ONBOARDING_PAGE_COUNT = 3

private enum class SlideTone { Calm, Attentive, Presence }

@Composable
@ReadOnlyComposable
private fun SlideTone.colors(): ToneColors {
    val tones = MaterialTheme.empati
    return when (this) {
        SlideTone.Calm -> tones.calm
        SlideTone.Attentive -> tones.attentive
        SlideTone.Presence -> tones.presence
    }
}

private data class OnboardSlide(
    val tag: String,
    val title: String,
    val body: String,
    val tone: SlideTone,
    val mainIcon: ImageVector,
    val badgeIcon: ImageVector
)

private val slides = listOf(
    OnboardSlide(
        tag = "Privasi terjaga",
        title = "Ruang aman untuk Anda",
        body = "Cerita dan jawaban Anda dilindungi dengan penyamaran identitas satu arah, sehingga kerahasiaan diri Anda selalu terjaga dengan tenang.",
        tone = SlideTone.Calm,
        mainIcon = Icons.Rounded.VerifiedUser,
        badgeIcon = Icons.Rounded.Lock
    ),
    OnboardSlide(
        tag = "Kenali diri lebih utuh",
        title = "Kesehatan tubuh & ketenangan batin",
        body = "Pahami kondisi tekanan darah Anda dari sisi kebiasaan harian, beban pikiran, hingga kenyamanan relasi dalam keseharian.",
        tone = SlideTone.Attentive,
        mainIcon = Icons.Rounded.MonitorHeart,
        badgeIcon = Icons.Rounded.Shield
    ),
    OnboardSlide(
        tag = "Teman cerita",
        title = "Ngobrol bersama Empati",
        body = "Tanyakan seputar kesehatan tekanan darah, cara meredakan stres, atau informasi dukungan ruang aman kapan pun Anda membutuhkannya.",
        tone = SlideTone.Presence,
        mainIcon = Icons.Rounded.ChatBubbleOutline,
        badgeIcon = Icons.Rounded.Favorite
    )
)

@Composable
fun OnboardingScreen(onDone: () -> Unit, modifier: Modifier = Modifier) {
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val scope = rememberCoroutineScope()

    OnboardingScreenContent(
        pagerState = pagerState,
        onSkip = onDone,
        onPrimary = {
            if (pagerState.currentPage == slides.lastIndex) {
                onDone()
            } else {
                scope.launch {
                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                }
            }
        },
        modifier = modifier
    )
}

/**
 * Stateless onboarding layout. Hand it a [PagerState] created with [ONBOARDING_PAGE_COUNT] pages;
 * [onPrimary] is wired to "Selanjutnya" / "Mulai sekarang" and [onSkip] to "Lewati".
 */
@Composable
fun OnboardingScreenContent(
    pagerState: PagerState,
    onSkip: () -> Unit,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLastPage = pagerState.currentPage == slides.lastIndex

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .safeDrawingPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EmpatiSpace.readableWidth)
                .fillMaxWidth()
        ) {
            SkipRow(onSkip = onSkip)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                OnboardingPage(page = page, pagerState = pagerState)
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = EmpatiSpace.gutter,
                        end = EmpatiSpace.gutter,
                        top = EmpatiSpace.md,
                        bottom = EmpatiSpace.xl
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                PageIndicator(
                    pageCount = slides.size,
                    currentPage = pagerState.currentPage
                )
                Spacer(Modifier.height(EmpatiSpace.xl))
                PrimaryButton(
                    text = if (isLastPage) "Mulai sekarang" else "Selanjutnya",
                    onClick = onPrimary
                )
            }
        }
    }
}

@Composable
private fun SkipRow(onSkip: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = EmpatiSpace.touchTarget)
            .padding(horizontal = EmpatiSpace.md, vertical = EmpatiSpace.xs),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(
            onClick = onSkip,
            interactionSource = interactionSource,
            shape = CircleShape,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier
                .heightIn(min = EmpatiSpace.touchTarget)
                .pressScale(interactionSource, 0.96f)
                .focusHalo(interactionSource, CircleShape)
        ) {
            Text(
                text = "Lewati",
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

@Composable
private fun OnboardingPage(page: Int, pagerState: PagerState) {
    val slide = slides[page]
    val tone = slide.tone.colors()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val pageHeight = maxHeight
        val illustrationSize = (pageHeight * 0.4f).coerceIn(168.dp, 280.dp)

        Box(modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = pageHeight)
                    .padding(horizontal = EmpatiSpace.gutter)
                    .graphicsLayer {
                        val distance = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
                            .absoluteValue
                            .coerceIn(0f, 1f)
                        alpha = lerp(0.35f, 1f, 1f - distance)
                    },
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            // Illustration lags behind the page a little: a quiet sense of depth.
                            val signed = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                            translationX = signed * size.width * 0.22f
                            val settle = lerp(0.94f, 1f, 1f - signed.absoluteValue.coerceIn(0f, 1f))
                            scaleX = settle
                            scaleY = settle
                        },
                    contentAlignment = Alignment.Center
                ) {
                    OnboardingIllustration(slide = slide, tone = tone, size = illustrationSize)
                }

                Spacer(Modifier.height(EmpatiSpace.xxl))

                SectionChip(
                    text = slide.tag,
                    containerColor = tone.container,
                    contentColor = tone.onContainer
                )
                Spacer(Modifier.height(EmpatiSpace.md))
                Text(
                    text = slide.title,
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(EmpatiSpace.sm))
                Text(
                    text = slide.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Pill progress: the current step stretches and takes the brand colour. */
@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "Langkah ${currentPage + 1} dari $pageCount"
        },
        horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageCount) { index ->
            val active = currentPage == index
            val indicatorWidth by animateDpAsState(
                targetValue = if (active) 32.dp else 8.dp,
                animationSpec = EmpatiMotion.settle(),
                label = "pagerPillWidth"
            )
            val indicatorColor by animateColorAsState(
                targetValue = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                animationSpec = EmpatiMotion.standard(),
                label = "pagerPillColor"
            )
            Box(
                Modifier
                    .width(indicatorWidth)
                    .height(8.dp)
                    .background(indicatorColor, CircleShape)
            )
        }
    }
}

/**
 * Layered tonal discs (aura · halo · card) with one icon and a small companion badge.
 * Built only from theme tones so it holds up in light and dark.
 */
@Composable
private fun OnboardingIllustration(slide: OnboardSlide, tone: ToneColors, size: Dp) {
    val reduced = LocalReducedMotion.current
    val transition = rememberInfiniteTransition(label = "onboardingBreath")
    val breath by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3600, easing = EmpatiMotion.EaseInOutSoft),
            repeatMode = RepeatMode.Reverse
        ),
        label = "onboardingBreathValue"
    )
    val outerScale = if (reduced) 1f else 1f + 0.035f * breath
    val cardColor = EmpatiSurfaces.card()

    Box(
        modifier = Modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier
                .size(size)
                .graphicsLayer {
                    scaleX = outerScale
                    scaleY = outerScale
                }
                .background(tone.container.copy(alpha = 0.40f), CircleShape)
        )
        Box(
            Modifier
                .size(size * 0.76f)
                .background(tone.container.copy(alpha = 0.80f), CircleShape)
        )
        Box(
            Modifier
                .size(size * 0.50f)
                .background(cardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.mainIcon,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier.size(size * 0.22f)
            )
        }

        // Quiet accent dots keep the composition from feeling symmetrical or static.
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .padding(top = size * 0.14f, end = size * 0.16f)
                .size(size * 0.07f)
                .background(tone.accent.copy(alpha = 0.85f), CircleShape)
        )
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = size * 0.20f, start = size * 0.10f)
                .size(size * 0.045f)
                .background(tone.accent.copy(alpha = 0.45f), CircleShape)
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = size * 0.16f, end = size * 0.14f)
                .size(size * 0.20f)
                .background(cardColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = slide.badgeIcon,
                contentDescription = null,
                tint = tone.onContainer,
                modifier = Modifier.size(size * 0.09f)
            )
        }
    }
}
