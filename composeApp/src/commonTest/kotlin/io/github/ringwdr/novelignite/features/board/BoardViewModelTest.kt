package io.github.ringwdr.novelignite.features.board

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class BoardViewModelTest {
    @Test
    fun refresh_loadsLatestLocalSnapshotsAndSelectsNewest() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val templateA = template(id = 1, title = "Noir Seoul", updatedAtEpochMs = 100)
        val templateB = template(id = 2, title = "Moon Archive", updatedAtEpochMs = 200)
        val viewModel = BoardViewModel(
            loadTemplates = { listOf(templateA, templateB) },
            loadTemplateVersions = { templateId ->
                when (templateId) {
                    1L -> listOf(
                        version(id = 11, templateId = 1, versionNumber = 2, title = "Noir Seoul Revised", createdAtEpochMs = 220),
                        version(id = 10, templateId = 1, versionNumber = 1, title = "Noir Seoul", createdAtEpochMs = 120),
                    )

                    2L -> listOf(
                        version(id = 20, templateId = 2, versionNumber = 1, title = "Moon Archive", createdAtEpochMs = 180),
                    )

                    else -> emptyList()
                }
            },
            scope = scope,
        )

        runCurrent()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals(listOf(11L, 20L, 10L), viewModel.state.value.snapshots.map { it.id })
        assertEquals(11L, viewModel.state.value.selectedSnapshotId)
        scope.coroutineContext[Job]?.cancel()
    }

    @Test
    fun startRemix_seedsEditorDraftFromSelectedSnapshot() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val sourceVersion = version(
            id = 11,
            templateId = 1,
            versionNumber = 2,
            title = "Noir Seoul Revised",
            genre = "Urban Fantasy",
            premise = "A broker bargains with a ghost market",
            promptBlocks = listOf("Keep sensory detail high", "Keep dialogue sharp"),
            createdAtEpochMs = 220,
        )
        val viewModel = BoardViewModel(
            loadTemplates = { listOf(template(id = 1, title = "Noir Seoul", updatedAtEpochMs = 100)) },
            loadTemplateVersions = { listOf(sourceVersion) },
            scope = scope,
        )

        runCurrent()
        viewModel.startRemix()

        val remix = assertNotNull(viewModel.state.value.activeRemix)
        assertEquals(11L, remix.sourceVersion.id)
        assertEquals(
            "Noir Seoul Revised Remix",
            remix.draft.title,
        )
        assertEquals("Urban Fantasy", remix.draft.genre)
        assertEquals("A broker bargains with a ghost market", remix.draft.premise)
        assertEquals(listOf("Keep sensory detail high", "Keep dialogue sharp"), remix.draft.promptBlocks)
    }
}

private fun template(
    id: Long,
    title: String,
    updatedAtEpochMs: Long,
): Template = Template(
    id = id,
    title = title,
    genre = "Urban Fantasy",
    premise = "Premise",
    worldSetting = "World",
    characterCards = "Characters",
    relationshipNotes = "Notes",
    toneStyle = "Tone",
    bannedElements = "Banned",
    plotConstraints = "Constraints",
    openingHook = "Hook",
    promptBlocks = listOf("Keep sensory detail high"),
    createdAtEpochMs = updatedAtEpochMs,
    updatedAtEpochMs = updatedAtEpochMs,
)

private fun version(
    id: Long,
    templateId: Long,
    versionNumber: Long,
    title: String,
    genre: String = "Urban Fantasy",
    premise: String = "Premise",
    promptBlocks: List<String> = listOf("Keep sensory detail high"),
    createdAtEpochMs: Long,
): TemplateVersion = TemplateVersion(
    id = id,
    templateId = templateId,
    versionNumber = versionNumber,
    title = title,
    genre = genre,
    premise = premise,
    worldSetting = "World",
    characterCards = "Characters",
    relationshipNotes = "Notes",
    toneStyle = "Tone",
    bannedElements = "Banned",
    plotConstraints = "Constraints",
    openingHook = "Hook",
    promptBlocks = promptBlocks,
    createdAtEpochMs = createdAtEpochMs,
)
