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
        assertEquals("Noir Seoul", templates.first().title)
        assertEquals(1, templates.first().promptBlocks.size)
    }
}
