package com.example.empati.data

import com.example.empati.data.chat.ChatRequestContext
import com.example.empati.data.chat.ChatRoute
import com.example.empati.data.chat.ConversationGateway
import com.example.empati.data.chat.EmpatiChatUseCase
import com.example.empati.data.presentation.UserResultPresenter
import com.example.empati.data.rag.OllamaRagService
import com.example.empati.data.rag.RagDocumentChunk
import com.example.empati.data.rag.RagRetrievalResult
import com.example.empati.data.rag.ScreeningContextSnapshot
import com.example.empati.data.triage.ReferenceChunk

data class ChatReplyResult(
    val text: String,
    val retrievedChunks: List<RagDocumentChunk> = emptyList(),
    val retrievedChunkIds: List<String> = emptyList(),
    val usedOllamaLlm: Boolean = false,
    val fetchedFromInternet: Boolean = false,
    val modelName: String = "local",
    val referenceChunks: List<ReferenceChunk> = emptyList(),
    val isPartial: Boolean = false,
    val failure: Throwable? = null,
    val route: ChatRoute = ChatRoute.LEMONADE
)

/**
 * Adaptor lama untuk pemanggil yang belum memakai use case. Orkestrasi dan
 * aturan jawaban deterministik tinggal di [EmpatiChatUseCase].
 */
object ChatEngine {

    fun reply(message: String, lastResult: ScreeningResult?): String =
        replyWithRag(message, lastResult).text

    fun replyWithRag(
        message: String,
        lastResult: ScreeningResult?,
        corpus: List<RagDocumentChunk> = emptyList(),
        integratedCategoryLabel: String? = null,
        enableNetworkLlm: Boolean = false,
        enableLiveWebSearch: Boolean = false,
        contextSnapshot: ScreeningContextSnapshot? = null
    ): ChatReplyResult {
        val snapshot = contextSnapshot ?: UserResultPresenter.buildContextSnapshot(
            lastResult = lastResult,
            fallbackCategoryLabel = integratedCategoryLabel
        )

        val gateway = object : ConversationGateway {
            override fun generate(query: String, context: ChatRequestContext): ChatReplyResult {
                val outcome = OllamaRagService.generateRagResponse(
                    rawQuery = query,
                    retrieval = RagRetrievalResult(
                        query = query,
                        matchedChunks = emptyList(),
                        contextSnapshot = context.snapshot
                    ),
                    enableNetworkLlm = enableNetworkLlm
                )
                return ChatReplyResult(
                    text = outcome.text,
                    usedOllamaLlm = outcome.usedOllamaLlm,
                    modelName = outcome.modelName
                )
            }
        }

        return EmpatiChatUseCase(gateway).reply(message, snapshot)
    }
}
