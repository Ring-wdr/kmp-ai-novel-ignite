package io.github.ringwdr.novelignite.features.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class TemplateEditorViewModelTest {
    @Test
    fun addPromptBlock_appendsFreeformRule() {
        val viewModel = TemplateEditorViewModel()

        viewModel.addPromptBlock("Keep sensory detail high")

        assertEquals(listOf("Keep sensory detail high"), viewModel.state.value.promptBlocks)
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

        viewModel.saveTemplate { draft ->
            saveCount += 1
            capturedDraft = draft
        }

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
}
