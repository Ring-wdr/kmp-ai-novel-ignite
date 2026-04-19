package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class TemplateRemixSelection(
    val draft: TemplateDraft,
    val sourceLabel: String,
    val sourceVersion: TemplateVersion? = null,
)

object TemplateRemixStore {
    private val _selection = MutableStateFlow<TemplateRemixSelection?>(null)
    val selection: StateFlow<TemplateRemixSelection?> = _selection

    fun select(selection: TemplateRemixSelection) {
        _selection.value = selection
    }

    fun clear() {
        _selection.value = null
    }
}
