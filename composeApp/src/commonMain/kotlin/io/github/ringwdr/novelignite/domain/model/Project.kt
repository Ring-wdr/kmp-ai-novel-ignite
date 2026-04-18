package io.github.ringwdr.novelignite.domain.model

data class Project(
    val id: Long,
    val title: String,
    val templateId: Long?,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
