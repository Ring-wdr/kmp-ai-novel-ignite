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
    fun start_ignoresSecondActiveTurnWhileStreaming() {
        val base = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-2",
                messageId = "generation-2-assistant",
            ),
        )

        assertEquals(base, updated)
    }

    @Test
    fun markdownDelta_ignoresLegacyAssistantMessageWithoutTypedState() {
        val base = WorkshopUiState(
            messages = listOf(
                WorkshopChatMessage(
                    id = "generation-1-assistant",
                    role = WorkshopMessageRole.Assistant,
                    text = "Legacy",
                    assistant = null,
                    isStreaming = true,
                ),
            ),
            streamingStatus = WorkshopStreamingStatus.Streaming,
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.MarkdownDelta(
                messageId = "generation-1-assistant",
                markdown = " ignored",
            ),
        )

        assertEquals(base, updated)
    }

    @Test
    fun markdownDelta_ignoresCompletedAssistantTurn() {
        val base = WorkshopUiState(
            messages = listOf(
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    assistant = WorkshopAssistantTurn(
                        renderedMarkdown = "Draft",
                        phase = WorkshopAssistantPhase.Completed,
                    ),
                    isStreaming = false,
                ),
            ),
            streamingStatus = WorkshopStreamingStatus.Idle,
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.MarkdownDelta(
                messageId = "generation-1-assistant",
                markdown = " ignored",
            ),
        )

        assertEquals(base, updated)
    }

    @Test
    fun metadataPatch_updatesStreamingAssistantMetadata() {
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

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.MetadataPatch(
                messageId = "generation-1-assistant",
                title = "Scene pulse",
                badge = "Drafting",
            ),
        )

        assertEquals("Scene pulse", updated.messages.single().assistant?.metadata?.title)
        assertEquals("Drafting", updated.messages.single().assistant?.metadata?.badge)
        assertEquals(base.messages.single().assistant?.renderedMarkdown, updated.messages.single().assistant?.renderedMarkdown)
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

    @Test
    fun error_removesMatchingStreamingAssistantTurn() {
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

        val failed = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.Error(
                messageId = "generation-1-assistant",
                message = "boom",
            ),
        )

        assertTrue(failed.messages.isEmpty())
        assertEquals(WorkshopStreamingStatus.Idle, failed.streamingStatus)
        assertEquals("boom", failed.errorMessage)
    }

    @Test
    fun complete_ignoresWrongIdAndLeavesActiveStreamUntouched() {
        val base = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.Complete(messageId = "generation-2-assistant"),
        )

        assertEquals(base, updated)
    }

    @Test
    fun complete_ignoresDuplicateTerminalEventAfterCompletion() {
        val started = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )
        val completed = WorkshopAssistantStreamReducer.apply(
            state = started,
            event = WorkshopAssistantStreamEvent.Complete(messageId = "generation-1-assistant"),
        )

        val duplicate = WorkshopAssistantStreamReducer.apply(
            state = completed,
            event = WorkshopAssistantStreamEvent.Complete(messageId = "generation-1-assistant"),
        )

        assertEquals(completed, duplicate)
    }

    @Test
    fun abortAck_removesAssistantTurnAndReturnsToIdle() {
        val started = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val aborted = WorkshopAssistantStreamReducer.apply(
            state = started,
            event = WorkshopAssistantStreamEvent.AbortAck(messageId = "generation-1-assistant"),
        )

        assertTrue(aborted.messages.isEmpty())
        assertEquals(WorkshopStreamingStatus.Idle, aborted.streamingStatus)
        assertFalse(aborted.isGenerating)
    }

    @Test
    fun abortAck_ignoresWrongIdAndLeavesActiveStreamUntouched() {
        val base = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.AbortAck(messageId = "generation-2-assistant"),
        )

        assertEquals(base, updated)
    }

    @Test
    fun error_ignoresWrongIdAndLeavesActiveStreamUntouched() {
        val base = WorkshopAssistantStreamReducer.apply(
            state = WorkshopUiState(),
            event = WorkshopAssistantStreamEvent.Start(
                requestId = "request-1",
                messageId = "generation-1-assistant",
            ),
        )

        val updated = WorkshopAssistantStreamReducer.apply(
            state = base,
            event = WorkshopAssistantStreamEvent.Error(
                messageId = "generation-2-assistant",
                message = "boom",
            ),
        )

        assertEquals(base, updated)
    }
}
