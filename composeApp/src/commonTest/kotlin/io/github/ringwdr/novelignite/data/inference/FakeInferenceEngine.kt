package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class FakeInferenceEngine : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Token("The gate opened."))
        emit(GenerationEvent.Token(" A silver wind answered."))
        emit(GenerationEvent.Final("The gate opened. A silver wind answered."))
    }
}
