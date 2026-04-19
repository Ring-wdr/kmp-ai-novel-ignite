package io.github.ringwdr.novelignite.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
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

    @Test
    fun saveTemplate_withExistingId_updatesTheSameRow() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)

        val created = repository.saveTemplate(
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
        )

        val updated = repository.saveTemplate(
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A ghost broker negotiates a deeper debt",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
            templateId = created.id,
        )

        val templates = repository.listTemplates()

        assertEquals(created.id, updated.id)
        assertEquals(1, templates.size)
        assertEquals(created.id, templates.first().id)
        assertEquals("Noir Seoul Revised", templates.first().title)
        assertEquals("Night markets and hidden contracts", templates.first().worldSetting)
        assertEquals(
            listOf("Keep sensory detail high", "Keep dialogue sharp"),
            templates.first().promptBlocks,
        )
    }

    @Test
    fun saveTemplate_appendsVersionSnapshotsWithIncreasingNumbers() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)

        val created = repository.saveTemplate(
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
        )

        repository.saveTemplate(
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A ghost broker negotiates a deeper debt",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
            templateId = created.id,
        )

        val versions = database.templateVersionQueries
            .selectTemplateVersionsByTemplateId(created.id)
            .executeAsList()

        assertEquals(2, versions.size)
        assertEquals(listOf(2L, 1L), versions.map { it.version_number })
        assertEquals("Noir Seoul Revised", versions.first().title)
        assertEquals("Noir Seoul", versions.last().title)
    }

    @Test
    fun deleteTemplate_removesTemplateHistory_andClearsProjectReference() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)

        val created = repository.saveTemplate(
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
        )

        repository.saveTemplate(
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A ghost broker negotiates a deeper debt",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
            templateId = created.id,
        )

        database.projectQueries.insertProject(
            title = "Workshop: Noir Seoul",
            template_id = created.id,
            created_at_epoch_ms = 1L,
            updated_at_epoch_ms = 1L,
        )
        val projectId = database.projectQueries.lastInsertedRowId().executeAsOne()

        repository.deleteTemplate(created.id)

        assertEquals(emptyList(), repository.listTemplates())
        assertEquals(emptyList(), repository.listTemplateVersions(created.id))
        assertNull(database.projectQueries.selectProjectById(projectId).executeAsOne().template_id)
    }
}
