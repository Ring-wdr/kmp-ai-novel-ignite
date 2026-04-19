package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

class WorkshopStateSnapshotTest {
    @Test
    fun fromUiState_persistsOnlyDurableTurns() {
        val completedAssistant = WorkshopAssistantTurn(
            renderedMarkdown = "Done",
            choices = listOf(
                WorkshopChoice(
                    id = "choice-1",
                    label = "Keep going",
                    prompt = "Keep going",
                    style = WorkshopChoiceStyle.Primary,
                ),
            ),
            metadata = WorkshopAssistantMetadata(
                title = "Assistant summary",
                badge = "Completed",
            ),
            phase = WorkshopAssistantPhase.Completed,
        )
        val state = WorkshopUiState(
            draftText = "Keep this",
            chatInputText = "Transient input",
            messages = listOf(
                WorkshopChatMessage.user("u-1", "Hello"),
                WorkshopChatMessage.assistant("a-1", "Working", isStreaming = true),
                WorkshopChatMessage.assistant("a-2", assistant = completedAssistant),
            ),
            streamingStatus = WorkshopStreamingStatus.Streaming,
            errorMessage = "Temporary failure",
        )

        val snapshot = WorkshopStateSnapshot.from(state)

        assertEquals("Keep this", snapshot.draftText)
        assertEquals(
            listOf(
                WorkshopPersistedMessage(
                    id = "u-1",
                    role = WorkshopMessageRole.User,
                    text = "Hello",
                ),
                WorkshopPersistedMessage(
                    id = "a-2",
                    role = WorkshopMessageRole.Assistant,
                    text = "Done",
                    assistant = completedAssistant,
                ),
            ),
            snapshot.messages,
        )
        assertNull(snapshot.errorMessage)
    }

    @Test
    fun toUiState_restoresIdleStateWithoutRevivingStreamingAssistantTurn() {
        val completedAssistant = WorkshopAssistantTurn(
            renderedMarkdown = "Done",
            choices = listOf(
                WorkshopChoice(
                    id = "choice-1",
                    label = "Keep going",
                    prompt = "Keep going",
                ),
            ),
            metadata = WorkshopAssistantMetadata(
                title = "Assistant summary",
                badge = "Completed",
            ),
            phase = WorkshopAssistantPhase.Completed,
        )
        val snapshot = WorkshopStateSnapshot(
            draftText = "Saved draft",
            messages = listOf(
                WorkshopPersistedMessage(
                    id = "u-1",
                    role = WorkshopMessageRole.User,
                    text = "Hello",
                ),
                WorkshopPersistedMessage(
                    id = "a-2",
                    role = WorkshopMessageRole.Assistant,
                    text = "Done",
                    assistant = completedAssistant,
                ),
            ),
            errorMessage = "Should not persist",
        )

        val restored = snapshot.toUiState()

        assertEquals("Saved draft", restored.draftText)
        assertEquals("", restored.chatInputText)
        assertEquals(WorkshopStreamingStatus.Idle, restored.streamingStatus)
        assertFalse(restored.messages.any { it.isStreaming })
        assertEquals(
            listOf(
                WorkshopChatMessage.user("u-1", "Hello"),
                WorkshopChatMessage.assistant("a-2", assistant = completedAssistant),
            ),
            restored.messages,
        )
        assertNull(restored.errorMessage)
    }

    @Test
    fun toUiState_reassignsDuplicateMessageIdsToKeepTimelineKeysUnique() {
        val snapshot = WorkshopStateSnapshot(
            draftText = "Saved draft",
            messages = listOf(
                WorkshopPersistedMessage(
                    id = "generation-1-user",
                    role = WorkshopMessageRole.User,
                    text = "Hello",
                ),
                WorkshopPersistedMessage(
                    id = "generation-1-assistant",
                    role = WorkshopMessageRole.Assistant,
                    text = "Reply",
                ),
                WorkshopPersistedMessage(
                    id = "generation-1-user",
                    role = WorkshopMessageRole.User,
                    text = "Hello again",
                ),
                WorkshopPersistedMessage(
                    id = "generation-1-assistant",
                    role = WorkshopMessageRole.Assistant,
                    text = "Reply again",
                ),
            ),
        )

        val restored = snapshot.toUiState()

        assertEquals(
            listOf(
                "generation-1-user",
                "generation-1-assistant",
                "restored-message-3-user",
                "restored-message-4-assistant",
            ),
            restored.messages.map(WorkshopChatMessage::id),
        )
        assertEquals(4, restored.messages.map(WorkshopChatMessage::id).toSet().size)
    }

    @Test
    fun uiState_isGeneratingFollowsStreamingStatus() {
        val state = WorkshopUiState(
            streamingStatus = WorkshopStreamingStatus.Streaming,
        )

        assertEquals(true, state.isGenerating)
    }

    @Test
    fun uiState_hasNoLegacyGeneratedTextGetter() {
        val hasGeneratedTextGetter = WorkshopUiState::class.java.methods.any { it.name == "getGeneratedText" }

        assertFalse(hasGeneratedTextGetter)
    }
}
