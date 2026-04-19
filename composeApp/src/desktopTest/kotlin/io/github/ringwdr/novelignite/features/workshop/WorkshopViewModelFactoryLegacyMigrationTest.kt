package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkshopViewModelFactoryLegacyMigrationTest {
    @Test
    fun legacyWorkshopStateJson_restoresDraftAndGeneratedTurn() {
        val legacyJson = """{"draftText":"Draft from disk","generatedText":"Recovered text"}"""

        val restored = invokeParseStoredState(legacyJson)

        assertEquals("Draft from disk", restored.draftText)
        assertEquals(
            listOf(
                WorkshopChatMessage.assistant("legacy-generated", "Recovered text"),
            ),
            restored.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, restored.streamingStatus)
        assertNull(restored.errorMessage)
    }

    @Test
    fun snapshotJson_restoresCompletedTypedAssistantTurnAndIgnoresUnknownFields() {
        val snapshotJson = """
            {
              "draftText": "Draft from disk",
              "messages": [
                {
                  "id": "u-1",
                  "role": "User",
                  "text": "Hello",
                  "ignoredMessageField": "ignored"
                },
                {
                  "id": "a-1",
                  "role": "Assistant",
                  "text": "Recovered text",
                  "assistant": {
                    "renderedMarkdown": "Recovered text",
                    "choices": [
                      {
                        "id": "choice-1",
                        "label": "Keep going",
                        "prompt": "Keep going",
                        "style": "Primary"
                      }
                    ],
                    "metadata": {
                      "title": "Recovered summary",
                      "badge": "Completed"
                    },
                    "phase": "Completed",
                    "failureMessage": null
                  }
                }
              ],
              "ignoredSnapshotField": true
            }
        """.trimIndent()

        val restored = invokeParseStoredState(snapshotJson)

        assertEquals("Draft from disk", restored.draftText)
        assertEquals(
            listOf(
                WorkshopChatMessage.user("u-1", "Hello"),
                WorkshopChatMessage.assistant(
                    id = "a-1",
                    assistant = WorkshopAssistantTurn(
                        renderedMarkdown = "Recovered text",
                        choices = listOf(
                            WorkshopChoice(
                                id = "choice-1",
                                label = "Keep going",
                                prompt = "Keep going",
                                style = WorkshopChoiceStyle.Primary,
                            ),
                        ),
                        metadata = WorkshopAssistantMetadata(
                            title = "Recovered summary",
                            badge = "Completed",
                        ),
                        phase = WorkshopAssistantPhase.Completed,
                    ),
                ),
            ),
            restored.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, restored.streamingStatus)
        assertNull(restored.errorMessage)
    }

    private fun invokeParseStoredState(content: String): WorkshopUiState {
        val factoryClass = Class.forName(
            "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelFactory_jvmKt"
        )
        val method = factoryClass.getDeclaredMethod("parseStoredState", String::class.java)
        method.isAccessible = true
        return method.invoke(null, content) as WorkshopUiState
    }
}
