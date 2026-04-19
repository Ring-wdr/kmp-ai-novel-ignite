package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
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

    fun loadTemplate(template: Template) {
        loadDraft(
            TemplateDraft(
                title = template.title,
                genre = template.genre,
                premise = template.premise,
                promptBlocks = template.promptBlocks,
            )
        )
    }

    fun loadDraft(draft: TemplateDraft) {
        state.value = TemplateEditorState(
            title = draft.title,
            genre = draft.genre,
            premise = draft.premise,
            promptBlocks = draft.promptBlocks,
        )
    }

    fun updateTitle(value: String) {
        state.update { it.copy(title = value) }
    }

    fun updateGenre(value: String) {
        state.update { it.copy(genre = value) }
    }

    fun updatePremise(value: String) {
        state.update { it.copy(premise = value) }
    }

    fun addPromptBlock(value: String): Boolean {
        val sanitized = value.trim()
        if (sanitized.isBlank()) return false
        state.update { it.copy(promptBlocks = it.promptBlocks + sanitized) }
        return true
    }

    fun updatePromptBlock(index: Int, value: String) {
        state.update { current ->
            if (index !in current.promptBlocks.indices) return@update current
            val updatedBlocks = current.promptBlocks.toMutableList()
            updatedBlocks[index] = value
            current.copy(promptBlocks = updatedBlocks)
        }
    }

    fun removePromptBlock(index: Int) {
        state.update { current ->
            if (index !in current.promptBlocks.indices) return@update current
            current.copy(
                promptBlocks = current.promptBlocks.filterIndexed { currentIndex, _ ->
                    currentIndex != index
                }
            )
        }
    }

    suspend fun saveTemplate(
        onSave: suspend (TemplateDraft) -> Template,
        resetAfterSave: Boolean = true,
    ): Template {
        val snapshot = state.value
        val normalizedPromptBlocks = snapshot.promptBlocks
            .map { it.trim() }
            .filter { it.isNotBlank() }
        val savedTemplate = onSave(
            TemplateDraft(
                title = snapshot.title.trim(),
                genre = snapshot.genre.trim(),
                premise = snapshot.premise.trim(),
                promptBlocks = normalizedPromptBlocks,
            )
        )
        if (resetAfterSave) {
            state.value = TemplateEditorState()
        } else {
            loadTemplate(savedTemplate)
        }
        return savedTemplate
    }
}
