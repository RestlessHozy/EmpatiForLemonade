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
    private val model: ChatModel
) : ConversationGateway {

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

        val raw = runCatching { model.complete(EmpatiPromptBuilder.SYSTEM_PROMPT, userPrompt) }
            .getOrElse {
                return ChatReplyResult(
                    text = "Maaf, model lokal belum bisa dihubungi saat ini. Saya belum bisa " +
                        "menyusun jawaban yang lengkap. Coba lagi sebentar lagi, atau buka menu " +
                        "skrining kalau ingin meninjau hasil Anda.",
                    modelName = "fallback-bounded",
                    referenceChunks = context.chunks
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
