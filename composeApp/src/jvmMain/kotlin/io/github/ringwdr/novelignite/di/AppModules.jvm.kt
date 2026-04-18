package io.github.ringwdr.novelignite.di

import io.github.ringwdr.novelignite.data.inference.DesktopOllamaModelsClient
import io.github.ringwdr.novelignite.data.inference.LocalOllamaEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine

actual fun createPlatformInferenceEngine(): InferenceEngine {
    val config = DesktopOllamaConfig.fromEnvironment()
    return LocalOllamaEngine(
        modelsClient = DesktopOllamaModelsClient(config.baseUrl),
        modelName = config.modelName,
    )
}

private data class DesktopOllamaConfig(
    val baseUrl: String,
    val modelName: String,
) {
    companion object {
        private const val DefaultBaseUrl = "http://127.0.0.1:11434"
        private const val DefaultModel = "hf.co/TrevorJS/gemma-4-E4B-it-uncensored-GGUF:Q8_0"

        fun fromEnvironment(): DesktopOllamaConfig = DesktopOllamaConfig(
            baseUrl = System.getenv("OLLAMA_BASE_URL")?.trim().orEmpty().ifBlank { DefaultBaseUrl },
            modelName = System.getenv("OLLAMA_MODEL")?.trim().orEmpty().ifBlank { DefaultModel },
        )
    }
}
