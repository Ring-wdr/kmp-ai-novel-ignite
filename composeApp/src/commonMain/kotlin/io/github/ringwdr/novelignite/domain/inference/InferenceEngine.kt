package io.github.ringwdr.novelignite.domain.inference

import kotlinx.coroutines.flow.Flow

interface InferenceEngine {
    fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent>
}
