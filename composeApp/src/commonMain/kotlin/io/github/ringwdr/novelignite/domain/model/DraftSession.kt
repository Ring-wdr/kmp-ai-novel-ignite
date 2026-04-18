package io.github.ringwdr.novelignite.domain.model

data class DraftSession(
    val id: Long,
    val projectId: Long,
    val content: String,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
