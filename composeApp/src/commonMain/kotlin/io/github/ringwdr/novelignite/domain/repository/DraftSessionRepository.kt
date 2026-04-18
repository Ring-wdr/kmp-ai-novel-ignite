package io.github.ringwdr.novelignite.domain.repository

import io.github.ringwdr.novelignite.domain.model.DraftSession

interface DraftSessionRepository {
    fun latestDraftSession(projectId: Long): DraftSession?

    fun latestDraftSessions(): Map<Long, DraftSession>

    suspend fun saveDraftSession(
        projectId: Long,
        sessionId: Long?,
        content: String,
    ): DraftSession
}
