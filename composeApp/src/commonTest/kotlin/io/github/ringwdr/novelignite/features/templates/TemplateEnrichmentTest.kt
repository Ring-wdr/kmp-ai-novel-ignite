package io.github.ringwdr.novelignite.features.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TemplateEnrichmentTest {
    @Test
    fun buildTemplateEnrichmentPrompt_requestsStructuredTemplateCompletion() {
        val prompt = buildTemplateEnrichmentPrompt(
            TemplateDraft(
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
            ),
        )

        assertTrue(prompt.contains("사용자가 이미 적은 의도는 유지하고 확장"))
        assertTrue(prompt.contains("title"))
        assertTrue(prompt.contains("promptBlocks"))
    }

    @Test
    fun parseTemplateEnrichmentResponse_mergesGeneratedValuesWithOriginalDraft() {
        val enriched = parseTemplateEnrichmentResponse(
            raw = """
                {
                  "title": "Noir Seoul",
                  "genre": "Urban Fantasy",
                  "premise": "A ghost broker solves debts",
                  "promptBlocks": ["Keep sensory detail high", "Keep dialogue sharp"]
                }
            """.trimIndent(),
            original = TemplateDraft(
                premise = "A ghost broker solves debts",
            ),
        )

        assertEquals("Noir Seoul", enriched.title)
        assertEquals("Urban Fantasy", enriched.genre)
        assertEquals(listOf("Keep sensory detail high", "Keep dialogue sharp"), enriched.promptBlocks)
    }

    @Test
    fun parseTemplateEnrichmentResponse_acceptsJsonWrappedInMarkdownFence() {
        val enriched = parseTemplateEnrichmentResponse(
            raw = """
                ```json
                {
                  "title": "Noir Seoul",
                  "promptBlocks": ["Keep sensory detail high"]
                }
                ```
            """.trimIndent(),
            original = TemplateDraft(
                premise = "A ghost broker solves debts",
            ),
        )

        assertEquals("Noir Seoul", enriched.title)
        assertEquals(listOf("Keep sensory detail high"), enriched.promptBlocks)
    }
}
