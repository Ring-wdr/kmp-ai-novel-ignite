package io.github.ringwdr.novelignite.domain.usecase

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform

class GenerateDraftUseCase(
    private val inferenceEngine: InferenceEngine,
) {
    operator fun invoke(request: GenerationRequest): Flow<GenerationEvent.Final> =
        inferenceEngine.streamGenerate(request).transform { event ->
            when (event) {
                is GenerationEvent.Final -> emit(event)
                is GenerationEvent.Error -> throw IllegalStateException(event.message)
                is GenerationEvent.Token -> Unit
            }
        }
}
