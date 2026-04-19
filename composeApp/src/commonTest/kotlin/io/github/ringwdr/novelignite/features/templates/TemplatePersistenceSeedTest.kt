package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import kotlin.test.Test
import kotlin.test.assertEquals

class TemplatePersistenceSeedTest {
    @Test
    fun resolveTemplateSaveSeed_preservesStructuredFieldsFromSourceVersion() {
        val draft = TemplateDraft(
            title = "Noir Seoul Revised Remix",
            genre = "Urban Fantasy",
            premise = "A broker bargains with a ghost market",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
        )
        val sourceVersion = TemplateVersion(
            id = 11,
            templateId = 1,
            versionNumber = 2,
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A broker bargains with a ghost market",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high"),
            createdAtEpochMs = 220,
        )

        val seed = resolveTemplateSaveSeed(
            draft = draft,
            originalVersion = sourceVersion,
        )

        assertEquals("Noir Seoul Revised Remix", seed.title)
        assertEquals("Night markets and hidden contracts", seed.worldSetting)
        assertEquals("Jin, Hyeon, Broker", seed.characterCards)
        assertEquals("Debt binds broker and ghost", seed.relationshipNotes)
        assertEquals("Moody and elegant", seed.toneStyle)
        assertEquals("No slapstick", seed.bannedElements)
        assertEquals("Reveal one secret per scene", seed.plotConstraints)
        assertEquals("Rain on neon stone", seed.openingHook)
        assertEquals(listOf("Keep sensory detail high", "Keep dialogue sharp"), seed.promptBlocks)
        assertEquals(null, seed.templateId)
    }

    @Test
    fun resolveTemplateSaveSeed_prefersOriginalTemplateWhenEditingExistingTemplate() {
        val draft = TemplateDraft(
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A broker bargains with a ghost market",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
        )
        val originalTemplate = Template(
            id = 42,
            title = "Noir Seoul",
            genre = "Urban Fantasy",
            premise = "A ghost broker solves debts",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high"),
            createdAtEpochMs = 1,
            updatedAtEpochMs = 2,
        )
        val sourceVersion = TemplateVersion(
            id = 11,
            templateId = 1,
            versionNumber = 2,
            title = "Different Source",
            genre = "Mystery",
            premise = "Different premise",
            worldSetting = "Wrong world",
            characterCards = "Wrong cards",
            relationshipNotes = "Wrong notes",
            toneStyle = "Wrong tone",
            bannedElements = "Wrong banned",
            plotConstraints = "Wrong constraints",
            openingHook = "Wrong hook",
            promptBlocks = listOf("Wrong block"),
            createdAtEpochMs = 220,
        )

        val seed = resolveTemplateSaveSeed(
            draft = draft,
            templateId = 42,
            originalTemplate = originalTemplate,
            originalVersion = sourceVersion,
        )

        assertEquals(42, seed.templateId)
        assertEquals("Night markets and hidden contracts", seed.worldSetting)
        assertEquals("Jin, Hyeon, Broker", seed.characterCards)
        assertEquals("Debt binds broker and ghost", seed.relationshipNotes)
        assertEquals("Moody and elegant", seed.toneStyle)
        assertEquals("No slapstick", seed.bannedElements)
        assertEquals("Reveal one secret per scene", seed.plotConstraints)
        assertEquals("Rain on neon stone", seed.openingHook)
    }
}
