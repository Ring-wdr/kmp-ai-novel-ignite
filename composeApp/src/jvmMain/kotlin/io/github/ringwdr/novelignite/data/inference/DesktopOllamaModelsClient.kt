package io.github.ringwdr.novelignite.data.inference

import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class DesktopOllamaModelsClient(
    private val baseUrl: String,
    private val httpClient: HttpClient = HttpClient.newHttpClient(),
) : OllamaModelsClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun listModels(): List<String> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$normalizedBaseUrl/api/tags"))
            .GET()
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Failed to load Ollama models: ${response.body()}")
        }
        return json.decodeFromString<TagsResponse>(response.body())
            .models
            .map { it.name }
    }

    override suspend fun generate(model: String, prompt: String): String {
        val requestBody = json.encodeToString(
            GenerateRequest(
                model = model,
                prompt = prompt,
                stream = false,
            )
        )
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$normalizedBaseUrl/api/generate"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Ollama generate failed: ${response.body()}")
        }
        return json.decodeFromString<GenerateResponse>(response.body()).response
    }

    override fun streamGenerate(model: String, prompt: String): Flow<String> = flow {
        val requestBody = json.encodeToString(
            GenerateRequest(
                model = model,
                prompt = prompt,
                stream = true,
            )
        )
        val connection = (URI.create("$normalizedBaseUrl/api/generate").toURL().openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/x-ndjson")
            doOutput = true
            useCaches = false
        }

        val job = currentCoroutineContext()[kotlinx.coroutines.Job]
            ?: throw IllegalStateException("Missing coroutine job.")
        val cancellationHandle = job.invokeOnCompletion {
            connection.disconnect()
        }

        try {
            connection.outputStream.use { output ->
                output.write(requestBody.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                val responseStream = connection.errorStream
                val message = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                throw IllegalStateException("Ollama generate failed: $message")
            }

            var sawDone = false
            connection.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                for (line in lines) {
                    val trimmedLine = line.trim()
                    if (trimmedLine.isEmpty()) continue
                    val chunk = json.decodeFromString<GenerateStreamResponse>(trimmedLine)
                    if (chunk.response.isNotEmpty()) emit(chunk.response)
                    if (chunk.done) {
                        sawDone = true
                        break
                    }
                }
            }

            if (!sawDone) {
                throw IllegalStateException("Ollama stream ended before done=true.")
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } finally {
            cancellationHandle.dispose()
            connection.disconnect()
        }
    }
}

@Serializable
private data class GenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
)

@Serializable
private data class GenerateStreamResponse(
    val response: String = "",
    val done: Boolean = false,
)

@Serializable
private data class GenerateResponse(
    val response: String = "",
)

@Serializable
private data class TagsResponse(
    val models: List<TagModel> = emptyList(),
)

@Serializable
private data class TagModel(
    @SerialName("name")
    val name: String,
)
