package io.github.ringwdr.novelignite.features.workshop

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import io.github.ringwdr.novelignite.data.local.DraftSessionRepositoryImpl
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val WorkshopProjectTitle = "Workshop Draft"
private val workshopStateJson = Json { ignoreUnknownKeys = true }

actual fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel {
    val database = createDesktopDatabase()
    val project = ensureWorkshopProject(database)
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

private fun createDesktopDatabase(): NovelIgniteDatabase {
    val databaseDirectory = File(System.getProperty("user.home"), ".novelignite")
    databaseDirectory.mkdirs()
    val databaseFile = File(databaseDirectory, "novel-ignite.db")
    val driver = JdbcSqliteDriver("jdbc:sqlite:${databaseFile.absolutePath}")
    driver.execute(null, "PRAGMA foreign_keys = ON", 0)
    if (!databaseFile.exists() || databaseFile.length() == 0L) {
        NovelIgniteDatabase.Schema.create(driver)
    }
    return NovelIgniteDatabase(driver)
}

private fun ensureWorkshopProject(database: NovelIgniteDatabase): io.github.ringwdr.novelignite.domain.model.Project {
    val existing = database.projectQueries.selectAllProjects()
        .executeAsList()
        .firstOrNull { it.title == WorkshopProjectTitle }
    if (existing != null) {
        return io.github.ringwdr.novelignite.domain.model.Project(
            id = existing.id,
            title = existing.title,
            templateId = existing.template_id,
            createdAtEpochMs = existing.created_at_epoch_ms,
            updatedAtEpochMs = existing.updated_at_epoch_ms,
        )
    }

    val now = kotlin.time.Clock.System.now().toEpochMilliseconds()
    database.projectQueries.insertProject(
        title = WorkshopProjectTitle,
        template_id = null,
        created_at_epoch_ms = now,
        updated_at_epoch_ms = now,
    )
    val projectId = database.projectQueries.lastInsertedRowId().executeAsOne()
    val created = database.projectQueries.selectProjectById(projectId).executeAsOne()
    return io.github.ringwdr.novelignite.domain.model.Project(
        id = created.id,
        title = created.title,
        templateId = created.template_id,
        createdAtEpochMs = created.created_at_epoch_ms,
        updatedAtEpochMs = created.updated_at_epoch_ms,
    )
}

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
