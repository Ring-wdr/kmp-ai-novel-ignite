package io.github.ringwdr.novelignite.data.inference

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
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
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build()
        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException("Ollama generate failed: ${response.body()}")
        }
        return json.decodeFromString<GenerateResponse>(response.body()).response
    }
}

@Serializable
private data class GenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
)

@Serializable
private data class GenerateResponse(
    val response: String,
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
