package io.github.ringwdr.novelignite.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TemplateRepositoryImplTest {
    @Test
    fun saveTemplate_persistsStructuredAndFreeformFields() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)

        repository.saveTemplate(
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
            promptBlocks = listOf("Keep sensory detail high")
        )

        val templates = repository.listTemplates()
        val template = templates.first()

        assertEquals("Noir Seoul", template.title)
        assertEquals("Urban Fantasy", template.genre)
        assertEquals("A ghost broker solves debts", template.premise)
        assertEquals("Night markets and hidden contracts", template.worldSetting)
        assertEquals("Jin, Hyeon, Broker", template.characterCards)
        assertEquals("Debt binds broker and ghost", template.relationshipNotes)
        assertEquals("Moody and elegant", template.toneStyle)
        assertEquals("No slapstick", template.bannedElements)
        assertEquals("Reveal one secret per scene", template.plotConstraints)
        assertEquals("Rain on neon stone", template.openingHook)
        assertEquals(listOf("Keep sensory detail high"), template.promptBlocks)
    }
}
