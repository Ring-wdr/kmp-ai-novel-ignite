package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion

data class TemplateSaveSeed(
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
    val templateId: Long?,
)

fun resolveTemplateSaveSeed(
    draft: TemplateDraft,
    templateId: Long? = null,
    originalTemplate: Template? = null,
    originalVersion: TemplateVersion? = null,
): TemplateSaveSeed {
    val structuredSource = originalTemplate ?: originalVersion?.toTemplate()

    return TemplateSaveSeed(
        title = draft.title,
        genre = draft.genre,
        premise = draft.premise,
        worldSetting = structuredSource?.worldSetting.orEmpty(),
        characterCards = structuredSource?.characterCards.orEmpty(),
        relationshipNotes = structuredSource?.relationshipNotes.orEmpty(),
        toneStyle = structuredSource?.toneStyle.orEmpty(),
        bannedElements = structuredSource?.bannedElements.orEmpty(),
        plotConstraints = structuredSource?.plotConstraints.orEmpty(),
        openingHook = structuredSource?.openingHook.orEmpty(),
        promptBlocks = draft.promptBlocks,
        templateId = if (originalTemplate != null || templateId == null) templateId else null,
    )
}

private fun TemplateVersion.toTemplate(): Template = Template(
    id = templateId,
    title = title,
    genre = genre,
    premise = premise,
    worldSetting = worldSetting,
    characterCards = characterCards,
    relationshipNotes = relationshipNotes,
    toneStyle = toneStyle,
    bannedElements = bannedElements,
    plotConstraints = plotConstraints,
    openingHook = openingHook,
    promptBlocks = promptBlocks,
    createdAtEpochMs = createdAtEpochMs,
    updatedAtEpochMs = createdAtEpochMs,
)
