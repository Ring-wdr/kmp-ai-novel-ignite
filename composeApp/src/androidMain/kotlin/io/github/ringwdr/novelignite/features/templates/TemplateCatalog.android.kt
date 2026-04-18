package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import kotlin.time.Clock

internal object AndroidTemplateMemoryStore {
    private var nextId: Long = 1
    private var nextVersionId: Long = 1
    private val templates = mutableListOf<Template>()
    private val versionsByTemplateId = linkedMapOf<Long, MutableList<TemplateVersion>>()

    fun list(): List<Template> = templates.toList()

    fun listVersions(templateId: Long): List<TemplateVersion> =
        versionsByTemplateId[templateId].orEmpty().toList()

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

        val templateVersions = versionsByTemplateId.getOrPut(saved.id) { mutableListOf() }
        val nextVersionNumber = (templateVersions.maxOfOrNull { it.versionNumber } ?: 0L) + 1L
        templateVersions.add(
            0,
            TemplateVersion(
                id = nextVersionId++,
                templateId = saved.id,
                versionNumber = nextVersionNumber,
                title = saved.title,
                genre = saved.genre,
                premise = saved.premise,
                worldSetting = saved.worldSetting,
                characterCards = saved.characterCards,
                relationshipNotes = saved.relationshipNotes,
                toneStyle = saved.toneStyle,
                bannedElements = saved.bannedElements,
                plotConstraints = saved.plotConstraints,
                openingHook = saved.openingHook,
                promptBlocks = saved.promptBlocks,
                createdAtEpochMs = now,
            )
        )
        return saved
    }
}

actual fun loadLocalTemplates(): List<Template> = AndroidTemplateMemoryStore.list()

actual fun loadLocalTemplateVersions(templateId: Long): List<TemplateVersion> =
    AndroidTemplateMemoryStore.listVersions(templateId)

actual suspend fun saveLocalTemplate(
    draft: TemplateDraft,
    templateId: Long?,
    originalTemplate: Template?,
): Template = AndroidTemplateMemoryStore.save(
    draft = draft,
    templateId = templateId,
    originalTemplate = originalTemplate,
)
