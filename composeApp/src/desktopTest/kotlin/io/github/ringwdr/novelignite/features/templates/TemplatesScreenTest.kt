package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
