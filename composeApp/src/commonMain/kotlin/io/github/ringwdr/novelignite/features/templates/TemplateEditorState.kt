package io.github.ringwdr.novelignite.features.templates

data class TemplateEditorState(
    val title: String = "",
    val genre: String = "",
    val premise: String = "",
    val promptBlocks: List<String> = emptyList(),
)
