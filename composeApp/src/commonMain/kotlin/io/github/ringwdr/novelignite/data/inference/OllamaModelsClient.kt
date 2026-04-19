package io.github.ringwdr.novelignite.data.inference

import kotlinx.coroutines.flow.Flow

interface OllamaModelsClient {
    suspend fun listModels(): List<String>
    suspend fun generate(model: String, prompt: String): String
    fun streamGenerate(model: String, prompt: String): Flow<String>
}
