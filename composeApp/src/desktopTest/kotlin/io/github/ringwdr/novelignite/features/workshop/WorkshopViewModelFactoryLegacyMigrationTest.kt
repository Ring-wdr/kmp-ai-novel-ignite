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

    private fun invokeParseStoredState(content: String): WorkshopUiState {
        val factoryClass = Class.forName(
            "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelFactory_jvmKt"
        )
        val method = factoryClass.getDeclaredMethod("parseStoredState", String::class.java)
        method.isAccessible = true
        return method.invoke(null, content) as WorkshopUiState
    }
}
