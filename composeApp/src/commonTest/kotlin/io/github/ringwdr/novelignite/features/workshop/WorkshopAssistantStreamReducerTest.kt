package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkshopAssistantStreamReducerTest {
    @Test
    fun startThenMarkdownDelta_createsStreamingAssistantTurn() {
        val started = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = started,
            event = WorkshopAssistantStreamEvent.MarkdownDelta(
                messageId = "generation-1-assistant",
                markdown = "## Gate\n\nThe gate opened.",
            ),
        )

        val assistant = updated.messages.single { it.role == WorkshopMessageRole.Assistant }
        assertEquals("## Gate\n\nThe gate opened.", assistant.assistant?.renderedMarkdown)
        assertEquals(WorkshopAssistantPhase.Streaming, assistant.assistant?.phase)
        assertTrue(assistant.isStreaming)
        assertEquals(WorkshopStreamingStatus.Streaming, updated.streamingStatus)
    }

    @Test
    fun choicesReplace_overwritesPreviousChoiceSurface() {
        val base = WorkshopUiState(
            messages = listOf(
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    assistant = WorkshopAssistantTurn(
                        renderedMarkdown = "Scene",
                        choices = listOf(
                            WorkshopChoice(
                                id = "c-1",
                                label = "Old choice",
                                prompt = "Old prompt",
                                style = WorkshopChoiceStyle.Secondary,
                            ),
                        ),
                        phase = WorkshopAssistantPhase.Streaming,
                    ),
                    isStreaming = true,
                ),
            ),
            streamingStatus = WorkshopStreamingStatus.Streaming,
        )

        val replaced = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.ChoicesReplace(
                messageId = "generation-1-assistant",
                choices = listOf(
                    WorkshopChoice(
                        id = "c-2",
                        label = "Push forward",
                        prompt = "Continue scene",
                        style = WorkshopChoiceStyle.Primary,
                    ),
                ),
            ),
        )

        assertEquals(
            listOf("Push forward"),
            replaced.messages.single().assistant?.choices?.map { it.label },
        )
    }

    @Test
    fun complete_freezesAssistantTurnAndClearsStreamingFlag() {
        val base = WorkshopUiState(
            messages = listOf(
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    assistant = WorkshopAssistantTurn(
                        renderedMarkdown = "Draft",
                        phase = WorkshopAssistantPhase.Streaming,
                    ),
                    isStreaming = true,
                ),
            ),
            streamingStatus = WorkshopStreamingStatus.Streaming,
        )

        val completed = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.Complete(messageId = "generation-1-assistant"),
        )

        val assistant = completed.messages.single().assistant!!
        assertEquals(WorkshopAssistantPhase.Completed, assistant.phase)
        assertFalse(completed.messages.single().isStreaming)
        assertEquals("Draft", assistant.renderedMarkdown)
        assertEquals(WorkshopStreamingStatus.Idle, completed.streamingStatus)
    }
}
