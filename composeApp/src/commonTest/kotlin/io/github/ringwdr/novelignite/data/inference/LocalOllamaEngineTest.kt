package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class LocalOllamaEngineTest {
    @Test
    fun streamGenerate_emitsTokensThenFinalText() = runTest {
        var observedModel: String? = null
        var observedPrompt: String? = null
        val modelsClient = object : OllamaModelsClient {
            override suspend fun listModels(): List<String> = emptyList()

            override suspend fun generate(model: String, prompt: String): String {
                observedModel = model
                observedPrompt = prompt
                return "The gate opened. A silver wind answered."
            }

            override fun streamGenerate(model: String, prompt: String): Flow<String> {
                observedModel = model
                observedPrompt = prompt
                return flowOf("The gate opened.", " A silver wind answered.")
            }
        }

        val events = LocalOllamaEngine(
            modelsClient = modelsClient,
            modelName = "llama3",
        ).streamGenerate(
            GenerationRequest(
                projectId = "project-1",
                templateId = "template-1",
                actionType = "continue",
                userPrompt = "Open the vault",
                manuscriptExcerpt = "The gate opened.",
                promptBlocks = listOf("Keep it lyrical", "Stay in third person"),
            )
        ).toList()

        assertEquals("llama3", observedModel)
        assertEquals(
            "Action:\ncontinue\n\nUser Prompt:\nOpen the vault\n\nStory Partner Instructions:\nKeep it lyrical\nStay in third person\n\nExcerpt:\nThe gate opened.",
            observedPrompt,
        )
        assertEquals(
            listOf(
                GenerationEvent.Token("The gate opened."),
                GenerationEvent.Token(" A silver wind answered."),
                GenerationEvent.Final("The gate opened. A silver wind answered."),
            ),
            events,
        )
    }
}
