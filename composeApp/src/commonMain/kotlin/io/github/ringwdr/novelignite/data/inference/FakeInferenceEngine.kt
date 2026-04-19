package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// Test fixture kept in commonMain to satisfy the Task 6 scaffold contract.
internal class FakeInferenceEngine : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        val opening = "The gate opened."
        val ending = " A silver wind answered."
        emit(GenerationEvent.Token(opening))
        emit(GenerationEvent.Token(ending))
        emit(GenerationEvent.Final(opening + ending))
    }
}
