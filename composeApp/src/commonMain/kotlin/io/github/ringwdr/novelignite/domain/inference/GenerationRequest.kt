package io.github.ringwdr.novelignite.domain.inference

data class GenerationRequest(
    val projectId: String,
    val templateId: String,
    val actionType: String,
    val manuscriptExcerpt: String,
    val promptBlocks: List<String>,
)
