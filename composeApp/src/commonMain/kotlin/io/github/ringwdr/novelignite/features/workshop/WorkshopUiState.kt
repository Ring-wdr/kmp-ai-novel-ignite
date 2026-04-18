package io.github.ringwdr.novelignite.features.workshop

data class WorkshopUiState(
    val draftText: String = "",
    val generatedText: String = "",
    val isGenerating: Boolean = false,
)
