package io.github.ringwdr.novelignite.domain.usecase

import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class GenerateDraftUseCaseTest {
    @Test
    fun invoke_emitsFinalText() = runTest {
        val useCase = GenerateDraftUseCase(FakeInferenceEngine())
        val request = GenerationRequest(
            projectId = "project-1",
            templateId = "template-1",
            actionType = "continue",
            manuscriptExcerpt = "The gate opened.",
            promptBlocks = listOf("Keep it lyrical")
        )

        val events: List<GenerationEvent> = useCase(request).toList()

        assertEquals(
            "The gate opened. A silver wind answered.",
            (events.last() as GenerationEvent.Final).text
        )
    }

    @Test
    fun invoke_emitsProviderErrors() = runTest {
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

        val events: List<GenerationEvent> = useCase(request).toList()

        assertEquals(
            listOf(GenerationEvent.Error("provider unavailable")),
            events
        )
    }
}
