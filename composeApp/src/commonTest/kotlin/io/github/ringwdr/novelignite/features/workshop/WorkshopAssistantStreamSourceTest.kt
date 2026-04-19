package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class WorkshopAssistantStreamSourceTest {
    @Test
    fun defaultSource_emitsStartMarkdownChoicesAndComplete() = runTest {
        val source = DefaultWorkshopAssistantStreamSource(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Token("## Gate"))
                    emit(GenerationEvent.Token("\n\nThe wind answered."))
                    emit(GenerationEvent.Final("## Gate\n\nThe wind answered."))
                }
            },
            choiceBuilder = WorkshopChoiceBuilder(),
        )

        val events = source.stream(
            request = GenerationRequest(
                projectId = "project",
                templateId = "template",
                actionType = "chat",
                userPrompt = "Continue scene",
                manuscriptExcerpt = "Excerpt",
                promptBlocks = emptyList(),
            ),
            generationId = 7,
        ).toList()

        assertEquals(5, events.size)

        val start = events[0] as WorkshopAssistantStreamEvent.Start
        assertEquals("request-7", start.requestId)
        assertEquals("generation-7-assistant", start.messageId)

        assertEquals(
            listOf("## Gate", "\n\nThe wind answered."),
            events.filterIsInstance<WorkshopAssistantStreamEvent.MarkdownDelta>().map { it.markdown },
        )

        val choices = events.filterIsInstance<WorkshopAssistantStreamEvent.ChoicesReplace>().single().choices
        assertEquals(3, choices.size)
        assertTrue(choices.first().prompt.contains("Gate"))
        assertTrue(events.last() is WorkshopAssistantStreamEvent.Complete)
    }

    @Test
    fun fixtureSource_emitsDeterministicEvents() = runTest {
        val source = FixtureWorkshopAssistantStreamSource()

        val events = source.stream(
            request = GenerationRequest(
                projectId = "project",
                templateId = "template",
                actionType = "continue",
                userPrompt = "Keep going",
                manuscriptExcerpt = "The gate opened.",
                promptBlocks = listOf("Keep it lyrical"),
            ),
            generationId = 3,
        ).toList()

        val start = events.first() as WorkshopAssistantStreamEvent.Start
        assertEquals("request-3", start.requestId)
        assertEquals("generation-3-assistant", start.messageId)
        assertTrue(events.any { it is WorkshopAssistantStreamEvent.MarkdownDelta && it.markdown.contains("Fixture turn") })
        assertTrue(events.any { it is WorkshopAssistantStreamEvent.MarkdownDelta && it.markdown.contains("Template") })

        val choices = events.filterIsInstance<WorkshopAssistantStreamEvent.ChoicesReplace>().single().choices
        assertEquals(3, choices.size)
        assertTrue(choices.first().prompt.contains("Fixture turn"))
        assertTrue(events.last() is WorkshopAssistantStreamEvent.Complete)
    }
}
