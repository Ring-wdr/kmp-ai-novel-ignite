package io.github.ringwdr.novelignite.data.local

import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.model.Project
import io.github.ringwdr.novelignite.domain.repository.ProjectRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class ProjectRepositoryImpl(
    private val database: NovelIgniteDatabase,
) : ProjectRepository {
    override suspend fun createProject(title: String, templateId: Long?): Project = withContext(Dispatchers.IO) {
        val now = Clock.System.now().toEpochMilliseconds()

        database.projectQueries.insertProject(
            title = title,
            template_id = templateId,
            created_at_epoch_ms = now,
            updated_at_epoch_ms = now,
        )

        database.projectQueries.selectAllProjects()
            .executeAsOne()
            .toDomainModel()
    }

    override fun observeProjects(): List<Project> = database.projectQueries.selectAllProjects()
        .executeAsList()
        .map { it.toDomainModel() }
}

private fun io.github.ringwdr.novelignite.db.Project.toDomainModel(): Project = Project(
    id = id,
    title = title,
    templateId = template_id,
    createdAtEpochMs = created_at_epoch_ms,
    updatedAtEpochMs = updated_at_epoch_ms,
)
