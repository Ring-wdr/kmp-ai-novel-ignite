package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.data.inference.DesktopOllamaModelsClient
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
    originalVersion: TemplateVersion?,
): Template = TemplateRepositoryImpl(
    database = openDesktopDatabase(),
).let { repository ->
    val resolvedOriginalTemplate = when {
        originalTemplate != null -> originalTemplate
        templateId != null -> repository.listTemplates().firstOrNull { it.id == templateId }
        else -> null
    }
    val seed = resolveTemplateSaveSeed(
        draft = draft,
        templateId = templateId,
        originalTemplate = resolvedOriginalTemplate,
        originalVersion = originalVersion,
    )

    repository.saveTemplate(
        title = seed.title,
        genre = seed.genre,
        premise = seed.premise,
        worldSetting = seed.worldSetting,
        characterCards = seed.characterCards,
        relationshipNotes = seed.relationshipNotes,
        toneStyle = seed.toneStyle,
        bannedElements = seed.bannedElements,
        plotConstraints = seed.plotConstraints,
        openingHook = seed.openingHook,
        promptBlocks = seed.promptBlocks,
        templateId = seed.templateId,
    )
}

actual suspend fun deleteLocalTemplate(
    templateId: Long,
) {
    TemplateRepositoryImpl(
        database = openDesktopDatabase(),
    ).deleteTemplate(templateId)
}

actual suspend fun enrichTemplateDraft(
    draft: TemplateDraft,
): TemplateDraft {
    val baseUrl = System.getenv("OLLAMA_BASE_URL")?.trim().orEmpty().ifBlank { "http://127.0.0.1:11434" }
    val modelName = System.getenv("OLLAMA_MODEL")?.trim().orEmpty()
        .ifBlank { "hf.co/TrevorJS/gemma-4-E4B-it-uncensored-GGUF:Q8_0" }
    val raw = DesktopOllamaModelsClient(baseUrl = baseUrl)
        .generate(model = modelName, prompt = buildTemplateEnrichmentPrompt(draft))
    return parseTemplateEnrichmentResponse(raw = raw, original = draft)
}
