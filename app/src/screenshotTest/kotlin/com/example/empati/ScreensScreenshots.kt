package com.example.empati

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import com.example.empati.data.cf.AnswerValue
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.speech.SpeechPlaybackState
import com.example.empati.data.speech.VoiceInputState
import com.example.empati.ui.screens.BottomNavBar
import com.example.empati.ui.screens.ChatContent
import com.example.empati.ui.screens.chatMessageId
import com.example.empati.ui.screens.ConsentScreenContent
import com.example.empati.ui.screens.HistoryScreenContent
import com.example.empati.ui.screens.HomeScreenContent
import com.example.empati.ui.screens.ONBOARDING_PAGE_COUNT
import com.example.empati.ui.screens.OnboardingScreenContent
import com.example.empati.ui.screens.ProfileField
import com.example.empati.ui.screens.ProfileScreenContent
import com.example.empati.ui.screens.QuestionnaireContent
import com.example.empati.ui.screens.SplashScreen

// ── Entry flow ───────────────────────────────────────────────────────────────────────────────

@PreviewTest
@PhoneLightDark
@Composable
fun FlowSplash() = PreviewTheme { SplashScreen() }

@Composable
private fun OnboardingPreview(page: Int) = PreviewTheme {
    val pager = rememberPagerState(initialPage = page, pageCount = { ONBOARDING_PAGE_COUNT })
    OnboardingScreenContent(pagerState = pager, onSkip = {}, onPrimary = {})
}

@PreviewTest
@PhoneLightDark
@Composable
fun FlowOnboardingPrivacy() = OnboardingPreview(0)

@PreviewTest
@PhoneLightDark
@Composable
fun FlowOnboardingScreening() = OnboardingPreview(1)

@PreviewTest
@PhoneLightDark
@Composable
fun FlowOnboardingCompanion() = OnboardingPreview(2)

@PreviewTest
@PhoneLightDark
@Composable
fun FlowConsent() = PreviewTheme {
    ConsentScreenContent(onAgree = {}, onCancel = {})
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun FlowConsentExpanded() = PreviewTheme {
    ConsentScreenContent(onAgree = {}, onCancel = {}, initiallyExpanded = true)
}

// ── Home · History · Profile ────────────────────────────────────────────────────────────────

@PreviewTest
@PhoneTallLightDark
@Composable
fun TabHomeWithResult() = PreviewTheme {
    HomeScreenContent(
        profile = Fixtures.profile,
        lastResult = Fixtures.history.first(),
        hour = 9,
        onStartScreening = {},
        onOpenHistory = {},
        onOpenChat = {},
        onOpenProfile = {}
    )
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun TabHomeEmpty() = PreviewTheme {
    HomeScreenContent(
        profile = Fixtures.profile,
        lastResult = null,
        hour = 20,
        onStartScreening = {},
        onOpenHistory = {},
        onOpenChat = {},
        onOpenProfile = {}
    )
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun TabHistoryTimeline() = PreviewTheme {
    HistoryScreenContent(items = Fixtures.history, onStart = {})
}

@PreviewTest
@PhoneLightDark
@Composable
fun TabHistoryEmpty() = PreviewTheme {
    HistoryScreenContent(items = emptyList(), onStart = {})
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun TabProfile() = PreviewTheme {
    ProfileScreenContent(
        profile = Fixtures.profile,
        onUpdateProfile = {},
        onLogout = {},
        onClearAllData = {}
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun TabProfileEditDialog() = PreviewTheme {
    ProfileScreenContent(
        profile = Fixtures.profile,
        onUpdateProfile = {},
        onLogout = {},
        onClearAllData = {},
        initialEditingField = ProfileField.NAME
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun TabProfileDeleteDialog() = PreviewTheme {
    ProfileScreenContent(
        profile = Fixtures.profile,
        onUpdateProfile = {},
        onLogout = {},
        onClearAllData = {},
        initialShowDeleteConfirm = true
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun NavigationBottomBar() = PreviewTheme {
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            Box(Modifier.height(760.dp))
            BottomNavBar(
                selected = com.example.empati.Screen.History,
                onSelectTab = {},
                onScreening = {}
            )
        }
    }
}

// ── Screening ────────────────────────────────────────────────────────────────────────────────

@PreviewTest
@PhoneLightDark
@Composable
fun QuestionYesNoSelected() = PreviewTheme {
    QuestionnaireContent(
        module = ScreeningModule.HYPERTENSION,
        index = 6,
        total = 16,
        question = Fixtures.dichotomyQuestion,
        answer = AnswerValue.Dichotomy(true),
        onBack = {},
        onAnswer = {},
        onNext = {}
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun QuestionYesNoUnanswered() = PreviewTheme {
    QuestionnaireContent(
        module = ScreeningModule.HYPERTENSION,
        index = 0,
        total = 16,
        question = Fixtures.dichotomyQuestion,
        answer = AnswerValue.Unanswered,
        onBack = {},
        onAnswer = {},
        onNext = {}
    )
}

@PreviewTest
@PhoneTallLightDark
@Composable
fun QuestionSensitiveLikert() = PreviewTheme {
    QuestionnaireContent(
        module = ScreeningModule.GBV,
        index = 2,
        total = 12,
        question = Fixtures.sensitiveLikertQuestion,
        answer = AnswerValue.Likert(3),
        onBack = {},
        onAnswer = {},
        onNext = {}
    )
}

// ── Companion chat ──────────────────────────────────────────────────────────────────────────

@PreviewTest
@PhoneTallLightDark
@Composable
fun ChatConversation() = PreviewTheme {
    ChatContent(
        messages = Fixtures.chat,
        isGenerating = false,
        llmReachable = true,
        input = "",
        onInputChange = {},
        onSend = {},
        onQuickReply = {},
        onCitationClick = {}
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun ChatThinkingWithDraft() = PreviewTheme {
    ChatContent(
        messages = Fixtures.chat.take(2),
        isGenerating = true,
        llmReachable = false,
        input = "Apa itu hipertensi?",
        onInputChange = {},
        onSend = {},
        onQuickReply = {},
        onCitationClick = {}
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun ChatVoiceRecording() = PreviewTheme {
    ChatContent(
        messages = Fixtures.chat.take(3),
        isGenerating = false,
        llmReachable = true,
        input = "",
        onInputChange = {},
        onSend = {},
        onQuickReply = {},
        onCitationClick = {},
        voiceState = VoiceInputState.Recording(startedAtMs = 0, elapsedMs = 7_000),
        onVoiceClick = {},
        onVoiceStop = {},
        onVoiceCancel = {},
        onVoiceDismiss = {}
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun ChatSpeaking() = PreviewTheme {
    ChatContent(
        messages = Fixtures.chat.take(3),
        isGenerating = false,
        llmReachable = true,
        input = "",
        onInputChange = {},
        onSend = {},
        onQuickReply = {},
        onCitationClick = {},
        speechState = SpeechPlaybackState.Playing(chatMessageId(2, Fixtures.chat[2])),
        onToggleSpeech = { _, _ -> }
    )
}

@PreviewTest
@PhoneLightDark
@Composable
fun ChatVoicePermissionDenied() = PreviewTheme {
    ChatContent(
        messages = Fixtures.chat.take(2),
        isGenerating = false,
        llmReachable = true,
        input = "",
        onInputChange = {},
        onSend = {},
        onQuickReply = {},
        onCitationClick = {},
        voiceState = VoiceInputState.PermissionDenied,
        onVoiceClick = {},
        onVoiceStop = {},
        onVoiceCancel = {},
        onVoiceDismiss = {}
    )
}
