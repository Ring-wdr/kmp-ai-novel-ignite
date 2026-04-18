package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalOllamaEngine(
    private val modelsClient: OllamaModelsClient,
    private val modelName: String,
) : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        val prompt = toPrompt(
            actionType = request.actionType,
            manuscriptExcerpt = request.manuscriptExcerpt,
            promptBlocks = request.promptBlocks,
        )
        runCatching { modelsClient.generate(model = modelName, prompt = prompt) }
            .onSuccess { emit(GenerationEvent.Final(it)) }
            .onFailure { emit(GenerationEvent.Error(it.message ?: "Ollama generation failed.")) }
    }

    companion object {
        fun toPrompt(
            actionType: String,
            manuscriptExcerpt: String,
            promptBlocks: List<String>,
        ): String = buildString {
            appendLine("Action: $actionType")
            appendLine("Rules:")
            promptBlocks.forEach { appendLine("- $it") }
            appendLine("Excerpt:")
            append(manuscriptExcerpt)
        }
    }
}
