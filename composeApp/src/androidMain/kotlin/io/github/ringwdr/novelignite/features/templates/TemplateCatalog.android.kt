package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.time.Clock

private object AndroidTemplateMemoryStore {
    private var nextId: Long = 1
    private val templates = mutableListOf<Template>()

    fun list(): List<Template> = templates.toList()

    fun save(draft: TemplateDraft, templateId: Long?, originalTemplate: Template?): Template {
        val now = Clock.System.now().toEpochMilliseconds()
        val existing = when {
            originalTemplate != null -> originalTemplate
            templateId != null -> templates.firstOrNull { it.id == templateId }
            else -> null
        }

        val saved = Template(
            id = existing?.id ?: nextId++,
            title = draft.title,
            genre = draft.genre,
            premise = draft.premise,
            worldSetting = existing?.worldSetting.orEmpty(),
            characterCards = existing?.characterCards.orEmpty(),
            relationshipNotes = existing?.relationshipNotes.orEmpty(),
            toneStyle = existing?.toneStyle.orEmpty(),
            bannedElements = existing?.bannedElements.orEmpty(),
            plotConstraints = existing?.plotConstraints.orEmpty(),
            openingHook = existing?.openingHook.orEmpty(),
            promptBlocks = draft.promptBlocks,
            createdAtEpochMs = existing?.createdAtEpochMs ?: now,
            updatedAtEpochMs = now,
        )

        templates.removeAll { it.id == saved.id }
        templates.add(0, saved)
        return saved
    }
}

actual fun loadLocalTemplates(): List<Template> = AndroidTemplateMemoryStore.list()

actual suspend fun saveLocalTemplate(
    draft: TemplateDraft,
    templateId: Long?,
    originalTemplate: Template?,
): Template = AndroidTemplateMemoryStore.save(
    draft = draft,
    templateId = templateId,
    originalTemplate = originalTemplate,
)
