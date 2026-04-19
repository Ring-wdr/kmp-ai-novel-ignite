package io.github.ringwdr.novelignite.data.inference

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class DesktopOllamaModelsClientTest {
    private lateinit var server: HttpServer
    private lateinit var baseUrl: String

    @BeforeTest
    fun setUp() {
        server = HttpServer.create(InetSocketAddress(0), 0)
        server.start()
        baseUrl = "http://localhost:${server.address.port}"
    }

    @AfterTest
    fun tearDown() {
        server.stop(0)
    }

    @Test
    fun streamGenerate_returnsResponseChunksInOrder_andSendsUtf8Request() = runTest {
        var request: RecordedGenerateRequest? = null
        var method: String? = null
        var contentType: String? = null
        var accept: String? = null

        server.createContext("/api/generate") { exchange ->
            method = exchange.requestMethod
            contentType = exchange.requestHeaders.getFirst("Content-Type")
            accept = exchange.requestHeaders.getFirst("Accept")
            val bodyText = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            request = Json.decodeFromString(RecordedGenerateRequest.serializer(), bodyText)
            respondWithNdjson(
                exchange = exchange,
                body = """
                    {"response":"café"}
                    {"response":" 世界","done":true}
                """.trimIndent(),
            )
        }

        val client = DesktopOllamaModelsClient(baseUrl = baseUrl)
        val chunks = client.streamGenerate(
            model = "llama3",
            prompt = "Write café in a poetic tone.",
        ).toList()

        assertEquals("POST", method)
        assertEquals("application/json", contentType)
        assertEquals("application/x-ndjson", accept)
        assertEquals(
            RecordedGenerateRequest(
                model = "llama3",
                prompt = "Write café in a poetic tone.",
                stream = true,
            ),
            request,
        )
        assertEquals(listOf("café", " 世界"), chunks)
    }

    @Test
    fun streamGenerate_failsWhenTerminalDoneFrameIsMissing() = runTest {
        server.createContext("/api/generate") { exchange ->
            respondWithNdjson(
                exchange = exchange,
                body = """
                    {"response":"partial text"}
                """.trimIndent(),
            )
        }

        val client = DesktopOllamaModelsClient(baseUrl = baseUrl)

        val error = assertFailsWith<IllegalStateException> {
            client.streamGenerate(
                model = "llama3",
                prompt = "Continue.",
            ).toList()
        }

        assertEquals("Ollama stream ended before done=true.", error.message)
    }

    @Test
    fun generate_returnsFullResponseBody() = runTest {
        var request: RecordedGenerateRequest? = null

        server.createContext("/api/generate") { exchange ->
            val bodyText = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            request = Json.decodeFromString(RecordedGenerateRequest.serializer(), bodyText)
            val bytes = """{"response":"full answer"}""".toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }

        val client = DesktopOllamaModelsClient(baseUrl = baseUrl)
        val response = client.generate(
            model = "llama3",
            prompt = "Expand this template.",
        )

        assertEquals(
            RecordedGenerateRequest(
                model = "llama3",
                prompt = "Expand this template.",
                stream = false,
            ),
            request,
        )
        assertEquals("full answer", response)
    }

    private fun respondWithNdjson(exchange: HttpExchange, body: String) {
        exchange.responseHeaders.add("Content-Type", "application/x-ndjson")
        val bytes = body.toByteArray(Charsets.UTF_8)
        exchange.sendResponseHeaders(200, bytes.size.toLong())
        exchange.responseBody.use { it.write(bytes) }
        exchange.close()
    }
}

@Serializable
private data class RecordedGenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
)
