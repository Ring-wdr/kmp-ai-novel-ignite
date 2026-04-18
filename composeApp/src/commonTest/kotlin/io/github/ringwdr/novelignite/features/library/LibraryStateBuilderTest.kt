package io.github.ringwdr.novelignite.features.library

import io.github.ringwdr.novelignite.domain.model.DraftSession
import io.github.ringwdr.novelignite.domain.model.Project
import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import kotlin.test.assertEquals

class LibraryStateBuilderTest {
    @Test
    fun buildLibraryState_includesProjectsTemplatesAndLatestDraftPreview() {
        val project = Project(
            id = 7,
            title = "Moon Archive",
            templateId = 2,
            createdAtEpochMs = 10,
            updatedAtEpochMs = 30,
        )
        val template = Template(
            id = 2,
            title = "Noir Seoul",
            genre = "Urban Fantasy",
            premise = "A ghost broker solves debts",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
            createdAtEpochMs = 11,
            updatedAtEpochMs = 40,
        )
        val draftSession = DraftSession(
            id = 11,
            projectId = project.id,
            content = "The gate opened.\nA silver wind answered.",
            createdAtEpochMs = 12,
            updatedAtEpochMs = 50,
        )

        val state = buildLibraryState(
            projects = listOf(project),
            templates = listOf(template),
            latestDraftSession = { projectId ->
                if (projectId == project.id) draftSession else null
            },
        )

        assertEquals(
            listOf(
                LibraryProjectSummary(
                    id = 7,
                    title = "Moon Archive",
                    templateTitle = "Noir Seoul",
                    latestDraftPreview = "The gate opened.",
                    hasLatestDraftSession = true,
                    updatedAtEpochMs = 30,
                ),
            ),
            state.projects,
        )
        assertEquals(
            listOf(
                LibraryTemplateSummary(
                    id = 2,
                    title = "Noir Seoul",
                    genre = "Urban Fantasy",
                    promptBlockCount = 2,
                    updatedAtEpochMs = 40,
                ),
            ),
            state.templates,
        )
    }
}
