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

    fun listAllVersions(): List<TemplateVersion> = versionsByTemplateId.values
        .flatten()
        .sortedWith(
            compareByDescending<TemplateVersion> { it.createdAtEpochMs }
                .thenByDescending { it.id }
        )

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
    originalVersion: TemplateVersion?,
): Template {
    val resolvedOriginalTemplate = when {
        originalTemplate != null -> originalTemplate
        templateId != null -> AndroidTemplateMemoryStore.list().firstOrNull { it.id == templateId }
        else -> null
    }
    val seed = resolveTemplateSaveSeed(
        draft = draft,
        templateId = templateId,
        originalTemplate = resolvedOriginalTemplate,
        originalVersion = originalVersion,
    )
    val preservedTemplate = resolvedOriginalTemplate ?: originalVersion?.let { version ->
        Template(
            id = version.templateId,
            title = version.title,
            genre = version.genre,
            premise = version.premise,
            worldSetting = version.worldSetting,
            characterCards = version.characterCards,
            relationshipNotes = version.relationshipNotes,
            toneStyle = version.toneStyle,
            bannedElements = version.bannedElements,
            plotConstraints = version.plotConstraints,
            openingHook = version.openingHook,
            promptBlocks = version.promptBlocks,
            createdAtEpochMs = version.createdAtEpochMs,
            updatedAtEpochMs = version.createdAtEpochMs,
        )
    }

    return AndroidTemplateMemoryStore.save(
        draft = TemplateDraft(
            title = seed.title,
            genre = seed.genre,
            premise = seed.premise,
            promptBlocks = seed.promptBlocks,
        ),
        templateId = seed.templateId,
        originalTemplate = preservedTemplate?.copy(
            worldSetting = seed.worldSetting,
            characterCards = seed.characterCards,
            relationshipNotes = seed.relationshipNotes,
            toneStyle = seed.toneStyle,
            bannedElements = seed.bannedElements,
            plotConstraints = seed.plotConstraints,
            openingHook = seed.openingHook,
        ),
    )
}
