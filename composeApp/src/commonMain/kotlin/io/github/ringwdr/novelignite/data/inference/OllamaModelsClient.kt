package io.github.ringwdr.novelignite.data.inference

interface OllamaModelsClient {
    suspend fun listModels(): List<String>
}
