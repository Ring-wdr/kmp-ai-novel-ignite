package io.github.ringwdr.novelignite.domain.model

data class TemplateVersion(
    val id: Long,
    val templateId: Long,
    val versionNumber: Long,
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
)
