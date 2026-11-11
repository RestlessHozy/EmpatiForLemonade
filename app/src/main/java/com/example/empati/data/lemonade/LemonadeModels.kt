package com.example.empati.data.lemonade

data class ChatMessage(val role: String, val content: String)

data class ChatCompletion(
    val text: String,
    val model: String,
    val finishReason: String?
)

data class Transcript(
    val text: String,
    val model: String?
)

data class EmbeddingVector(
    val index: Int,
    val values: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EmbeddingVector) return false
        return index == other.index && values.contentEquals(other.values)
    }

    override fun hashCode(): Int = 31 * index + values.contentHashCode()
}

data class ModelInfo(
    val id: String,
    val recipe: String?,
    val labels: List<String>,
    val downloaded: Boolean
)

data class LemonadeStatus(
    val models: List<ModelInfo>,
    val missingAliases: List<String>
) {
    val isReady: Boolean get() = missingAliases.isEmpty()
}

/**
 * Kegagalan yang dibedakan supaya UI bisa memilih pesan dan fallback yang tepat.
 * Tiga kasus pertama adalah yang muncul di demo: server mati, model belum diunduh,
 * dan model sibuk.
 */
sealed class LemonadeFailure(message: String, cause: Throwable? = null) : Exception(message, cause) {

    class Unreachable(cause: Throwable?) :
        LemonadeFailure("Server Lemonade tidak dapat dihubungi", cause)

    class ModelNotReady(val model: String, val serverMessage: String?) :
        LemonadeFailure("Model belum siap: $model${serverMessage?.let { " ($it)" } ?: ""}")

    class Busy(val serverMessage: String?) :
        LemonadeFailure("Model sedang dipakai${serverMessage?.let { ": $it" } ?: ""}")

    class Http(val status: Int, val code: String?, val body: String) :
        LemonadeFailure("Lemonade membalas HTTP $status${code?.let { " ($it)" } ?: ""}")

    class ProtocolViolation(val detail: String) :
        LemonadeFailure("Balasan Lemonade tidak dikenali: $detail")
}
