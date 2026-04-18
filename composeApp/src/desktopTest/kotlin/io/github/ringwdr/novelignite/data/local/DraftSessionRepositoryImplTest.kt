package io.github.ringwdr.novelignite.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.test.runTest

class DraftSessionRepositoryImplTest {
    @Test
    fun saveDraftSession_insertsAndLoadsLatestSession() = runTest {
        val database = TestDatabaseFactory.create()
        val projectRepository = ProjectRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val project = projectRepository.createProject(title = "Workshop Draft", templateId = null)

        val saved = draftSessionRepository.saveDraftSession(
            projectId = project.id,
            sessionId = null,
            content = "The gate opened.",
        )

        val latest = draftSessionRepository.latestDraftSession(project.id)

        assertNotNull(latest)
        assertEquals(saved.id, latest.id)
        assertEquals("The gate opened.", latest.content)
    }

    @Test
    fun saveDraftSession_updatesExistingSessionWhenSessionIdProvided() = runTest {
        val database = TestDatabaseFactory.create()
        val projectRepository = ProjectRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val project = projectRepository.createProject(title = "Workshop Draft", templateId = null)
        val first = draftSessionRepository.saveDraftSession(
            projectId = project.id,
            sessionId = null,
            content = "The gate opened.",
        )

        val updated = draftSessionRepository.saveDraftSession(
            projectId = project.id,
            sessionId = first.id,
            content = "The gate opened. A silver wind answered.",
        )

        assertEquals(first.id, updated.id)
        assertEquals("The gate opened. A silver wind answered.", updated.content)
        assertEquals(
            1,
            database.draftSessionQueries.selectDraftSessionsByProjectId(project.id)
                .executeAsList()
                .size,
        )
    }
}
