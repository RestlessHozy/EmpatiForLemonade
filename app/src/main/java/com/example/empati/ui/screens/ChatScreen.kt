package com.example.empati.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Article
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.SupportAgent
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.empati.EmpatiViewModel
import com.example.empati.data.ChatMessage
import com.example.empati.data.lemonade.ChatCapability
import com.example.empati.data.lemonade.ChatCapabilityNotice
import com.example.empati.data.rag.RagTopicScope
import com.example.empati.data.speech.SpeechPlaybackState
import com.example.empati.data.speech.VoiceInputState
import com.example.empati.data.triage.ReferenceChunk
import com.example.empati.ui.components.EmpatiLogo
import com.example.empati.ui.components.EmpatiSurfaces
import com.example.empati.ui.components.BreathingDots
import com.example.empati.ui.components.PrimaryButton
import com.example.empati.ui.components.SoftDivider
import com.example.empati.ui.components.SoftNotice
import com.example.empati.ui.components.focusHalo
import com.example.empati.ui.components.pressScale
import com.example.empati.ui.theme.EmpatiElevation
import com.example.empati.ui.theme.EmpatiMetricStyle
import com.example.empati.ui.theme.EmpatiMotion
import com.example.empati.ui.theme.EmpatiSpace
import com.example.empati.ui.theme.LocalReducedMotion
import com.example.empati.ui.theme.ToneColors
import com.example.empati.ui.theme.empati
import com.example.empati.ui.theme.softShadow
import kotlinx.coroutines.delay

private val quickReplies = listOf(
    "Apa itu hipertensi?",
    "Lihat hasil skrining saya",
    "Analisis hasil skrining saya",
    "Cara mencegah hipertensi?",
    "Bantuan SAPA 129"
)

/** Horizontal page margin inside the chat: a touch tighter than the 24dp gutter so bubbles can breathe. */
private val ChatGutter = EmpatiSpace.lg

// ═══════════════════════════════════════════════════════════════════════════
//  Stateful entry point (ViewModel contract unchanged)
// ═══════════════════════════════════════════════════════════════════════════

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChatScreen(vm: EmpatiViewModel, modifier: Modifier = Modifier) {
    val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }
    var selectedChunk by remember { mutableStateOf<ReferenceChunk?>(null) }
    val isImeVisible = WindowInsets.isImeVisible
    val context = LocalContext.current
    val voiceState = vm.voiceState
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> vm.onVoicePermissionResult(granted) }

    LaunchedEffect(Unit) {
        vm.refreshLemonadeStatus()
    }

    LaunchedEffect(voiceState) {
        if (voiceState is VoiceInputState.Ready) {
            input = voiceState.text
            vm.resetVoiceInput()
        }
    }

    LaunchedEffect(voiceState is VoiceInputState.Recording) {
        while (vm.voiceState is VoiceInputState.Recording) {
            delay(1_000)
            vm.tickVoiceRecording()
        }
    }

    DisposableEffect(Unit) {
        onDispose { vm.cancelVoiceRecording() }
    }

    val onVoiceClick: () -> Unit = {
        when (vm.voiceState) {
            is VoiceInputState.Recording -> vm.stopVoiceRecording()
            VoiceInputState.RequestingPermission, is VoiceInputState.Transcribing -> Unit
            else -> {
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.RECORD_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
                vm.requestVoicePermission()
                if (granted) {
                    vm.onVoicePermissionResult(true)
                } else {
                    micPermission.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        }
    }

    LaunchedEffect(vm.chatMessages.size, vm.isChatGenerating) {
        if (vm.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(vm.chatMessages.lastIndex)
        }
    }

    LaunchedEffect(isImeVisible) {
        if (isImeVisible && vm.chatMessages.isNotEmpty()) {
            listState.animateScrollToItem(vm.chatMessages.lastIndex)
        }
    }

    ChatContent(
        messages = vm.chatMessages,
        isGenerating = vm.isChatGenerating,
        llmReachable = vm.lemonadeStatus != null,
        input = input,
        onInputChange = { input = it },
        onSend = {
            if (input.isNotBlank()) {
                vm.sendChat(input)
                input = ""
            }
        },
        onQuickReply = { q -> vm.sendChat(q) },
        onCitationClick = { chunk -> selectedChunk = chunk },
        modifier = modifier,
        onRefreshStatus = vm::refreshLemonadeStatus,
        listState = listState,
        capability = vm.capabilityNotice,
        voiceState = voiceState,
        onVoiceClick = onVoiceClick,
        onVoiceStop = vm::stopVoiceRecording,
        onVoiceCancel = vm::cancelVoiceRecording,
        onVoiceDismiss = vm::resetVoiceInput,
        speechState = vm.speechState,
        onToggleSpeech = vm::toggleSpeech
    )

    selectedChunk?.let { chunk ->
        RagSourceBottomSheet(
            chunk = chunk,
            onDismiss = { selectedChunk = null }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Stateless content
// ═══════════════════════════════════════════════════════════════════════════

/**
 * The whole conversation surface: presence header, message list, quick replies and the floating composer.
 * Insets: imePadding is applied to maintain the chat composer comfortably above the soft keyboard.
 */
@Composable
fun ChatContent(
    messages: List<ChatMessage>,
    isGenerating: Boolean,
    llmReachable: Boolean,
    input: String,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    onQuickReply: (String) -> Unit,
    onCitationClick: (ReferenceChunk) -> Unit,
    modifier: Modifier = Modifier,
    onRefreshStatus: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    capability: ChatCapabilityNotice? = null,
    voiceState: VoiceInputState = VoiceInputState.Idle,
    onVoiceClick: () -> Unit = {},
    onVoiceStop: () -> Unit = {},
    onVoiceCancel: () -> Unit = {},
    onVoiceDismiss: () -> Unit = {},
    speechState: SpeechPlaybackState = SpeechPlaybackState.Idle,
    onToggleSpeech: (String, String) -> Unit = { _, _ -> }
) {
    val reduced = LocalReducedMotion.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .imePadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = EmpatiSpace.readableWidth)
                .fillMaxWidth()
        ) {
            ChatHeader(llmReachable = llmReachable, onRefreshStatus = onRefreshStatus)

            capability?.let { notice ->
                CapabilityStrip(
                    notice = notice,
                    modifier = Modifier.padding(horizontal = ChatGutter, vertical = EmpatiSpace.xxs)
                )
            }

            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(horizontal = ChatGutter, vertical = EmpatiSpace.md),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(EmpatiSpace.md)
            ) {
                item(key = "scope_banner") {
                    ScopeNotice()
                }
                itemsIndexed(
                    items = messages,
                    key = { index, msg -> chatMessageId(index, msg) }
                ) { index, msg ->
                    val messageId = chatMessageId(index, msg)
                    ChatBubble(
                        message = msg,
                        messageId = messageId,
                        speechState = speechState,
                        onSpeechClick = { onToggleSpeech(messageId, msg.text) },
                        onCitationClick = onCitationClick,
                        modifier = if (reduced) Modifier else Modifier.animateItem(
                            fadeInSpec = EmpatiMotion.standard(),
                            placementSpec = EmpatiMotion.standard()
                        )
                    )
                }
                if (isGenerating) {
                    item(key = "generating_indicator") {
                        ThinkingBubble()
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = ChatGutter, vertical = EmpatiSpace.xxs),
                horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
            ) {
                quickReplies.forEach { q ->
                    QuickReplyChip(text = q, onClick = { onQuickReply(q) })
                }
            }

            VoiceStatusStrip(
                state = voiceState,
                onStop = onVoiceStop,
                onCancel = onVoiceCancel,
                onRetry = onVoiceClick,
                onDismiss = onVoiceDismiss,
                modifier = Modifier.padding(horizontal = ChatGutter, vertical = EmpatiSpace.xxs)
            )

            ChatComposer(
                value = input,
                onValueChange = onInputChange,
                onSend = onSend,
                voiceState = voiceState,
                onVoiceClick = onVoiceClick
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Header & presence
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun ChatHeader(
    llmReachable: Boolean,
    onRefreshStatus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = ChatGutter, vertical = EmpatiSpace.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompanionAvatar(online = llmReachable)
        Spacer(Modifier.width(EmpatiSpace.md))
        Text(
            text = "SRYA Emora",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        Spacer(Modifier.width(EmpatiSpace.xs))
        StatusPill(reachable = llmReachable, onClick = onRefreshStatus)
    }
}

/** Soft presence avatar: SRYA Emora logo with background clipped in a circle, plus an online dot that pulses when reachable. */
@Composable
private fun CompanionAvatar(online: Boolean, modifier: Modifier = Modifier) {
    val avatarSize = 48.dp
    Box(modifier = modifier.size(avatarSize)) {
        EmpatiLogo(
            size = avatarSize,
            animated = online,
            withBackground = true,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .border(1.dp, EmpatiSurfaces.hairline(), CircleShape)
        )
        StatusDot(
            online = online,
            ringColor = MaterialTheme.colorScheme.background,
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }
}

@Composable
private fun StatusDot(online: Boolean, ringColor: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val accent = if (online) MaterialTheme.empati.calm.accent else MaterialTheme.empati.neutral.accent
    Box(modifier = modifier.size(14.dp), contentAlignment = Alignment.Center) {
        if (online && !reduced) PulseRing(color = accent)
        Box(
            Modifier
                .size(14.dp)
                .background(accent, CircleShape)
                .border(2.dp, ringColor, CircleShape)
        )
    }
}

@Composable
private fun PulseRing(color: Color) {
    val t by rememberInfiniteTransition(label = "presencePulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, easing = EmpatiMotion.EaseOutSoft)
        ),
        label = "presencePulseValue"
    )
    Box(
        Modifier
            .size(14.dp)
            .graphicsLayer {
                val s = 1f + 1.2f * t
                scaleX = s
                scaleY = s
                alpha = 0.45f * (1f - t)
            }
            .background(color, CircleShape)
    )
}

/** Quiet capability badge. Tapping re-checks Lemonade on port 13305. */
@Composable
private fun StatusPill(reachable: Boolean, onClick: () -> Unit) {
    val tone = MaterialTheme.empati
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (reachable) tone.calm.container else EmpatiSurfaces.inset(),
        modifier = Modifier.minimumInteractiveComponentSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 10.dp, end = 14.dp, top = 7.dp, bottom = 7.dp)
        ) {
            Box(
                Modifier
                    .size(8.dp)
                    .background(
                        if (reachable) tone.calm.accent else tone.neutral.accent,
                        CircleShape
                    )
            )
            Spacer(Modifier.width(EmpatiSpace.xs))
            Text(
                text = if (reachable) "Lokal siap" else "Periksa server",
                style = MaterialTheme.typography.labelMedium,
                color = if (reachable) tone.calm.onContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Small status strip that names the capability Lemonade is missing right now. */
@Composable
private fun CapabilityStrip(
    notice: ChatCapabilityNotice,
    modifier: Modifier = Modifier
) {
    val tone = when (notice.capability) {
        ChatCapability.SERVER_MISSING -> MaterialTheme.empati.concerned
        ChatCapability.MODEL_LOADING -> MaterialTheme.empati.presence
        ChatCapability.MODEL_BUSY -> MaterialTheme.empati.attentive
    }
    val icon = when (notice.capability) {
        ChatCapability.SERVER_MISSING -> Icons.Rounded.CloudOff
        ChatCapability.MODEL_LOADING -> Icons.Rounded.HourglassTop
        ChatCapability.MODEL_BUSY -> Icons.Rounded.HourglassBottom
    }

    SoftNotice(
        text = notice.detail,
        icon = icon,
        tone = tone,
        title = notice.title,
        liveRegion = true,
        modifier = modifier
    )
}

@Composable
private fun ScopeNotice() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = EmpatiSurfaces.inset()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = EmpatiSpace.xs)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Lock,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(EmpatiSpace.xs))
                Text(
                    text = "Teman cerita seputar kesehatan, ketenangan pikiran, dan ruang aman",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Static mini companion mark for bubbles using SRYA Emora logo with background.
 */
@Composable
private fun CompanionMark(modifier: Modifier = Modifier, markSize: Dp = 32.dp) {
    EmpatiLogo(
        size = markSize,
        animated = false,
        withBackground = true,
        modifier = modifier
            .size(markSize)
            .clip(CircleShape)
            .border(1.dp, EmpatiSurfaces.hairline(), CircleShape)
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Messages
// ═══════════════════════════════════════════════════════════════════════════

private val BubbleMaxWidth = 320.dp

fun chatMessageId(index: Int, message: ChatMessage): String =
    "$index-${message.isUser}-${message.text.hashCode()}"

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ChatBubble(
    message: ChatMessage,
    messageId: String,
    speechState: SpeechPlaybackState,
    onSpeechClick: () -> Unit,
    onCitationClick: (ReferenceChunk) -> Unit,
    modifier: Modifier = Modifier
) {
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp, lineHeight = 24.sp)
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    if (message.isUser) {
        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.widthIn(max = BubbleMaxWidth)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 24.dp,
                        topEnd = 24.dp,
                        bottomEnd = 6.dp,
                        bottomStart = 24.dp
                    ),
                    color = MaterialTheme.colorScheme.primary
                ) {
                    SelectionContainer {
                        Text(
                            text = message.text,
                            color = MaterialTheme.colorScheme.onPrimary,
                            style = bodyStyle,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
                        )
                    }
                }
                Surface(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "Pesan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    color = Color.Transparent,
                    modifier = Modifier
                        .padding(top = 2.dp, end = 4.dp)
                        .size(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin pesan",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }
        }
    } else {
        val hasGbvProtectionChunk = message.retrievedChunks.any {
            it.topicScope == RagTopicScope.GBV_PROTECTION || it.chunkId == "DOC-GBV-01"
        } || message.citations.any { it.id.startsWith("sapa129") }

        Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.Top
        ) {
            CompanionMark(modifier = Modifier.padding(top = EmpatiSpace.xxs))
            Spacer(Modifier.width(EmpatiSpace.xs))
            Column(
                modifier = Modifier.widthIn(max = BubbleMaxWidth),
                verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
            ) {
                Surface(
                    shape = RoundedCornerShape(
                        topStart = 6.dp,
                        topEnd = 24.dp,
                        bottomEnd = 24.dp,
                        bottomStart = 24.dp
                    ),
                    color = EmpatiSurfaces.card(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline())
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
                    ) {
                        SelectionContainer {
                            Text(
                                text = message.text,
                                color = MaterialTheme.colorScheme.onSurface,
                                style = bodyStyle
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpeechActionButton(
                                state = speechState,
                                messageId = messageId,
                                onClick = onSpeechClick
                            )
                            Surface(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(message.text))
                                    Toast.makeText(context, "Pesan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                                },
                                shape = CircleShape,
                                color = Color.Transparent,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = "Salin pesan",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                        }
                        val speechError = (speechState as? SpeechPlaybackState.Error)
                            ?.takeIf { it.messageId == messageId }
                        if (speechError != null) {
                            Text(
                                text = speechError.message,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }

                if (message.citations.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                        verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xxs),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        message.citations.forEach { chunk ->
                            RagSourceCitationChip(
                                chunk = chunk,
                                onClick = { onCitationClick(chunk) }
                            )
                        }
                    }
                }

                if (hasGbvProtectionChunk) {
                    GbvContextualActionStrip()
                }
            }
        }
    }
}

@Composable
private fun ThinkingBubble(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CompanionMark()
        Spacer(Modifier.width(EmpatiSpace.xs))
        Surface(
            shape = MaterialTheme.shapes.large,
            color = EmpatiSurfaces.card(),
            border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                BreathingDots(color = MaterialTheme.empati.presence.accent)
                Spacer(Modifier.width(EmpatiSpace.sm))
                Text(
                    text = "SRYA Emora sedang mengetik…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun GbvContextualActionStrip(modifier: Modifier = Modifier) {
    SoftNotice(
        title = "Layanan rahasia SAPA 129",
        text = "Hotline 129 · WhatsApp 08111-129-129 (KemenPPPA)",
        icon = Icons.Rounded.SupportAgent,
        tone = MaterialTheme.empati.presence,
        modifier = modifier
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  Quick replies & composer
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun QuickReplyChip(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = EmpatiSurfaces.inset(),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline()),
        modifier = modifier
            .heightIn(min = EmpatiSpace.touchTarget)
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Box(
            modifier = Modifier
                .heightIn(min = EmpatiSpace.touchTarget)
                .padding(horizontal = EmpatiSpace.lg),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Floating bar: the only chat element with a real shadow. */
@Composable
private fun ChatComposer(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    voiceState: VoiceInputState,
    onVoiceClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme
    val voiceBusy = voiceState is VoiceInputState.Recording || voiceState is VoiceInputState.Transcribing
    val canSend = value.isNotBlank() && !voiceBusy
    val placeholder = when (voiceState) {
        is VoiceInputState.Recording -> "Sedang merekam… ketuk tombol berhenti bila selesai."
        is VoiceInputState.Transcribing -> "Sedang memproses rekaman Anda…"
        else -> "Ceritakan yang Anda rasakan atau tanyakan sesuatu…"
    }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val sendContainer by animateColorAsState(
        targetValue = if (canSend) colors.primary else colors.surfaceContainerHigh,
        animationSpec = EmpatiMotion.quick(),
        label = "sendBtnBg"
    )
    val sendTint by animateColorAsState(
        targetValue = if (canSend) colors.onPrimary else colors.onSurfaceVariant,
        animationSpec = EmpatiMotion.quick(),
        label = "sendBtnTint"
    )
    val sendScale by animateFloatAsState(
        targetValue = if (canSend) 1f else 0.92f,
        animationSpec = EmpatiMotion.press(),
        label = "sendBtnScale"
    )
    val barShape = MaterialTheme.shapes.extraLarge
    val fieldInteraction = remember { MutableInteractionSource() }
    val sendInteraction = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = EmpatiSpace.md, vertical = EmpatiSpace.sm)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .softShadow(EmpatiElevation.floating, barShape, MaterialTheme.empati.shadowTint)
                .background(EmpatiSurfaces.card(), barShape)
                .border(1.dp, EmpatiSurfaces.hairline(), barShape)
                .padding(6.dp)
        ) {
            VoiceActionButton(state = voiceState, onClick = onVoiceClick)
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = EmpatiSpace.touchTarget)
                    .focusHalo(fieldInteraction, CircleShape)
                    .background(EmpatiSurfaces.inset(), CircleShape)
                    .padding(horizontal = EmpatiSpace.lg),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    enabled = !voiceBusy,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(color = colors.onSurface),
                    cursorBrush = SolidColor(colors.primary),
                    singleLine = true,
                    interactionSource = fieldInteraction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (value.isEmpty()) {
                                Text(
                                    text = placeholder,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.onSurfaceVariant
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }
            if (value.isNotEmpty()) {
                val copyInteraction = remember { MutableInteractionSource() }
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(value))
                        Toast.makeText(context, "Draf pesan disalin ke clipboard", Toast.LENGTH_SHORT).show()
                    },
                    shape = CircleShape,
                    color = EmpatiSurfaces.inset(),
                    border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline()),
                    modifier = Modifier
                        .size(EmpatiSpace.touchTarget)
                        .pressScale(copyInteraction, 0.92f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin draf teks",
                            tint = colors.onSurfaceVariant,
                            modifier = Modifier.size(EmpatiSpace.md)
                        )
                    }
                }
            }
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = onSend,
                enabled = canSend,
                interactionSource = sendInteraction,
                shape = CircleShape,
                color = sendContainer,
                modifier = Modifier
                    .size(EmpatiSpace.touchTarget)
                    .graphicsLayer {
                        scaleX = sendScale
                        scaleY = sendScale
                    }
                    .pressScale(sendInteraction, 0.92f, canSend)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = "Kirim",
                        tint = sendTint,
                        modifier = Modifier.size(EmpatiSpace.lg)
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  Voice input
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun VoiceStatusStrip(
    state: VoiceInputState,
    onStop: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    when (state) {
        VoiceInputState.Idle,
        VoiceInputState.RequestingPermission,
        is VoiceInputState.Ready -> Unit

        is VoiceInputState.Recording -> Surface(
            shape = MaterialTheme.shapes.medium,
            color = EmpatiSurfaces.inset(),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
            ) {
                RecordingDot(tint = MaterialTheme.empati.concerned.accent)
                Spacer(Modifier.width(EmpatiSpace.xs))
                Text(
                    text = "Merekam… ${formatElapsed(state.elapsedMs)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextAction(text = "Selesai", onClick = onStop)
                TextAction(text = "Batalkan", onClick = onCancel)
            }
        }

        is VoiceInputState.Transcribing -> Surface(
            shape = MaterialTheme.shapes.medium,
            color = EmpatiSurfaces.inset(),
            modifier = modifier
                .fillMaxWidth()
                .semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                BreathingDots(color = MaterialTheme.empati.presence.accent)
                Spacer(Modifier.width(EmpatiSpace.sm))
                Text(
                    text = if (state.timedOut) {
                        "Batas 60 detik tercapai. Sedang memproses rekaman…"
                    } else {
                        "Sedang memproses rekaman Anda…"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        VoiceInputState.PermissionDenied -> VoiceNotice(
            title = "Izin mikrofon ditolak",
            text = "Tidak masalah, Anda tetap bisa mengetik cerita Anda. Ketuk coba lagi bila ingin merekam.",
            icon = Icons.Rounded.MicOff,
            tone = MaterialTheme.empati.attentive,
            primaryLabel = "Coba lagi",
            onPrimary = onRetry,
            onDismiss = onDismiss,
            modifier = modifier
        )

        VoiceInputState.Cancelled -> VoiceNotice(
            title = "Rekaman dibatalkan",
            text = "Tidak ada yang dikirim. Anda bisa mulai merekam lagi kapan saja.",
            icon = Icons.Rounded.MicOff,
            tone = MaterialTheme.empati.neutral,
            primaryLabel = "Rekam lagi",
            onPrimary = onRetry,
            onDismiss = onDismiss,
            modifier = modifier
        )

        is VoiceInputState.Error -> VoiceNotice(
            title = "Rekaman belum berhasil",
            text = state.message,
            icon = Icons.Rounded.MicOff,
            tone = MaterialTheme.empati.attentive,
            primaryLabel = "Coba lagi",
            onPrimary = onRetry,
            onDismiss = onDismiss,
            modifier = modifier
        )
    }
}

@Composable
private fun VoiceActionButton(
    state: VoiceInputState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val recording = state is VoiceInputState.Recording
    val busy = state is VoiceInputState.RequestingPermission || state is VoiceInputState.Transcribing
    val label = when (state) {
        is VoiceInputState.Recording -> "Hentikan rekaman"
        VoiceInputState.RequestingPermission -> "Meminta izin mikrofon"
        is VoiceInputState.Transcribing -> "Sedang memproses rekaman"
        else -> "Rekam pesan suara"
    }

    Surface(
        onClick = onClick,
        enabled = !busy,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = if (recording) MaterialTheme.colorScheme.primary else EmpatiSurfaces.inset(),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline()),
        modifier = modifier
            .size(EmpatiSpace.touchTarget)
            .semantics { contentDescription = label }
            .pressScale(interactionSource, 0.92f, !busy)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    imageVector = if (recording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = if (recording) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(EmpatiSpace.md)
                )
            }
        }
    }
}

@Composable
private fun RecordingDot(tint: Color, modifier: Modifier = Modifier) {
    val reduced = LocalReducedMotion.current
    val pulse by rememberInfiniteTransition(label = "recordingPulse").animateFloat(
        initialValue = 1f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = EmpatiMotion.EaseInOutSoft),
            repeatMode = RepeatMode.Reverse
        ),
        label = "recordingPulseValue"
    )
    Box(
        modifier = modifier
            .size(10.dp)
            .graphicsLayer { alpha = if (reduced) 1f else pulse }
            .background(tint, CircleShape)
    )
}

@Composable
private fun VoiceNotice(
    title: String,
    text: String,
    icon: ImageVector,
    tone: ToneColors,
    primaryLabel: String,
    onPrimary: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = tone.container,
        modifier = modifier
            .fillMaxWidth()
            .semantics { liveRegion = LiveRegionMode.Polite }
    ) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 4.dp)) {
            Row(verticalAlignment = Alignment.Top) {
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
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        color = tone.onContainer
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = text,
                        style = MaterialTheme.typography.bodySmall,
                        color = tone.onContainer
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextAction(
                    text = "Tutup",
                    onClick = onDismiss,
                    color = tone.onContainer
                )
                TextAction(
                    text = primaryLabel,
                    onClick = onPrimary,
                    color = tone.onContainer,
                    emphasized = true
                )
            }
        }
    }
}

@Composable
private fun TextAction(
    text: String,
    onClick: () -> Unit,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    emphasized: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 12.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Medium,
                color = color
            )
        }
    }
}

private fun formatElapsed(elapsedMs: Long): String {
    val seconds = (elapsedMs / 1_000).coerceAtLeast(0)
    return "${seconds / 60}:${(seconds % 60).toString().padStart(2, '0')}"
}

// ═══════════════════════════════════════════════════════════════════════════
//  Voice output
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SpeechActionButton(
    state: SpeechPlaybackState,
    messageId: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preparing = state is SpeechPlaybackState.Preparing && state.messageId == messageId
    val playing = state is SpeechPlaybackState.Playing && state.messageId == messageId
    val label = when {
        preparing -> "Batalkan penyiapan suara"
        playing -> "Hentikan pemutaran"
        else -> "Putar balasan sebagai suara"
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = Color.Transparent,
        modifier = modifier
            .size(28.dp)
            .semantics { contentDescription = label }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = when {
                    preparing -> Icons.Rounded.HourglassTop
                    playing -> Icons.Rounded.Stop
                    else -> Icons.Rounded.VolumeUp
                },
                contentDescription = null,
                tint = if (playing || preparing) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                },
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  RAG sources
// ═══════════════════════════════════════════════════════════════════════════

/** Penerbit singkat + tahun, supaya chip tetap terbaca di samping bubble. */
private fun ReferenceChunk.sourceLabel(): String {
    val normalized = publisher.lowercase()
    val shortPublisher = when {
        normalized.startsWith("world health organization") -> "WHO"
        normalized.startsWith("kementerian kesehatan") -> "Kemenkes"
        normalized.startsWith("kementerian pemberdayaan perempuan") -> "KemenPPPA"
        else -> publisher
    }
    return "$shortPublisher · $year"
}

@Composable
fun RagSourceCitationChip(
    chunk: ReferenceChunk,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    Surface(
        onClick = onClick,
        interactionSource = interactionSource,
        shape = CircleShape,
        color = EmpatiSurfaces.inset(),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmpatiSurfaces.hairline()),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .pressScale(interactionSource, 0.96f)
            .focusHalo(interactionSource, CircleShape)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.Article,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(EmpatiSpace.xs))
            Text(
                text = chunk.sourceLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 180.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RagSourceBottomSheet(
    chunk: ReferenceChunk,
    onDismiss: () -> Unit
) {
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
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(EmpatiSpace.xs),
                verticalArrangement = Arrangement.spacedBy(EmpatiSpace.xs)
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = chunk.year.toString(),
                        style = EmpatiMetricStyle.copy(fontSize = 12.sp, lineHeight = 16.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    shape = CircleShape,
                    color = EmpatiSurfaces.inset()
                ) {
                    Text(
                        text = chunk.section,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(EmpatiSpace.md))

            Text(
                text = chunk.title,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )

            Spacer(Modifier.height(EmpatiSpace.xs))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Verified,
                    contentDescription = null,
                    tint = MaterialTheme.empati.calm.accent,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(EmpatiSpace.xs))
                Text(
                    text = chunk.publisher,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.empati.calm.accent
                )
            }

            Spacer(Modifier.height(EmpatiSpace.md))
            SoftDivider()
            Spacer(Modifier.height(EmpatiSpace.md))

            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = chunk.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(Modifier.height(EmpatiSpace.xl))

            PrimaryButton(
                text = "Tutup rujukan",
                onClick = onDismiss,
                trailingIcon = null
            )
        }
    }
}
