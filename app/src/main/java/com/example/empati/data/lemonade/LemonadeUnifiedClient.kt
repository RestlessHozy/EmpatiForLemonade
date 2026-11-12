package com.example.empati.data.lemonade

import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

/**
 * Satu-satunya pintu ke Lemonade Server. Semua permintaan memakai port 13305 dan
 * kontrak alias dari [LemonadeConfig], sehingga nama model tidak pernah masuk ke
 * kode aplikasi dan tidak ada jalur kedua ke Ollama.
 */
class LemonadeUnifiedClient(
    baseUrl: String = LemonadeConfig.defaultBaseUrl,
    private val apiKey: String = "",
    private val connectTimeoutMs: Int = 5_000,
    private val readTimeoutMs: Int = 60_000
) {

    private val baseUrl: String = LemonadeConfig.normalizeBaseUrl(baseUrl)

    fun listModels(): Result<LemonadeStatus> = attempt {
        val payload = exchange(open("/v1/models", "GET"), null, null)
        val root = parseObject(payload)
        val data = root["data"] as? List<*> ?: emptyList<Any?>()
        val models = data.mapNotNull { entry ->
            val item = entry as? Map<*, *> ?: return@mapNotNull null
            val id = item["id"] as? String ?: return@mapNotNull null
            ModelInfo(
                id = id,
                recipe = item["recipe"] as? String,
                labels = (item["labels"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                downloaded = item["downloaded"] as? Boolean ?: false
            )
        }
        val ids = models.map { it.id }.toSet()
        LemonadeStatus(
            models = models,
            missingAliases = LemonadeConfig.REQUIRED_ALIASES.filterNot { it in ids }
        )
    }

    fun transcribeAudio(
        wav: ByteArray,
        fileName: String = "complaint.wav",
        language: String = "id",
        model: String = LemonadeConfig.ALIAS_STT
    ): Result<Transcript> = attempt {
        val boundary = "----EmpatiBoundary" + UUID.randomUUID().toString().replace("-", "")
        val body = buildMultipart(boundary, wav, fileName, language, model)
        val payload = exchange(
            connection = open("/v1/audio/transcriptions", "POST"),
            body = body,
            contentType = "multipart/form-data; boundary=$boundary"
        )
        val root = parseObject(payload)
        Transcript(
            text = root["text"] as? String ?: "",
            model = root["model"] as? String
        )
    }

    fun generateEmbeddings(
        inputs: List<String>,
        model: String = LemonadeConfig.ALIAS_EMBED
    ): Result<List<EmbeddingVector>> = attempt {
        val payload = MiniJson.write(
            mapOf(
                "model" to model,
                "input" to inputs,
                "encoding_format" to "float"
            )
        )
        val response = exchange(
            connection = open("/v1/embeddings", "POST"),
            body = payload.toByteArray(Charsets.UTF_8),
            contentType = "application/json"
        )
        val root = parseObject(response)
        val data = root["data"] as? List<*> ?: throw LemonadeFailure.ProtocolViolation("missing data")
        data.mapNotNull { entry ->
            val item = entry as? Map<*, *> ?: return@mapNotNull null
            val vector = item["embedding"] as? List<*> ?: return@mapNotNull null
            EmbeddingVector(
                index = (item["index"] as? Number)?.toInt() ?: 0,
                values = vector.mapNotNull { (it as? Number)?.toFloat() }.toFloatArray()
            )
        }.sortedBy { it.index }
    }

    fun chatCompletion(
        messages: List<ChatMessage>,
        model: String = LemonadeConfig.ALIAS_LLM,
        temperature: Double = 0.3,
        maxTokens: Int = 512
    ): Result<ChatCompletion> = attempt {
        val payload = MiniJson.write(
            mapOf(
                "model" to model,
                "messages" to messages.toWire(),
                "temperature" to temperature,
                "max_tokens" to maxTokens,
                "stream" to false
            )
        )
        val response = exchange(
            connection = open("/v1/chat/completions", "POST"),
            body = payload.toByteArray(Charsets.UTF_8),
            contentType = "application/json"
        )
        parseChatCompletion(response, model)
    }

    fun streamChatCompletion(
        messages: List<ChatMessage>,
        model: String = LemonadeConfig.ALIAS_LLM,
        temperature: Double = 0.3,
        maxTokens: Int = 512,
        onDelta: (String) -> Unit
    ): Result<ChatCompletion> = attempt {
        val payload = MiniJson.write(
            mapOf(
                "model" to model,
                "messages" to messages.toWire(),
                "temperature" to temperature,
                "max_tokens" to maxTokens,
                "stream" to true
            )
        )
        val connection = open("/v1/chat/completions", "POST", accept = "text/event-stream")
        writeBody(connection, payload.toByteArray(Charsets.UTF_8), "application/json")

        val status = connection.responseCode
        if (status !in 200..299) {
            val error = connection.errorStream?.use { it.readBytes() } ?: ByteArray(0)
            throw failureFor(status, error)
        }

        val collected = StringBuilder()
        var finishReason: String? = null
        connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                val line = reader.readLine() ?: break
                val trimmed = line.trim()
                if (!trimmed.startsWith("data:")) continue
                val data = trimmed.removePrefix("data:").trim()
                if (data == "[DONE]") break
                val chunk = runCatching { MiniJson.parse(data) as? Map<*, *> }.getOrNull() ?: continue
                val choices = chunk["choices"] as? List<*> ?: continue
                val choice = choices.firstOrNull() as? Map<*, *> ?: continue
                val delta = (choice["delta"] as? Map<*, *>)?.get("content") as? String
                if (!delta.isNullOrEmpty()) {
                    collected.append(delta)
                    onDelta(delta)
                }
                (choice["finish_reason"] as? String)?.let { finishReason = it }
            }
        }
        ChatCompletion(
            text = collected.toString(),
            model = model,
            finishReason = finishReason
        )
    }

    fun synthesizeSpeech(
        text: String,
        voice: String = "af_sky",
        format: String = "mp3",
        model: String = LemonadeConfig.ALIAS_TTS
    ): Result<ByteArray> = attempt {
        val payload = MiniJson.write(
            mapOf(
                "model" to model,
                "input" to text,
                "voice" to voice,
                "speed" to 1.0,
                "response_format" to format
            )
        )
        exchange(
            connection = open("/v1/audio/speech", "POST", accept = "audio/mpeg"),
            body = payload.toByteArray(Charsets.UTF_8),
            contentType = "application/json"
        )
    }

    private fun List<ChatMessage>.toWire(): List<Map<String, String>> =
        map { mapOf("role" to it.role, "content" to it.content) }

    private fun parseChatCompletion(payload: ByteArray, fallbackModel: String): ChatCompletion {
        val root = parseObject(payload)
        val choices = root["choices"] as? List<*> ?: throw LemonadeFailure.ProtocolViolation("missing choices")
        val choice = choices.firstOrNull() as? Map<*, *>
            ?: throw LemonadeFailure.ProtocolViolation("empty choices")
        val message = choice["message"] as? Map<*, *>
        return ChatCompletion(
            text = message?.get("content") as? String ?: "",
            model = root["model"] as? String ?: fallbackModel,
            finishReason = choice["finish_reason"] as? String
        )
    }

    private fun parseObject(payload: ByteArray): Map<*, *> {
        val text = String(payload, Charsets.UTF_8)
        return runCatching { MiniJson.parse(text) as? Map<*, *> }.getOrNull()
            ?: throw LemonadeFailure.ProtocolViolation(text.take(160))
    }

    private fun open(path: String, method: String, accept: String? = "application/json"): HttpURLConnection {
        val connection = URL("$baseUrl$path").openConnection() as HttpURLConnection
        connection.requestMethod = method
        connection.connectTimeout = connectTimeoutMs
        connection.readTimeout = readTimeoutMs
        if (apiKey.isNotBlank()) {
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
        }
        if (accept != null) {
            connection.setRequestProperty("Accept", accept)
        }
        return connection
    }

    private fun writeBody(connection: HttpURLConnection, body: ByteArray, contentType: String) {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", contentType)
        connection.setRequestProperty("Content-Length", body.size.toString())
        connection.outputStream.use { it.write(body) }
    }

    private fun exchange(connection: HttpURLConnection, body: ByteArray?, contentType: String?): ByteArray {
        if (body != null && contentType != null) {
            writeBody(connection, body, contentType)
        }
        val status = connection.responseCode
        val stream = if (status in 200..299) connection.inputStream else connection.errorStream
        val payload = stream?.use { it.readBytes() } ?: ByteArray(0)
        if (status !in 200..299) throw failureFor(status, payload)
        return payload
    }

    private fun failureFor(status: Int, payload: ByteArray): LemonadeFailure {
        val text = String(payload, Charsets.UTF_8)
        val root = runCatching { MiniJson.parse(text) as? Map<*, *> }.getOrNull()
        val error = root?.get("error")
        val code = when (error) {
            is Map<*, *> -> error["code"] as? String
            is String -> error
            else -> root?.get("code") as? String
        }
        val message = when (error) {
            is Map<*, *> -> error["message"] as? String
            is String -> error
            else -> root?.get("message") as? String
        }
        return when {
            status == 404 && code == "model_not_found" ->
                LemonadeFailure.ModelNotReady(extractModelName(text), message)
            status == 500 && code == "model_load_error" ->
                LemonadeFailure.ModelNotReady(extractModelName(text), message)
            status == 409 && (code == "slots_pinned_error" || code == "router_residency_conflict") ->
                LemonadeFailure.Busy(message)
            else -> LemonadeFailure.Http(status, code, text)
        }
    }

    private fun extractModelName(text: String): String {
        val quoted = Regex("'([^']+)'").find(text)?.groupValues?.getOrNull(1)
        if (!quoted.isNullOrBlank()) return quoted
        val bare = Regex("\"model\"\\s*:\\s*\"([^\"]+)\"").find(text)?.groupValues?.getOrNull(1)
        return bare ?: "unknown"
    }

    private fun buildMultipart(
        boundary: String,
        wav: ByteArray,
        fileName: String,
        language: String,
        model: String
    ): ByteArray {
        val safeFileName = fileName.replace("\"", "").replace("\r", "").replace("\n", "")
        val out = ByteArrayOutputStream()
        fun part(name: String, value: String) {
            out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
            out.write("Content-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
            out.write(value.toByteArray(Charsets.UTF_8))
            out.write("\r\n".toByteArray(Charsets.UTF_8))
        }

        part("model", model)
        part("language", language)
        part("response_format", "json")

        out.write("--$boundary\r\n".toByteArray(Charsets.UTF_8))
        out.write(
            "Content-Disposition: form-data; name=\"file\"; filename=\"$safeFileName\"\r\n"
                .toByteArray(Charsets.UTF_8)
        )
        out.write("Content-Type: audio/wav\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(wav)
        out.write("\r\n".toByteArray(Charsets.UTF_8))
        out.write("--$boundary--\r\n".toByteArray(Charsets.UTF_8))
        return out.toByteArray()
    }

    private fun <T> attempt(block: () -> T): Result<T> = try {
        Result.success(block())
    } catch (failure: LemonadeFailure) {
        Result.failure(failure)
    } catch (io: java.io.IOException) {
        Result.failure(LemonadeFailure.Unreachable(io))
    } catch (error: Exception) {
        Result.failure(
            LemonadeFailure.ProtocolViolation(error.message ?: error::class.java.simpleName)
        )
    }
}
