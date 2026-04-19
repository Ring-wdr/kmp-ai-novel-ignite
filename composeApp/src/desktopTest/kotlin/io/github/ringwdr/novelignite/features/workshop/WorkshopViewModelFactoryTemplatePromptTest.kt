package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.data.local.TestDatabaseFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class WorkshopViewModelFactoryTemplatePromptTest {
    @Test
    fun resolveWorkshopTemplatePromptConfig_returnsDefaultConfigWhenNoTemplateIsSelected() = runTest {
        val database = TestDatabaseFactory.create()

        val config = resolveWorkshopTemplatePromptConfig(
            database = database,
            activeTemplate = null,
        )

        assertEquals("workshop-default-template", config.templateId)
        assertEquals(emptyList(), config.promptBlocks)
    }

    @Test
    fun resolveWorkshopTemplatePromptConfig_usesSelectedTemplatePromptBlocks() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)
        val template = repository.saveTemplate(
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
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
        )

        val config = resolveWorkshopTemplatePromptConfig(
            database = database,
            activeTemplate = ActiveWorkshopTemplate(
                id = template.id,
                title = template.title,
            ),
        )

        assertEquals(template.id.toString(), config.templateId)
        assertEquals(listOf("Keep sensory detail high", "Keep dialogue sharp"), config.promptBlocks)
    }
}
