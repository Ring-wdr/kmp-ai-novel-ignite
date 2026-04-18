package io.github.ringwdr.novelignite.data.local

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class ProjectRepositoryImplTest {
    @Test
    fun createProject_persistsAndReturnsStoredEntity() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = ProjectRepositoryImpl(database)

        repository.createProject(title = "Moon Archive", templateId = null)
        val projects = repository.observeProjects()

        assertEquals(1, projects.size)
        assertEquals("Moon Archive", projects.first().title)
    }
}
