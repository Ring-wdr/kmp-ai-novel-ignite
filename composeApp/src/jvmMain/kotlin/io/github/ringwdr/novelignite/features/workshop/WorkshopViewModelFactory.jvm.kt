package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import io.github.ringwdr.novelignite.data.local.DraftSessionRepositoryImpl
import io.github.ringwdr.novelignite.data.local.openDesktopDatabase
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val DefaultWorkshopProjectTitle = "Workshop Draft"
private val workshopStateJson = Json { ignoreUnknownKeys = true }

actual fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel {
    val database = openDesktopDatabase()
    val project = ensureWorkshopProject(
        database = database,
        activeTemplate = ActiveWorkshopTemplateStore.selection.value,
    )
    val repository = DraftSessionRepositoryImpl(database)
    val latestSession = repository.latestDraftSession(project.id)
    var currentSessionId = latestSession?.id
    val initialState = latestSession
        ?.let { parseStoredState(it.content) }
        ?: WorkshopUiState()

    return WorkshopViewModel(
        inferenceEngine = inferenceEngine,
        initialState = initialState,
        persistState = { state ->
            val content = buildStoredContent(state)
            val saved = repository.saveDraftSession(
                projectId = project.id,
                sessionId = currentSessionId,
                content = content,
            )
            currentSessionId = saved.id
        },
    )
}

private fun ensureWorkshopProject(
    database: NovelIgniteDatabase,
    activeTemplate: ActiveWorkshopTemplate?,
): io.github.ringwdr.novelignite.domain.model.Project {
    val projectTitle = activeTemplate?.title?.let { "Workshop: $it" } ?: DefaultWorkshopProjectTitle
    val existing = database.projectQueries.selectAllProjects()
        .executeAsList()
        .firstOrNull { project ->
            if (activeTemplate != null) {
                project.template_id == activeTemplate.id
            } else {
                project.title == DefaultWorkshopProjectTitle && project.template_id == null
            }
        }
    if (existing != null) {
        return existing.toDomainModel()
    }

    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    database.projectQueries.insertProject(
        title = projectTitle,
        template_id = activeTemplate?.id,
        created_at_epoch_ms = now,
        updated_at_epoch_ms = now,
    )
    val projectId = database.projectQueries.lastInsertedRowId().executeAsOne()
    return database.projectQueries.selectProjectById(projectId).executeAsOne().toDomainModel()
}

private fun io.github.ringwdr.novelignite.db.Project.toDomainModel(): io.github.ringwdr.novelignite.domain.model.Project =
    io.github.ringwdr.novelignite.domain.model.Project(
        id = id,
        title = title,
        templateId = template_id,
        createdAtEpochMs = created_at_epoch_ms,
        updatedAtEpochMs = updated_at_epoch_ms,
    )

private fun buildStoredContent(state: WorkshopUiState): String =
    workshopStateJson.encodeToString(StoredWorkshopState.from(state))

private fun parseStoredState(content: String): WorkshopUiState =
    runCatching { workshopStateJson.decodeFromString<StoredWorkshopState>(content).toUiState() }
        .getOrElse { WorkshopUiState(draftText = content) }

@Serializable
private data class StoredWorkshopState(
    val draftText: String,
    val generatedText: String = "",
) {
    fun toUiState(): WorkshopUiState = WorkshopUiState(
        draftText = draftText,
        generatedText = generatedText,
    )

    companion object {
        fun from(state: WorkshopUiState): StoredWorkshopState = StoredWorkshopState(
            draftText = state.draftText,
            generatedText = state.generatedText,
        )
    }
}
