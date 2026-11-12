package com.example.empati

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.empati.data.lemonade.LemonadeStatus
import com.example.empati.data.lemonade.LemonadeUnifiedClient
import com.example.empati.data.db.EmpatiDatabase
import com.example.empati.data.triage.HybridTriageBridge
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.empati.data.ChatEngine
import com.example.empati.data.ChatMessage
import com.example.empati.data.ScreeningResult
import com.example.empati.data.ScreeningStore
import com.example.empati.data.UserProfile
import com.example.empati.data.cf.AnswerValue
import com.example.empati.data.cf.CfQuestion
import com.example.empati.data.cf.CfResultGenerator
import com.example.empati.data.cf.IntegratedAnalysis
import com.example.empati.data.cf.ModuleResult
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.cf.gbvQuestions
import com.example.empati.data.cf.hypertensionQuestions
import com.example.empati.data.cf.questionsFor
import com.example.empati.data.presentation.UserIntegratedPresentation
import com.example.empati.data.presentation.UserModulePresentation
import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.data.presentation.UserRiskSummary
import com.example.empati.data.rag.OllamaConnectionStatus
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.research.ResearchRecord
import com.example.empati.data.research.ResearchRecordFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface Screen {
    data object Splash : Screen
    data object Onboarding : Screen
    data object Auth : Screen
    data object Home : Screen
    data object Consent : Screen
    data object DualQuestionnaire : Screen
    data object ModuleResult : Screen
    data object IntegratedResult : Screen
    data object Chat : Screen
    data object History : Screen
    data object Profile : Screen
}

class EmpatiViewModel(application: Application) : AndroidViewModel(application) {

    private val store = ScreeningStore(application)

    var route by mutableStateOf<Screen>(Screen.Splash)
        private set

    val backStack = mutableStateListOf<Screen>()

    var history by mutableStateOf(if (store.isLoggedIn()) store.loadHistory() else emptyList())
        private set

    var profile by mutableStateOf(if (store.isLoggedIn()) store.loadProfile() else UserProfile())
        private set

    var lastResult by mutableStateOf<ScreeningResult?>(if (store.isLoggedIn()) history.firstOrNull() else null)
        private set

    var chatMessages by mutableStateOf(
        if (store.isLoggedIn()) listOf(welcomeMessage()) + store.loadChatLogs()
        else listOf(welcomeMessage())
    )
        private set

    var isChatGenerating by mutableStateOf(false)
        private set

    var ollamaStatus by mutableStateOf(
        OllamaConnectionStatus(
            isReachable = false,
            activeEndpoint = OllamaRagService.CANDIDATE_ENDPOINTS.first(),
            activeModel = OllamaRagService.DEFAULT_MODEL,
            statusLabel = "RAG · QWEN2.5:3B-INSTRUCT"
        )
    )
        private set

    private val lemonadeClient = LemonadeUnifiedClient()

    var lemonadeStatus by mutableStateOf<LemonadeStatus?>(null)
        private set

    var lemonadeError by mutableStateOf<String?>(null)
        private set

    fun refreshLemonadeStatus() {
        viewModelScope.launch {
            val client = lemonadeClient
            val result = withContext(Dispatchers.IO) { client.listModels() }
            lemonadeStatus = result.getOrNull()
            lemonadeError = result.exceptionOrNull()?.message
        }
    }

    // ---- Dual-module state ----
    var activeModule by mutableStateOf(ScreeningModule.HYPERTENSION)
        private set

    var dualIndex by mutableStateOf(0)
        private set

    var htAnswers by mutableStateOf<Map<String, AnswerValue>>(emptyMap())
        private set

    var gbvAnswers by mutableStateOf<Map<String, AnswerValue>>(emptyMap())
        private set

    var htModuleResult by mutableStateOf<ModuleResult?>(null)
        private set

    var gbvModuleResult by mutableStateOf<ModuleResult?>(null)
        private set

    var integrated by mutableStateOf<IntegratedAnalysis?>(null)
        private set

    private var currentSessionSaved by mutableStateOf(false)

    private fun welcomeMessage() = ChatMessage(
        isUser = false,
        text = "Halo, saya SRYA Emora. Saya di sini untuk menemani Anda memahami kondisi tekanan darah, mengelola beban pikiran atau pola tidur, serta menjadi ruang cerita yang aman. Ada yang sedang Anda rasakan atau ingin kita obrolkan hari ini?"
    )

    fun finishSplash() {
        route = when {
            !store.hasCompletedOnboarding() -> Screen.Onboarding
            !store.isLoggedIn() -> Screen.Auth
            else -> {
                val latest = lastResult
                viewModelScope.launch(Dispatchers.IO) {
                    store.syncPendingResearchToSupabase(latest)
                }
                Screen.Home
            }
        }
    }

    fun completeOnboarding() {
        store.setOnboardingComplete()
        backStack.clear()
        route = if (store.isLoggedIn()) {
            val latest = lastResult
            viewModelScope.launch(Dispatchers.IO) {
                store.syncPendingResearchToSupabase(latest)
            }
            Screen.Home
        } else {
            Screen.Auth
        }
    }

    fun loginAsn(nip: String, pin: String): String? {
        return store.loginAsn(nip, pin, allowCloudRestore = true).fold(
            onSuccess = { loadedProfile ->
                enterSession(loadedProfile)
                null
            },
            onFailure = { it.message ?: "Gagal melakukan otentikasi akun." }
        )
    }

    fun registerAsn(newProfile: UserProfile, pin: String): String? {
        return store.registerAsn(newProfile, pin).fold(
            onSuccess = { savedProfile ->
                enterSession(savedProfile)
                null
            },
            onFailure = { it.message ?: "Gagal mendaftarkan akun." }
        )
    }

    fun logoutAsn() {
        store.logoutAsn()
        resetSessionState()
        route = Screen.Auth
    }

    /** Memuat data milik pengguna yang baru login, tanpa sisa state dari pengguna sebelumnya. */
    private fun enterSession(sessionProfile: UserProfile) {
        resetSessionState()
        profile = sessionProfile
        history = store.loadHistory()
        lastResult = history.firstOrNull()
        val savedChats = store.loadChatLogs()
        chatMessages = listOf(welcomeMessage()) + savedChats
        route = Screen.Home
        val latest = lastResult
        viewModelScope.launch(Dispatchers.IO) {
            store.syncPendingResearchToSupabase(latest)
        }
    }

    private fun resetSessionState() {
        history = emptyList()
        lastResult = null
        chatMessages = listOf(welcomeMessage())
        profile = UserProfile()
        htAnswers = emptyMap()
        gbvAnswers = emptyMap()
        htModuleResult = null
        gbvModuleResult = null
        integrated = null
        activeModule = ScreeningModule.HYPERTENSION
        dualIndex = 0
        backStack.clear()
    }

    fun navigateTo(screen: Screen) {
        backStack.add(route)
        route = screen
    }

    fun selectTab(screen: Screen) {
        backStack.clear()
        route = screen
    }

    fun onBack() {
        if (backStack.isNotEmpty()) {
            route = backStack.removeAt(backStack.lastIndex)
        }
    }

    // ---- Dual-module flow ----
    fun startDualScreening() {
        htAnswers = emptyMap()
        gbvAnswers = emptyMap()
        htModuleResult = null
        gbvModuleResult = null
        integrated = null
        currentSessionSaved = false
        activeModule = ScreeningModule.HYPERTENSION
        dualIndex = 0
        navigateTo(Screen.DualQuestionnaire)
    }

    fun currentQuestions(): List<CfQuestion> = questionsFor(activeModule)

    fun currentQuestion(): CfQuestion {
        val questions = currentQuestions()
        return questions[dualIndex.coerceIn(0, questions.lastIndex)]
    }

    fun answersFor(module: ScreeningModule): Map<String, AnswerValue> =
        if (module == ScreeningModule.HYPERTENSION) htAnswers else gbvAnswers

    fun answerFor(questionId: String): AnswerValue =
        answersFor(activeModule)[questionId] ?: AnswerValue.Unanswered

    fun answerDual(questionId: String, value: AnswerValue) {
        when (activeModule) {
            ScreeningModule.HYPERTENSION -> htAnswers = htAnswers + (questionId to value)
            ScreeningModule.GBV -> gbvAnswers = gbvAnswers + (questionId to value)
        }
    }

    fun nextDualQuestion() {
        val questions = currentQuestions()
        if (dualIndex < questions.lastIndex) dualIndex++ else finishActiveModule()
    }

    fun previousDualQuestion() {
        if (dualIndex > 0) dualIndex-- else onBack()
    }

    private fun persistCurrentScreeningIfNeeded() {
        if (currentSessionSaved) return
        val analysis = integrated ?: return
        val summary = CfResultGenerator.toScreeningResult(analysis)
        lastResult = summary
        store.addHistory(summary)
        history = store.loadHistory()

        val hypertension = htModuleResult
        val gbv = gbvModuleResult
        if (hypertension != null && gbv != null) {
            val presentation = UserResultPresenter.presentIntegrated(hypertension, gbv, analysis)
            UserResultPresenter.buildContextSnapshot(
                lastResult = summary,
                integrated = presentation
            )?.let { store.saveQualitativeSnapshot(it) }

            store.saveResearchRecord(ResearchRecordFactory.create(hypertension, gbv, analysis))
            viewModelScope.launch(Dispatchers.IO) {
                store.syncPendingResearchToSupabase(summary)
            }
        }
        currentSessionSaved = true
    }

    private fun finishActiveModule() {
        when (activeModule) {
            ScreeningModule.HYPERTENSION -> {
                htModuleResult = CfResultGenerator.buildModuleResult(
                    ScreeningModule.HYPERTENSION, hypertensionQuestions, htAnswers
                )
                navigateTo(Screen.ModuleResult)
            }

            ScreeningModule.GBV -> {
                val gbvResult = CfResultGenerator.buildModuleResult(
                    ScreeningModule.GBV, gbvQuestions, gbvAnswers
                )
                gbvModuleResult = gbvResult
                val htResult = htModuleResult ?: CfResultGenerator.buildModuleResult(
                    ScreeningModule.HYPERTENSION, hypertensionQuestions, htAnswers
                )
                htModuleResult = htResult
                val analysis = CfResultGenerator.integrate(htResult, gbvResult)
                integrated = analysis
                persistCurrentScreeningIfNeeded()
                navigateTo(Screen.ModuleResult)
            }
        }
    }

    fun proceedToGbv() {
        activeModule = ScreeningModule.GBV
        dualIndex = 0
        navigateTo(Screen.DualQuestionnaire)
    }

    fun goToIntegrated() {
        navigateTo(Screen.IntegratedResult)
    }

    fun currentModuleResult(): ModuleResult? = when (activeModule) {
        ScreeningModule.HYPERTENSION -> htModuleResult
        ScreeningModule.GBV -> gbvModuleResult
    }

    // ---- User-facing presentation (no internal CF values) ----

    fun userHistory(): List<UserRiskSummary> =
        history.map { UserResultPresenter.presentRiskSummary(it) }

    fun currentModulePresentation(): UserModulePresentation? =
        currentModuleResult()?.let { UserResultPresenter.presentModule(it) }

    fun integratedPresentation(): UserIntegratedPresentation? {
        val hypertension = htModuleResult ?: return null
        val gbv = gbvModuleResult ?: return null
        val analysis = integrated ?: return null
        return UserResultPresenter.presentIntegrated(hypertension, gbv, analysis)
    }

    /**
     * JSON konteks skrining untuk diperiksa manual dari layar hasil. Dipakai
     * untuk memastikan angka dan nama enum yang dikirim ke model sama dengan
     * keluaran mesin.
     */
    fun triageContextDebugJson(): String? = HybridTriageBridge.toDebugJson(
        hypertension = htModuleResult,
        gbv = gbvModuleResult,
        integrated = integrated,
        kbVersion = EmpatiDatabase.CURRENT_KB_VERSION,
        capturedAt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).format(Date())
    )

    /** Internal research data — for authorized research use only, never rendered in UI. */
    fun researchRecords(): List<ResearchRecord> = store.loadResearchRecords()

    fun saveDualResult() {
        persistCurrentScreeningIfNeeded()
        backStack.clear()
        route = Screen.Home
    }

    fun askAboutResult() {
        backStack.clear()
        route = Screen.Chat
        refreshOllamaStatus()
    }

    fun refreshOllamaStatus() {
        viewModelScope.launch {
            val status = withContext(Dispatchers.IO) {
                OllamaRagService.probeStatus()
            }
            ollamaStatus = status
        }
    }

    fun sendChat(text: String) {
        val clean = text.trim()
        if (clean.isBlank() || isChatGenerating) return
        val snapshotResult = lastResult
        val currentIntegrated = integratedPresentation()
        val integratedLabel = currentIntegrated?.categoryLabel
        val richSnapshot = store.loadQualitativeSnapshot(
            lastResult = snapshotResult,
            currentIntegrated = currentIntegrated
        )

        chatMessages = chatMessages + ChatMessage(isUser = true, text = clean)
        isChatGenerating = true

        viewModelScope.launch {
            val replyResult = withContext(Dispatchers.IO) {
                ChatEngine.replyWithRag(
                    message = clean,
                    lastResult = snapshotResult,
                    integratedCategoryLabel = integratedLabel,
                    enableNetworkLlm = true,
                    enableLiveWebSearch = false,
                    contextSnapshot = richSnapshot
                )
            }
            store.appendChatExchange(clean, replyResult)
            viewModelScope.launch(Dispatchers.IO) {
                store.syncChatExchangeToSupabase(clean, replyResult)
            }
            val badge = if (replyResult.usedOllamaLlm) {
                "LLM · ${replyResult.modelName.uppercase()}"
            } else {
                "AI ASISTEN · TERBATAS"
            }
            chatMessages = chatMessages + ChatMessage(
                isUser = false,
                text = replyResult.text,
                retrievedChunks = emptyList(),
                modelBadge = badge
            )
            if (replyResult.usedOllamaLlm && !ollamaStatus.isReachable) {
                ollamaStatus = ollamaStatus.copy(
                    isReachable = true,
                    statusLabel = "LLM · ${replyResult.modelName.uppercase()}"
                )
            }
            isChatGenerating = false
        }
    }

    fun updateProfile(p: UserProfile) {
        store.saveProfile(p)
        profile = store.loadProfile().takeIf { it.name.isNotBlank() || it.nip.isNotBlank() } ?: p
        viewModelScope.launch(Dispatchers.IO) {
            store.syncUserAccountsToSupabase()
        }
    }

    fun clearAllData() {
        val (deletedUserId, deletedSubjectHash) = store.clearCurrentUserData(deleteFromCloud = false)
        resetSessionState()
        route = Screen.Auth
        if (deletedUserId.isNotBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                com.example.empati.data.cloud.SupabaseCloudSyncService.deleteUserAccount(
                    userId = deletedUserId,
                    subjectHash = deletedSubjectHash
                )
            }
        }
    }
}
