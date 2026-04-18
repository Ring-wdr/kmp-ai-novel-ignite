package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.data.local.openDesktopDatabase
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion

actual fun loadLocalTemplates(): List<Template> = TemplateRepositoryImpl(
    database = openDesktopDatabase(),
).listTemplates()

actual fun loadLocalTemplateVersions(templateId: Long): List<TemplateVersion> =
    TemplateRepositoryImpl(database = openDesktopDatabase()).listTemplateVersions(templateId)

actual suspend fun saveLocalTemplate(
    draft: TemplateDraft,
    templateId: Long?,
    originalTemplate: Template?,
): Template = TemplateRepositoryImpl(
    database = openDesktopDatabase(),
).let { repository ->
    val preservedTemplate = when {
        originalTemplate != null -> originalTemplate
        templateId != null -> repository.listTemplates().firstOrNull { it.id == templateId }
        else -> null
    }

    repository.saveTemplate(
        title = draft.title,
        genre = draft.genre,
        premise = draft.premise,
        worldSetting = preservedTemplate?.worldSetting.orEmpty(),
        characterCards = preservedTemplate?.characterCards.orEmpty(),
        relationshipNotes = preservedTemplate?.relationshipNotes.orEmpty(),
        toneStyle = preservedTemplate?.toneStyle.orEmpty(),
        bannedElements = preservedTemplate?.bannedElements.orEmpty(),
        plotConstraints = preservedTemplate?.plotConstraints.orEmpty(),
        openingHook = preservedTemplate?.openingHook.orEmpty(),
        promptBlocks = draft.promptBlocks,
        templateId = if (preservedTemplate != null || templateId == null) templateId else null,
    )
}
