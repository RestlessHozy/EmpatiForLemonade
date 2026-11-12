package com.example.empati.data.chat

import com.example.empati.data.ChatReplyResult
import com.example.empati.data.cf.CfLevel
import com.example.empati.data.cf.ScreeningModule
import com.example.empati.data.triage.EmpatiPromptBuilder
import com.example.empati.data.triage.TriageContext

fun interface ChatModel {
    fun complete(systemPrompt: String, userPrompt: String): String
}

/**
 * Gateway yang menyusun prompt dari konteks skrining dan potongan rujukan,
 * memanggil model, lalu membersihkan hasilnya. Semua fakta klinis harus
 * berasal dari potongan rujukan; bila tidak ada, prompt meminta penolakan.
 */
class GroundedChatGateway(
    private val model: ChatModel,
    private val cloud: ((String, String) -> String)? = null,
    private val streamer: ((String, String, (String) -> Unit) -> String)? = null
) : ConversationGateway {

    /**
     * Satu percobaan streaming. Bila gagal setelah ada teks, teks parsial
     * dipertahankan dan ditandai belum lengkap. Bila gagal sebelum ada teks,
     * permintaan diulang sekali tanpa streaming.
     */
    override fun generateStreaming(
        query: String,
        context: ChatRequestContext,
        onDelta: (String) -> Unit
    ): ChatReplyResult {
        val userPrompt = EmpatiPromptBuilder.buildUserPrompt(
            query = query,
            context = context.triage,
            chunks = context.chunks
        )
        val gbv = deriveGbv(context)
        val received = StringBuilder()
        var streamFailure: Exception? = null

        if (streamer != null) {
            val streamed = try {
                streamer(EmpatiPromptBuilder.SYSTEM_PROMPT, userPrompt) { delta ->
                    if (delta.isNotEmpty()) {
                        received.append(delta)
                        onDelta(delta)
                    }
                }
            } catch (error: Exception) {
                streamFailure = error
                null
            }

            if (streamed != null) {
                val guarded = AnswerGuardrail.enforce(streamed, gbv)
                return ChatReplyResult(
                    text = guarded.text,
                    modelName = "lemonade-stream",
                    referenceChunks = context.chunks
                )
            }

            if (received.isNotEmpty()) {
                val guarded = AnswerGuardrail.enforce(received.toString(), gbv)
                return ChatReplyResult(
                    text = guarded.text,
                    modelName = "lemonade-partial",
                    referenceChunks = context.chunks,
                    isPartial = true,
                    failure = streamFailure
                )
            }
        }

        return generate(query, context, gbv)
    }

    override fun generate(query: String, context: ChatRequestContext): ChatReplyResult =
        generate(query, context, gbvIndicated = deriveGbv(context))

    fun generate(
        query: String,
        context: ChatRequestContext,
        gbvIndicated: Boolean
    ): ChatReplyResult {
        val userPrompt = EmpatiPromptBuilder.buildUserPrompt(
            query = query,
            context = context.triage,
            chunks = context.chunks
        )

        val raw = try {
            model.complete(EmpatiPromptBuilder.SYSTEM_PROMPT, userPrompt)
        } catch (error: Exception) {
            val outcome = ChatFallbackLadder(
                lemonade = { throw error },
                cloud = cloud?.let { call -> { call(EmpatiPromptBuilder.SYSTEM_PROMPT, userPrompt) } }
            ).answer()
            val guarded = AnswerGuardrail.enforce(outcome.text, gbvIndicated)
            return ChatReplyResult(
                text = guarded.text,
                modelName = if (outcome.route == ChatRoute.CLOUD) "cloud-fallback" else "fallback-bounded",
                referenceChunks = context.chunks,
                failure = error,
                route = outcome.route
            )
        }

        val guarded = AnswerGuardrail.enforce(raw, gbvIndicated)
        return ChatReplyResult(
            text = guarded.text,
            modelName = "lemonade-grounded",
            referenceChunks = context.chunks
        )
    }

    private fun deriveGbv(context: ChatRequestContext): Boolean {
        val gbvModule = context.triage?.modules?.firstOrNull { it.module == ScreeningModule.GBV }
        val fromEngine = gbvModule != null && gbvModule.cf > 0.0 && gbvModule.level != CfLevel.RENDAH
        val fromSnapshot = context.snapshot?.gbvStatusLabel
            ?.trim()
            ?.lowercase()
            ?.startsWith("terdapat") == true
        return fromEngine || fromSnapshot
    }
}
