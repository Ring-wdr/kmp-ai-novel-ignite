package io.github.ringwdr.novelignite.domain.usecase

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class GenerateDraftUseCaseErrorTest {
    @Test
    fun invoke_throwsWhenProviderEmitsError() = runTest {
        val useCase = GenerateDraftUseCase(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flowOf(
                    GenerationEvent.Error("provider unavailable")
                )
            }
        )
        val request = GenerationRequest(
            projectId = "project-1",
            templateId = "template-1",
            actionType = "continue",
            manuscriptExcerpt = "The gate opened.",
            promptBlocks = listOf("Keep it lyrical")
        )

        val error = assertFailsWith<IllegalStateException> {
            useCase(request).toList()
        }

        assertEquals("provider unavailable", error.message)
    }
}
