package com.example.empati.data.lemonade

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.InetSocketAddress
import java.net.ServerSocket

class LemonadeUnifiedClientTest {

    private data class Captured(
        val method: String,
        val path: String,
        val headers: Map<String, String>,
        val body: ByteArray,
        val contentType: String?
    ) {
        val bodyText: String get() = String(body, Charsets.UTF_8)
    }

    private data class MockResponse(
        val status: Int,
        val body: ByteArray,
        val contentType: String
    ) {
        constructor(status: Int, body: String, contentType: String = "application/json") :
            this(status, body.toByteArray(Charsets.UTF_8), contentType)
    }

    private lateinit var server: HttpServer
    private var port: Int = 0
    private val captured = mutableListOf<Captured>()
    private var responder: (Captured) -> MockResponse = {
        MockResponse(404, """{"error":{"code":"model_not_found"}}""")
    }

    @Before
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange -> handle(exchange) }
        server.executor = null
        server.start()
        port = server.address.port
    }

    @After
    fun stopServer() {
        server.stop(0)
    }

    private fun handle(exchange: HttpExchange) {
        val body = exchange.requestBody.readBytes()
        val headers = exchange.requestHeaders.entries.associate { it.key.lowercase() to it.value.first() }
        val request = Captured(
            method = exchange.requestMethod,
            path = exchange.requestURI.path,
            headers = headers,
            body = body,
            contentType = exchange.requestHeaders.getFirst("Content-Type")
        )
        captured += request
        val response = responder(request)
        exchange.responseHeaders.add("Content-Type", response.contentType)
        exchange.sendResponseHeaders(response.status, response.body.size.toLong())
        exchange.responseBody.use { it.write(response.body) }
    }

    private fun client(apiKey: String = "test-key") =
        LemonadeUnifiedClient(baseUrl = "http://127.0.0.1:$port", apiKey = apiKey)

    private fun only(path: String): Captured {
        val matches = captured.filter { it.path == path }
        assertEquals("expected exactly one request to $path", 1, matches.size)
        return matches.single()
    }

    @Test
    fun `chat completion posts alias messages and parses assistant text`() {
        responder = {
            MockResponse(
                200,
                """{"model":"empati-llm","choices":[{"message":{"role":"assistant","content":"Halo, saya Empati."},"finish_reason":"stop"}]}"""
            )
        }

        val completion = client()
            .chatCompletion(listOf(ChatMessage("user", "saya pusing sejak kemarin")))
            .getOrThrow()

        assertEquals("Halo, saya Empati.", completion.text)
        assertEquals("stop", completion.finishReason)

        val request = only("/v1/chat/completions")
        assertEquals("POST", request.method)
        assertTrue(request.bodyText.contains("\"model\":\"empati-llm\""))
        assertTrue(request.bodyText.contains("saya pusing sejak kemarin"))
        assertTrue(request.bodyText.contains("\"role\":\"user\""))
        assertEquals("Bearer test-key", request.headers["authorization"])
    }

    @Test
    fun `chat completion omits authorization header when no key configured`() {
        responder = {
            MockResponse(200, """{"choices":[{"message":{"content":"ok"}}]}""")
        }

        client(apiKey = "").chatCompletion(listOf(ChatMessage("user", "halo"))).getOrThrow()

        val request = only("/v1/chat/completions")
        assertNull(request.headers["authorization"])
    }

    @Test
    fun `streaming chat concatenates deltas and stops at the done marker`() {
        val sse = buildString {
            append("data: {\"choices\":[{\"delta\":{\"role\":\"assistant\",\"content\":\"Halo\"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\", \"}}]}\n\n")
            append("data: {\"choices\":[{\"delta\":{\"content\":\"dunia\"}}]}\n\n")
            append("data: [DONE]\n\n")
        }
        responder = { MockResponse(200, sse, "text/event-stream") }

        val deltas = mutableListOf<String>()
        val result = client().streamChatCompletion(
            messages = listOf(ChatMessage("user", "halo")),
            onDelta = { deltas += it }
        ).getOrThrow()

        assertEquals(listOf("Halo", ", ", "dunia"), deltas)
        assertEquals("Halo, dunia", result.text)
    }

    @Test
    fun `embeddings returns vectors in input index order`() {
        responder = {
            MockResponse(
                200,
                """{"model":"empati-embed","data":[{"index":1,"embedding":[0.5,0.25]},{"index":0,"embedding":[0.1,0.2]}]}"""
            )
        }

        val vectors = client()
            .generateEmbeddings(listOf("hipertensi", "gaya hidup"))
            .getOrThrow()

        assertEquals(2, vectors.size)
        assertEquals(0, vectors[0].index)
        assertEquals(0.1f, vectors[0].values[0], 0.0001f)
        assertEquals(1, vectors[1].index)
        assertEquals(0.5f, vectors[1].values[0], 0.0001f)

        val request = only("/v1/embeddings")
        assertTrue(request.bodyText.contains("\"model\":\"empati-embed\""))
        assertTrue(request.bodyText.contains("\"hipertensi\""))
    }

    @Test
    fun `transcription posts multipart wav with alias model and language`() {
        responder = { MockResponse(200, """{"text":"Saya pusing sejak kemarin."}""") }

        val transcript = client()
            .transcribeAudio(wav = WavCodec.wrapPcm16(ShortArray(16)), language = "id")
            .getOrThrow()

        assertEquals("Saya pusing sejak kemarin.", transcript.text)

        val request = only("/v1/audio/transcriptions")
        assertEquals("POST", request.method)
        assertNotNull(request.contentType)
        assertTrue(request.contentType!!.startsWith("multipart/form-data; boundary="))
        val body = request.bodyText
        assertTrue(body.contains("name=\"model\""))
        assertTrue(body.contains("empati-stt"))
        assertTrue(body.contains("name=\"language\""))
        assertTrue(body.contains("id"))
        assertTrue(body.contains("filename=\"complaint.wav\""))
        assertTrue(body.contains("Content-Type: audio/wav"))
        assertTrue(body.contains("RIFF"))

        val boundary = request.contentType!!.substringAfter("boundary=")
        assertTrue("first part must use the same boundary", body.startsWith("--$boundary\r\n"))
        assertTrue("body must terminate with the same boundary", body.trimEnd().endsWith("--$boundary--"))
    }

    @Test
    fun `list models reports downloaded models and missing aliases`() {
        responder = {
            MockResponse(
                200,
                """{"data":[
                    {"id":"empati-llm","recipe":"llamacpp","labels":["chat"],"downloaded":true},
                    {"id":"empati-embed","recipe":"llamacpp","labels":["embeddings"],"downloaded":true}
                ]}"""
            )
        }

        val status = client().listModels().getOrThrow()

        assertEquals(2, status.models.size)
        assertEquals(listOf("empati-stt", "empati-tts"), status.missingAliases)
        assertTrue(!status.isReady)

        val request = only("/v1/models")
        assertEquals("GET", request.method)
    }

    @Test
    fun `read timeout maps to Unreachable`() {
        responder = {
            Thread.sleep(400)
            MockResponse(200, """{"choices":[{"message":{"content":"late"}}]}""")
        }
        val impatient = LemonadeUnifiedClient(
            baseUrl = "http://127.0.0.1:$port",
            apiKey = "",
            connectTimeoutMs = 1_000,
            readTimeoutMs = 100
        )

        val failure = impatient.chatCompletion(listOf(ChatMessage("user", "halo"))).exceptionOrNull()

        assertTrue("expected Unreachable but was $failure", failure is LemonadeFailure.Unreachable)
    }

    @Test
    fun `lemonade client sources never reference the ollama port`() {
        val sourceDir = java.io.File("src/main/java/com/example/empati/data/lemonade")
        org.junit.Assume.assumeTrue(sourceDir.isDirectory)

        val offenders = sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { it.readText().contains("11434") || it.readText().contains("/api/chat") }
            .map { it.name }
            .toList()

        assertTrue("unexpected ollama references: $offenders", offenders.isEmpty())
    }

    @Test
    fun `speech synthesis requests mp3 and returns audio bytes`() {
        val audio = byteArrayOf(1, 2, 3, 4, 5)
        responder = { MockResponse(200, audio, "audio/mpeg") }

        val result = client().synthesizeSpeech("Hasil skrining Anda menunjukkan indikator sedang.").getOrThrow()

        assertTrue(result.contentEquals(audio))

        val request = only("/v1/audio/speech")
        assertTrue(request.bodyText.contains("\"model\":\"empati-tts\""))
        assertTrue(request.bodyText.contains("\"response_format\":\"mp3\""))
        assertTrue(request.bodyText.contains("Hasil skrining Anda"))
    }

    @Test
    fun `missing model maps to ModelNotReady`() {
        responder = {
            MockResponse(404, """{"error":{"code":"model_not_found","message":"model 'empati-llm' not found"}}""")
        }

        val failure = client()
            .chatCompletion(listOf(ChatMessage("user", "halo")))
            .exceptionOrNull()

        assertTrue("expected ModelNotReady but was $failure", failure is LemonadeFailure.ModelNotReady)
    }

    @Test
    fun `pinned slots map to Busy`() {
        responder = {
            MockResponse(409, """{"error":{"code":"slots_pinned_error","message":"all slots pinned"}}""")
        }

        val failure = client()
            .chatCompletion(listOf(ChatMessage("user", "halo")))
            .exceptionOrNull()

        assertTrue("expected Busy but was $failure", failure is LemonadeFailure.Busy)
    }

    @Test
    fun `connection refused maps to Unreachable`() {
        val closedPort = ServerSocket(0).use { it.localPort }
        val offlineClient = LemonadeUnifiedClient(
            baseUrl = "http://127.0.0.1:$closedPort",
            apiKey = "",
            connectTimeoutMs = 1_500,
            readTimeoutMs = 1_500
        )

        val failure = offlineClient.chatCompletion(listOf(ChatMessage("user", "halo"))).exceptionOrNull()

        assertTrue("expected Unreachable but was $failure", failure is LemonadeFailure.Unreachable)
    }

    @Test
    fun `base url normalizes scheme and trailing slash`() {
        assertEquals("http://192.168.1.7:13305", LemonadeConfig.normalizeBaseUrl("192.168.1.7:13305"))
        assertEquals("http://10.0.2.2:13305", LemonadeConfig.normalizeBaseUrl("http://10.0.2.2:13305/"))
        assertEquals("http://10.0.2.2:13305", LemonadeConfig.normalizeBaseUrl("  "))
        assertEquals("http://127.0.0.1:13305", LemonadeConfig.normalizeBaseUrl("http://127.0.0.1:13305"))
    }
}
