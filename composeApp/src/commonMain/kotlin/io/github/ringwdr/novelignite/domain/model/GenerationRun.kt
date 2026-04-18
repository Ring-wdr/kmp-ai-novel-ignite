package io.github.ringwdr.novelignite.domain.model

data class GenerationRun(
    val id: Long,
    val projectId: Long?,
    val templateId: Long?,
    val templateVersionId: Long?,
    val actionType: String,
    val inputText: String,
    val outputText: String,
    val createdAtEpochMs: Long,
)
