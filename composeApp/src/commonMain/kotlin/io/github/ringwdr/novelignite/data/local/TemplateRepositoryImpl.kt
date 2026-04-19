package io.github.ringwdr.novelignite.data.local

import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.domain.repository.TemplateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlin.time.Clock

class TemplateRepositoryImpl(
    private val database: NovelIgniteDatabase,
    private val json: Json = Json,
) : TemplateRepository {
    override suspend fun saveTemplate(
        title: String,
        genre: String,
        premise: String,
        worldSetting: String,
        characterCards: String,
        relationshipNotes: String,
        toneStyle: String,
        bannedElements: String,
        plotConstraints: String,
        openingHook: String,
        promptBlocks: List<String>,
        templateId: Long?,
    ): Template = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()
        val encodedPromptBlocks = json.encodeToString(promptBlocks)
        database.transactionWithResult {
            val savedTemplate = if (templateId == null) {
                val persistedTemplateId = insertTemplate(
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
                    encodedPromptBlocks = encodedPromptBlocks,
                    now = now,
                )
                database.templateQueries.selectTemplateById(persistedTemplateId).executeAsOne()
            } else {
                database.templateQueries.updateTemplate(
                    id = templateId,
                    title = title,
                    genre = genre,
                    premise = premise,
                    world_setting = worldSetting,
                    character_cards = characterCards,
                    relationship_notes = relationshipNotes,
                    tone_style = toneStyle,
                    banned_elements = bannedElements,
                    plot_constraints = plotConstraints,
                    opening_hook = openingHook,
                    prompt_blocks_json = encodedPromptBlocks,
                    updated_at_epoch_ms = now,
                )

                database.templateQueries.selectTemplateById(templateId).executeAsOneOrNull()
                    ?: run {
                        val persistedTemplateId = insertTemplate(
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
                            encodedPromptBlocks = encodedPromptBlocks,
                            now = now,
                        )
                        database.templateQueries.selectTemplateById(persistedTemplateId).executeAsOne()
                    }
            }

            val nextVersionNumber = database.templateVersionQueries
                .selectMaxTemplateVersionNumberByTemplateId(savedTemplate.id)
                .executeAsOne() + 1L

            database.templateVersionQueries.insertTemplateVersion(
                template_id = savedTemplate.id,
                version_number = nextVersionNumber,
                title = savedTemplate.title,
                genre = savedTemplate.genre,
                premise = savedTemplate.premise,
                world_setting = savedTemplate.world_setting,
                character_cards = savedTemplate.character_cards,
                relationship_notes = savedTemplate.relationship_notes,
                tone_style = savedTemplate.tone_style,
                banned_elements = savedTemplate.banned_elements,
                plot_constraints = savedTemplate.plot_constraints,
                opening_hook = savedTemplate.opening_hook,
                prompt_blocks_json = savedTemplate.prompt_blocks_json,
                created_at_epoch_ms = now,
            )

            savedTemplate.toDomainModel(json)
        }
    }

    override fun listTemplates(): List<Template> = database.templateQueries.selectAllTemplates()
        .executeAsList()
        .map { it.toDomainModel(json) }

    override fun listTemplateVersions(templateId: Long): List<TemplateVersion> =
        database.templateVersionQueries.selectTemplateVersionsByTemplateId(templateId)
            .executeAsList()
            .map { it.toDomainModel(json) }

    override fun listAllTemplateVersions(): List<TemplateVersion> =
        database.templateVersionQueries.selectAllTemplateVersions()
            .executeAsList()
            .map { it.toDomainModel(json) }

    private fun insertTemplate(
        title: String,
        genre: String,
        premise: String,
        worldSetting: String,
        characterCards: String,
        relationshipNotes: String,
        toneStyle: String,
        bannedElements: String,
        plotConstraints: String,
        openingHook: String,
        encodedPromptBlocks: String,
        now: Long,
    ): Long {
        database.templateQueries.insertTemplate(
            title = title,
            genre = genre,
            premise = premise,
            world_setting = worldSetting,
            character_cards = characterCards,
            relationship_notes = relationshipNotes,
            tone_style = toneStyle,
            banned_elements = bannedElements,
            plot_constraints = plotConstraints,
            opening_hook = openingHook,
            prompt_blocks_json = encodedPromptBlocks,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
        )

        return database.templateQueries.lastInsertedRowId().executeAsOne()
    }
}

private fun io.github.ringwdr.novelignite.db.Template.toDomainModel(json: Json): Template = Template(
    id = id,
    title = title,
    genre = genre,
    premise = premise,
    worldSetting = world_setting,
    characterCards = character_cards,
    relationshipNotes = relationship_notes,
    toneStyle = tone_style,
    bannedElements = banned_elements,
    plotConstraints = plot_constraints,
    openingHook = opening_hook,
    promptBlocks = json.decodeFromString(prompt_blocks_json),
    createdAtEpochMs = created_at_epoch_ms,
    updatedAtEpochMs = updated_at_epoch_ms,
)

private fun io.github.ringwdr.novelignite.db.TemplateVersion.toDomainModel(json: Json): TemplateVersion =
    TemplateVersion(
        id = id,
        templateId = template_id,
        versionNumber = version_number,
        title = title,
        genre = genre,
        premise = premise,
        worldSetting = world_setting,
        characterCards = character_cards,
        relationshipNotes = relationship_notes,
        toneStyle = tone_style,
        bannedElements = banned_elements,
        plotConstraints = plot_constraints,
        openingHook = opening_hook,
        promptBlocks = json.decodeFromString(prompt_blocks_json),
        createdAtEpochMs = created_at_epoch_ms,
    )
