package io.github.ringwdr.novelignite.features.board

import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.templates.TemplateDraft

data class BoardSnapshotCard(
    val id: Long,
    val templateId: Long,
    val versionNumber: Long,
    val title: String,
    val genre: String,
    val premise: String,
    val promptBlocks: List<String>,
    val createdAtEpochMs: Long,
    val sourceVersion: TemplateVersion,
)

fun BoardSnapshotCard.toTemplateDraft(): TemplateDraft = TemplateDraft(
    title = "$title Remix",
    genre = genre,
    premise = premise,
    promptBlocks = promptBlocks,
)

internal fun TemplateVersion.toBoardSnapshotCard(): BoardSnapshotCard = BoardSnapshotCard(
    id = id,
    templateId = templateId,
    versionNumber = versionNumber,
    title = title,
    genre = genre,
    premise = premise,
    promptBlocks = promptBlocks,
    createdAtEpochMs = createdAtEpochMs,
    sourceVersion = this,
)
