package io.github.ringwdr.novelignite.domain.model

data class Template(
    val id: Long,
    val title: String,
    val genre: String,
    val premise: String,
    val worldSetting: String,
    val characterCards: String,
    val relationshipNotes: String,
    val toneStyle: String,
    val bannedElements: String,
    val plotConstraints: String,
    val openingHook: String,
    val promptBlocks: List<String>,
    val createdAtEpochMs: Long,
    val updatedAtEpochMs: Long,
)
