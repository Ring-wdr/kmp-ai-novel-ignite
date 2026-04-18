package io.github.ringwdr.novelignite.features.templates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class TemplateEditorViewModel {
    val state = MutableStateFlow(TemplateEditorState())

    fun addPromptBlock(value: String) {
        state.update { it.copy(promptBlocks = it.promptBlocks + value) }
    }
}
