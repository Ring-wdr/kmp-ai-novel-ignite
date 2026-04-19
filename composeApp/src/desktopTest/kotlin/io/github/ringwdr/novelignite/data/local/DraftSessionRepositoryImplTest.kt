package io.github.ringwdr.novelignite.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
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

    @Test
    fun latestDraftSessions_returnsLatestSessionPerProject() = runTest {
        val database = TestDatabaseFactory.create()
        val projectRepository = ProjectRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val firstProject = projectRepository.createProject(title = "Moon Archive", templateId = null)
        val secondProject = projectRepository.createProject(title = "Neon Debt", templateId = null)

        draftSessionRepository.saveDraftSession(
            projectId = firstProject.id,
            sessionId = null,
            content = "First draft",
        )
        val latestFirst = draftSessionRepository.saveDraftSession(
            projectId = firstProject.id,
            sessionId = null,
            content = "First draft revised",
        )
        val latestSecond = draftSessionRepository.saveDraftSession(
            projectId = secondProject.id,
            sessionId = null,
            content = "Second project draft",
        )

        val latestByProject = draftSessionRepository.latestDraftSessions()

        assertEquals(latestFirst.content, latestByProject[firstProject.id]?.content)
        assertEquals(latestSecond.content, latestByProject[secondProject.id]?.content)
    }

    @Test
    fun saveDraftSession_supportsConcurrentInsertsWithoutLosingInsertedRows() = runTest {
        val database = TestDatabaseFactory.create()
        val projectRepository = ProjectRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val project = projectRepository.createProject(title = "Concurrent Workshop Draft", templateId = null)

        coroutineScope {
            (1..40).map { index ->
                async(Dispatchers.Default) {
                    draftSessionRepository.saveDraftSession(
                        projectId = project.id,
                        sessionId = null,
                        content = "Draft $index",
                    )
                }
            }.awaitAll()
        }

        assertEquals(
            40,
            database.draftSessionQueries.selectDraftSessionsByProjectId(project.id)
                .executeAsList()
                .size,
        )
    }

    @Test
    fun saveDraftSession_remainsStableWhileOtherTablesInsertRowsConcurrently() = runTest {
        val database = TestDatabaseFactory.create()
        val projectRepository = ProjectRepositoryImpl(database)
        val draftSessionRepository = DraftSessionRepositoryImpl(database)
        val project = projectRepository.createProject(title = "Concurrent Workshop Draft", templateId = null)

        coroutineScope {
            val draftWrites = (1..40).map { index ->
                async(Dispatchers.Default) {
                    draftSessionRepository.saveDraftSession(
                        projectId = project.id,
                        sessionId = null,
                        content = "Draft $index",
                    )
                }
            }
            val projectWrites = (1..40).map { index ->
                async(Dispatchers.Default) {
                    projectRepository.createProject(
                        title = "Background Project $index",
                        templateId = null,
                    )
                }
            }

            (draftWrites + projectWrites).awaitAll()
        }

        assertEquals(
            40,
            database.draftSessionQueries.selectDraftSessionsByProjectId(project.id)
                .executeAsList()
                .size,
        )
    }
}
