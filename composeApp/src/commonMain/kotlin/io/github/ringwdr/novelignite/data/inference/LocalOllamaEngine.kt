package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import java.lang.StringBuilder
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class LocalOllamaEngine(
    private val modelsClient: OllamaModelsClient,
    private val modelName: String,
) : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        val prompt = toPrompt(
            actionType = request.actionType,
            userPrompt = request.userPrompt,
            manuscriptExcerpt = request.manuscriptExcerpt,
            promptBlocks = request.promptBlocks,
        )
        try {
            val fullText = StringBuilder()
            modelsClient.streamGenerate(model = modelName, prompt = prompt).collect { chunk ->
                fullText.append(chunk)
                emit(GenerationEvent.Token(chunk))
            }
            emit(GenerationEvent.Final(fullText.toString()))
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (error: Throwable) {
            emit(GenerationEvent.Error(error.message ?: "Ollama generation failed."))
        }
    }

    companion object {
        fun toPrompt(
            actionType: String,
            userPrompt: String,
            manuscriptExcerpt: String,
            promptBlocks: List<String>,
        ): String = buildString {
            appendLine("Action:")
            appendLine(actionType)
            appendLine()
            if (userPrompt.isNotBlank()) {
                appendLine("User Prompt:")
                appendLine(userPrompt)
                appendLine()
            }
            appendLine("Story Partner Instructions:")
            if (promptBlocks.isEmpty()) {
                appendLine("- Stay in-scene and continue naturally.")
                appendLine("- Offer 2-3 complete Korean next-turn suggestions.")
            } else {
                promptBlocks.forEach { appendLine(it) }
            }
            appendLine()
            appendLine("Excerpt:")
            append(manuscriptExcerpt)
        }
    }
}
