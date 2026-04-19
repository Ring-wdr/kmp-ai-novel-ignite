package io.github.ringwdr.novelignite.domain.usecase

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest

class GenerateDraftUseCaseTest {
    @Test
    fun invoke_emitsStreamingTokensAndFinalText() = runTest {
        val useCase = GenerateDraftUseCase(FakeInferenceEngine())
        val request = GenerationRequest(
            projectId = "project-1",
            templateId = "template-1",
            actionType = "continue",
            userPrompt = "",
            manuscriptExcerpt = "The gate opened.",
            promptBlocks = listOf("Keep it lyrical")
        )

        val events = useCase(request).toList()

        assertEquals("The gate opened. A silver wind answered.", events.last().text)
    }
}
