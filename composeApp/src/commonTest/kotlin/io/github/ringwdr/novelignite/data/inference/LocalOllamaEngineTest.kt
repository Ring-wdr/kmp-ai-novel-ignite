package io.github.ringwdr.novelignite.data.inference

import kotlin.test.Test
import kotlin.test.assertEquals

class LocalOllamaEngineTest {
    @Test
    fun toPrompt_buildsPromptFromTemplateAndExcerpt() {
        val prompt = LocalOllamaEngine.toPrompt(
            actionType = "continue",
            manuscriptExcerpt = "The gate opened.",
            promptBlocks = listOf("Keep it lyrical", "Stay in third person")
        )

        assertEquals(
            "Action: continue\nRules:\n- Keep it lyrical\n- Stay in third person\nExcerpt:\nThe gate opened.",
            prompt
        )
    }
}
