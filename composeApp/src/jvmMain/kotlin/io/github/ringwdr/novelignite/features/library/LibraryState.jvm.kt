package io.github.ringwdr.novelignite.features.library

import io.github.ringwdr.novelignite.data.local.DraftSessionRepositoryImpl
import io.github.ringwdr.novelignite.data.local.ProjectRepositoryImpl
import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.data.local.openDesktopDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

actual suspend fun loadLocalLibraryState(): LibraryState {
    return withContext(Dispatchers.IO) {
        val database = openDesktopDatabase()
        val projectRepository = ProjectRepositoryImpl(database)
        val templateRepository = TemplateRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val latestDraftSessions = draftSessionRepository.latestDraftSessions()

        buildLibraryState(
            projects = projectRepository.observeProjects(),
            templates = templateRepository.listTemplates(),
            latestDraftSession = { projectId -> latestDraftSessions[projectId] },
        )
    }
}
