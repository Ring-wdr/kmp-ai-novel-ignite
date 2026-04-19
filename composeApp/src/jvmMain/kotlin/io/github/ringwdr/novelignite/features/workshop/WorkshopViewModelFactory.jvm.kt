package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import io.github.ringwdr.novelignite.data.local.DraftSessionRepositoryImpl
import io.github.ringwdr.novelignite.data.local.openDesktopDatabase
import kotlinx.serialization.json.Json

private const val DefaultWorkshopProjectTitle = "Workshop Draft"
private const val DefaultWorkshopTemplateId = "workshop-default-template"
private val workshopStateJson = Json { ignoreUnknownKeys = true }

actual fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel {
    val database = openDesktopDatabase()
    val activeTemplate = ActiveWorkshopTemplateStore.selection.value
    val project = ensureWorkshopProject(
        database = database,
        activeTemplate = activeTemplate,
    )
    val promptConfig = resolveWorkshopTemplatePromptConfig(
        database = database,
        activeTemplate = activeTemplate,
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
        templateId = promptConfig.templateId,
        templatePromptBlocks = promptConfig.promptBlocks,
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
    return database.transactionWithResult {
        database.projectQueries.insertProject(
            title = projectTitle,
            template_id = activeTemplate?.id,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
        )
        val projectId = database.projectQueries.lastInsertedRowId().executeAsOne()
        database.projectQueries.selectProjectById(projectId)
            .executeAsOneOrNull()
            ?.toDomainModel()
            ?: database.projectQueries.selectAllProjects()
                .executeAsList()
                .first { project ->
                    project.title == projectTitle &&
                        project.template_id == activeTemplate?.id &&
                        project.created_at_epoch_ms == now
                }
                .toDomainModel()
    }
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
    workshopStateJson.encodeToString(WorkshopStateSnapshot.from(state))

private fun parseStoredState(content: String): WorkshopUiState =
    runCatching { workshopStateJson.decodeFromString<WorkshopStateSnapshot>(content).toUiState() }
        .recoverCatching {
            workshopStateJson.decodeFromString<LegacyStoredWorkshopState>(content).toUiState()
        }
        .getOrElse { WorkshopUiState(draftText = content) }

@kotlinx.serialization.Serializable
private data class LegacyStoredWorkshopState(
    val draftText: String,
    val generatedText: String = "",
) {
    fun toUiState(): WorkshopUiState = WorkshopUiState(
        draftText = draftText,
        messages = generatedText.takeIf { it.isNotBlank() }
            ?.let { listOf(WorkshopChatMessage.assistant("legacy-generated", it)) }
            ?: emptyList(),
        streamingStatus = WorkshopStreamingStatus.Idle,
        errorMessage = null,
    )
}

internal data class WorkshopTemplatePromptConfig(
    val templateId: String,
    val promptBlocks: List<String>,
)

internal fun resolveWorkshopTemplatePromptConfig(
    database: NovelIgniteDatabase,
    activeTemplate: ActiveWorkshopTemplate?,
): WorkshopTemplatePromptConfig {
    if (activeTemplate == null) {
        return WorkshopTemplatePromptConfig(
            templateId = DefaultWorkshopTemplateId,
            promptBlocks = emptyList(),
        )
    }

    val template = TemplateRepositoryImpl(database)
        .listTemplates()
        .firstOrNull { candidate -> candidate.id == activeTemplate.id }

    return WorkshopTemplatePromptConfig(
        templateId = activeTemplate.id.toString(),
        promptBlocks = template?.promptBlocks.orEmpty(),
    )
}
