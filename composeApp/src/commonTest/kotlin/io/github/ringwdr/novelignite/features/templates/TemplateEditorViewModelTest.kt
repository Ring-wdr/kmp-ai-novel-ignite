package io.github.ringwdr.novelignite.features.templates

import kotlin.test.Test
import kotlin.test.assertEquals

class TemplateEditorViewModelTest {
    @Test
    fun addPromptBlock_appendsFreeformRule() {
        val viewModel = TemplateEditorViewModel()

        viewModel.addPromptBlock("Keep sensory detail high")

        assertEquals(listOf("Keep sensory detail high"), viewModel.state.value.promptBlocks)
    }
}
