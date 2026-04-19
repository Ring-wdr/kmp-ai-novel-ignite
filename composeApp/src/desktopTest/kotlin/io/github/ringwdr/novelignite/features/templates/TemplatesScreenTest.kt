package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateSelectionPersistence
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.Rule

class TemplatesScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @AfterTest
    fun tearDown() {
        TemplateRemixStore.clear()
        ActiveWorkshopTemplateStore.resetForTests()
    }

    @Test
    fun defaultsToList_andOpensCreateEditorFromHeaderButton() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Templates").fetchSemanticsNode()
        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()

        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).fetchSemanticsNode()
        rule.onNodeWithText("New Template").fetchSemanticsNode()
        assertFailsWith<AssertionError> {
            rule.onNodeWithText("Noir Seoul").fetchSemanticsNode()
        }
    }

    @Test
    fun clickingListCard_opensSharedEditorInEditMode() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { listOf(sampleVersion()) },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Noir Seoul").performClick()

        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).fetchSemanticsNode()
        rule.onNodeWithText("Edit Template").fetchSemanticsNode()
        rule.onNodeWithText("Version history").fetchSemanticsNode()
    }

    @Test
    fun useInWorkshop_updatesBannerWithoutLeavingList() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Use in Workshop").performClick()

        rule.onNodeWithText("Workshop active: Noir Seoul").fetchSemanticsNode()
        rule.onNodeWithText("Templates").fetchSemanticsNode()
    }

    @Test
    fun remixSelection_opensCreateEditor_withSourceBanner() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        TemplateRemixStore.select(
            TemplateRemixSelection(
                draft = TemplateDraft(
                    title = "Gothic Mystery",
                    genre = "Mystery",
                    premise = "A locked tower refuses to stay silent",
                    promptBlocks = listOf("Keep every reveal eerie"),
                ),
                sourceLabel = "Board: Gothic Mystery v3",
            )
        )

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { emptyList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Remix from Board: Gothic Mystery v3").fetchSemanticsNode()
        rule.onNodeWithText("New Template").fetchSemanticsNode()
    }

    @Test
    fun saveTemplate_returnsToList_withFeedbackAndHighlight() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        val templates = mutableListOf(sampleTemplate())

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { templates.toList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, templateId, _, _ ->
                    val saved = sampleTemplate(
                        id = templateId ?: 99L,
                        title = draft.title,
                        genre = draft.genre,
                        premise = draft.premise,
                        promptBlocks = draft.promptBlocks,
                    )
                    templates.removeAll { it.id == saved.id }
                    templates += saved
                    saved
                },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()
        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG).performTextInput("Moon Archive")
        rule.onNodeWithTag(TEMPLATE_GENRE_FIELD_TAG).performTextInput("Fantasy")
        rule.onNodeWithTag(TEMPLATE_PREMISE_FIELD_TAG).performTextInput("A moon archivist wakes an old debt.")
        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).performTextInput("Keep the tone haunted")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Save Template").performClick()

        rule.onNodeWithText("Template saved").fetchSemanticsNode()
        rule.onNodeWithText("Moon Archive").fetchSemanticsNode()
        rule.onNodeWithText("Recently saved").fetchSemanticsNode()
    }

    @Test
    fun deleteFlow_confirmsBeforeRemovingTemplate_andClearsWorkshopSelection() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        val templates = mutableListOf(sampleTemplate())
        ActiveWorkshopTemplateStore.select(
            io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate(
                id = 7L,
                title = "Noir Seoul",
            )
        )

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { templates.toList() },
                loadTemplateVersions = { listOf(sampleVersion()) },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = { templateId ->
                    templates.removeAll { it.id == templateId }
                },
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Noir Seoul").performClick()
        rule.onNodeWithTag(TEMPLATE_DELETE_BUTTON_TAG).performClick()
        rule.onNodeWithText("Delete template?").fetchSemanticsNode()
        rule.onNodeWithText("'Noir Seoul' will be removed. If it is active in Workshop, that selection will also be cleared.").fetchSemanticsNode()
        rule.onNodeWithText("Delete").performClick()

        rule.onNodeWithText("Template deleted").fetchSemanticsNode()
        assertFailsWith<AssertionError> {
            rule.onNodeWithText("Noir Seoul").fetchSemanticsNode()
        }
        assertFailsWith<AssertionError> {
            rule.onNodeWithText("Workshop active: Noir Seoul").fetchSemanticsNode()
        }
    }

    @Test
    fun backFromDirtyEditor_showsDiscardDialog_beforeReturningToList() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { emptyList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()
        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG).performTextInput("Moon Archive")
        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).performClick()

        rule.onNodeWithText("Discard changes?").fetchSemanticsNode()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("New Template").fetchSemanticsNode()
        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).performClick()
        rule.onNodeWithText("Discard changes").performClick()
        rule.onNodeWithText("Templates").fetchSemanticsNode()
    }
}

private object NoOpPersistence : ActiveWorkshopTemplateSelectionPersistence {
    override fun load() = null
    override fun save(selection: io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate?) = Unit
}

private fun sampleTemplate(
    id: Long = 7L,
    title: String = "Noir Seoul",
    genre: String = "Urban Fantasy",
    premise: String = "A ghost broker solves debts",
    promptBlocks: List<String> = listOf("Keep sensory detail high"),
): Template = Template(
    id = id,
    title = title,
    genre = genre,
    premise = premise,
    worldSetting = "",
    characterCards = "",
    relationshipNotes = "",
    toneStyle = "",
    bannedElements = "",
    plotConstraints = "",
    openingHook = "",
    promptBlocks = promptBlocks,
    createdAtEpochMs = 1L,
    updatedAtEpochMs = 2L,
)

private fun sampleVersion(): TemplateVersion = TemplateVersion(
    id = 10L,
    templateId = 7L,
    versionNumber = 1L,
    title = "Noir Seoul",
    genre = "Urban Fantasy",
    premise = "A ghost broker solves debts",
    worldSetting = "",
    characterCards = "",
    relationshipNotes = "",
    toneStyle = "",
    bannedElements = "",
    plotConstraints = "",
    openingHook = "",
    promptBlocks = listOf("Keep sensory detail high"),
    createdAtEpochMs = 1L,
)
