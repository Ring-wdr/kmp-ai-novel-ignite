package io.github.ringwdr.novelignite.features.templates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class TemplateEditorViewModel {
    private val _state = MutableStateFlow(TemplateEditorState())
    val state: StateFlow<TemplateEditorState> = _state

    fun addPromptBlock(value: String) {
        _state.update { it.copy(promptBlocks = it.promptBlocks + value) }
    }
}
