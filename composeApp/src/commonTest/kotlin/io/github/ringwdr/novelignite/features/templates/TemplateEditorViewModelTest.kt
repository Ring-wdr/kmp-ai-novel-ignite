package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TemplateEditorViewModelTest {
    @Test
    fun addPromptBlock_appendsFreeformRule() {
        val viewModel = TemplateEditorViewModel()

        val added = viewModel.addPromptBlock("Keep sensory detail high")

        assertEquals(true, added)
        assertEquals(listOf("Keep sensory detail high"), viewModel.state.value.promptBlocks)
    }

    @Test
    fun addPromptBlock_returnsFalseForBlankInput_andKeepsStateUntouched() {
        val viewModel = TemplateEditorViewModel()

        viewModel.updateTitle("Noir Seoul")
        val added = viewModel.addPromptBlock("   ")

        assertEquals(false, added)
        assertEquals("Noir Seoul", viewModel.state.value.title)
        assertEquals(emptyList(), viewModel.state.value.promptBlocks)
    }

    @Test
    fun updateAndRemovePromptBlock_editsExistingBlocks() {
        val viewModel = TemplateEditorViewModel()

        viewModel.addPromptBlock("Keep sensory detail high")
        viewModel.addPromptBlock("Keep dialogue sharp")
        viewModel.updatePromptBlock(0, "Keep atmosphere heavy")
        viewModel.removePromptBlock(1)

        assertEquals(listOf("Keep atmosphere heavy"), viewModel.state.value.promptBlocks)
    }

    @Test
    fun loadTemplate_prefillsEditorStateFromExistingTemplate() {
        val viewModel = TemplateEditorViewModel()
        val template = Template(
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

        viewModel.loadTemplate(template)

        assertEquals(
            TemplateEditorState(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun saveTemplate_buildsStructuredDraftAndClearsEditor() = runTest {
        val viewModel = TemplateEditorViewModel()
        var capturedDraft: TemplateDraft? = null
        var saveCount = 0

        viewModel.updateTitle("Noir Seoul")
        viewModel.updateGenre("Urban Fantasy")
        viewModel.updatePremise("A ghost broker solves debts")
        viewModel.addPromptBlock("Keep sensory detail high")

        viewModel.saveTemplate(
            onSave = { draft ->
                saveCount += 1
                capturedDraft = draft
                Template(
                    id = 42,
                    title = draft.title,
                    genre = draft.genre,
                    premise = draft.premise,
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = draft.promptBlocks,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 2,
                )
            },
        )

        assertEquals(1, saveCount)
        assertEquals(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            ),
            capturedDraft,
        )
        assertEquals(TemplateEditorState(), viewModel.state.value)
    }

    @Test
    fun saveTemplate_withoutReset_keepsEditorSyncedToSavedTemplate() = runTest {
        val viewModel = TemplateEditorViewModel()

        viewModel.updateTitle("Noir Seoul Revised")
        viewModel.updateGenre("Urban Fantasy")
        viewModel.updatePremise("A ghost broker negotiates a deeper debt")
        viewModel.addPromptBlock("Keep sensory detail high")

        val saved = viewModel.saveTemplate(
            onSave = { draft ->
                Template(
                    id = 42,
                    title = draft.title,
                    genre = draft.genre,
                    premise = draft.premise,
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = draft.promptBlocks,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 3,
                )
            },
            resetAfterSave = false,
        )

        assertEquals(42, saved.id)
        assertEquals(
            TemplateEditorState(
                title = "Noir Seoul Revised",
                genre = "Urban Fantasy",
                premise = "A ghost broker negotiates a deeper debt",
                promptBlocks = listOf("Keep sensory detail high"),
            ),
            viewModel.state.value,
        )
    }

    @Test
    fun saveTemplate_trimsAndDropsBlankPromptBlocks() = runTest {
        val viewModel = TemplateEditorViewModel()
        var capturedDraft: TemplateDraft? = null

        viewModel.updateTitle("Noir Seoul")
        viewModel.updateGenre("Urban Fantasy")
        viewModel.updatePremise("A ghost broker solves debts")
        viewModel.addPromptBlock("  Keep sensory detail high  ")
        viewModel.addPromptBlock("Keep dialogue sharp")
        viewModel.updatePromptBlock(1, "   ")

        viewModel.saveTemplate(
            onSave = { draft ->
                capturedDraft = draft
                Template(
                    id = 42,
                    title = draft.title,
                    genre = draft.genre,
                    premise = draft.premise,
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = draft.promptBlocks,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 3,
                )
            },
        )

        assertEquals(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            ),
            capturedDraft,
        )
    }

    @Test
    fun hasUnsavedChanges_isFalseImmediatelyAfterLoadDraft_andTrueAfterEdit() {
        val viewModel = TemplateEditorViewModel()

        viewModel.loadDraft(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            )
        )

        assertEquals(false, viewModel.hasUnsavedChanges())

        viewModel.updatePremise("A broker chases a deeper debt")

        assertEquals(true, viewModel.hasUnsavedChanges())
    }

    @Test
    fun saveTemplate_withoutReset_refreshesDirtyBaselineToSavedDraft() = runTest {
        val viewModel = TemplateEditorViewModel()

        viewModel.loadDraft(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            )
        )
        viewModel.updateTitle("Noir Seoul Revised")

        assertEquals(true, viewModel.hasUnsavedChanges())

        viewModel.saveTemplate(
            onSave = { draft ->
                Template(
                    id = 42,
                    title = draft.title,
                    genre = draft.genre,
                    premise = draft.premise,
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = draft.promptBlocks,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 2,
                )
            },
            resetAfterSave = false,
        )

        assertEquals(false, viewModel.hasUnsavedChanges())
    }
}
