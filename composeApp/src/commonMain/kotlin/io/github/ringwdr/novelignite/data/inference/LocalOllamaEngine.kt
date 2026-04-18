package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalOllamaEngine(
    private val modelsClient: OllamaModelsClient,
) : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Final(toPrompt(request.actionType, request.manuscriptExcerpt, request.promptBlocks)))
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
