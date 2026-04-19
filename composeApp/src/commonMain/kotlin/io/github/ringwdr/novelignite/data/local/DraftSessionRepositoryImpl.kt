package io.github.ringwdr.novelignite.data.local

import io.github.ringwdr.novelignite.db.NovelIgniteDatabase
import io.github.ringwdr.novelignite.domain.model.DraftSession
import io.github.ringwdr.novelignite.domain.repository.DraftSessionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Clock

class DraftSessionRepositoryImpl(
    private val database: NovelIgniteDatabase,
) : DraftSessionRepository {
    private val writeMutex = Mutex()

    override fun latestDraftSession(projectId: Long): DraftSession? =
        database.draftSessionQueries.selectLatestDraftSessionByProjectId(projectId)
            .executeAsOneOrNull()
            ?.toDomainModel()

    override fun latestDraftSessions(): Map<Long, DraftSession> =
        database.draftSessionQueries.selectLatestDraftSessions()
            .executeAsList()
            .map { it.toDomainModel() }
            .associateBy(DraftSession::projectId)

    override suspend fun saveDraftSession(
        projectId: Long,
        sessionId: Long?,
        content: String,
    ): DraftSession = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val now = Clock.System.now().toEpochMilliseconds()
            database.transactionWithResult {
                if (sessionId == null) {
                    database.draftSessionQueries.insertDraftSession(
                        project_id = projectId,
                        content = content,
                        created_at_epoch_ms = now,
                        updated_at_epoch_ms = now,
                    )
                    val draftSessionId = database.draftSessionQueries.lastInsertedDraftSessionRowId()
                        .executeAsOne()
                    database.draftSessionQueries.selectDraftSessionById(draftSessionId)
                        .executeAsOneOrNull()
                        ?.toDomainModel()
                        ?: database.draftSessionQueries.selectLatestDraftSessionByProjectId(projectId)
                            .executeAsOne()
                            .toDomainModel()
                } else {
                    database.draftSessionQueries.updateDraftSessionContentById(
                        content = content,
                        updated_at_epoch_ms = now,
                        id = sessionId,
                    )
                    database.draftSessionQueries.selectDraftSessionById(sessionId)
                        .executeAsOneOrNull()
                        ?.toDomainModel()
                        ?: error("Draft session $sessionId not found after update")
                }
            }
        }
    }
}

private fun io.github.ringwdr.novelignite.db.DraftSession.toDomainModel(): DraftSession = DraftSession(
    id = id,
    projectId = project_id,
    content = content,
    createdAtEpochMs = created_at_epoch_ms,
    updatedAtEpochMs = updated_at_epoch_ms,
)
