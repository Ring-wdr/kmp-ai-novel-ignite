package io.github.ringwdr.novelignite.data.local

import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.model.Template
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

        val persistedTemplateId = if (templateId == null) {
            insertTemplate(
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

            database.templateQueries.selectTemplateById(templateId).executeAsOneOrNull()?.id
                ?: insertTemplate(
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
        }

        database.templateQueries.selectTemplateById(persistedTemplateId)
            .executeAsOne()
            .toDomainModel(json)
    }

    override fun listTemplates(): List<Template> = database.templateQueries.selectAllTemplates()
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
