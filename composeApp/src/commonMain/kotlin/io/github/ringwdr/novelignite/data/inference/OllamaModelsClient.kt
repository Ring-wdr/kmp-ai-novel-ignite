package io.github.ringwdr.novelignite.data.inference

interface OllamaModelsClient {
    suspend fun listModels(): List<String>
    suspend fun generate(model: String, prompt: String): String
}
