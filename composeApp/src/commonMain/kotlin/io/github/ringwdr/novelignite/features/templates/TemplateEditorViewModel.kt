package io.github.ringwdr.novelignite.features.templates

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class TemplateDraft(
    val title: String,
    val genre: String,
    val premise: String,
    val promptBlocks: List<String>,
)

class TemplateEditorViewModel {
    val state = MutableStateFlow(TemplateEditorState())

    fun updateTitle(value: String) {
        state.update { it.copy(title = value) }
    }

    fun updateGenre(value: String) {
        state.update { it.copy(genre = value) }
    }

    fun updatePremise(value: String) {
        state.update { it.copy(premise = value) }
    }

    fun addPromptBlock(value: String) {
        val sanitized = value.trim()
        if (sanitized.isBlank()) return
        state.update { it.copy(promptBlocks = it.promptBlocks + sanitized) }
    }

    suspend fun saveTemplate(onSave: suspend (TemplateDraft) -> Unit) {
        val snapshot = state.value
        onSave(
            TemplateDraft(
                title = snapshot.title.trim(),
                genre = snapshot.genre.trim(),
                premise = snapshot.premise.trim(),
                promptBlocks = snapshot.promptBlocks,
            )
        )
        state.value = TemplateEditorState()
    }
}
