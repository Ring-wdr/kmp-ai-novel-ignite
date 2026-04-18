package io.github.ringwdr.novelignite.features.library

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {
    @Test
    fun refresh_loadsLibraryStateOffTheInitialPlaceholder() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val expected = LibraryState(
            projects = listOf(
                LibraryProjectSummary(
                    id = 1,
                    title = "Moon Archive",
                    templateTitle = "Noir Seoul",
                    latestDraftPreview = "The gate opened.",
                    hasLatestDraftSession = true,
                    updatedAtEpochMs = 10,
                )
            ),
            templates = listOf(
                LibraryTemplateSummary(
                    id = 2,
                    title = "Noir Seoul",
                    genre = "Urban Fantasy",
                    promptBlockCount = 2,
                    updatedAtEpochMs = 11,
                )
            ),
        )
        val viewModel = LibraryViewModel(
            loadState = { expected },
            scope = scope,
        )

        assertTrue(viewModel.state.value.isLoading)

        runCurrent()

        assertEquals(expected, viewModel.state.value)
        viewModel.clear()
    }

    @Test
    fun refresh_exposesErrorAndStopsLoadingWhenLoadFails() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val viewModel = LibraryViewModel(
            loadState = { error("db unavailable") },
            scope = scope,
        )

        runCurrent()

        assertEquals(false, viewModel.state.value.isLoading)
        assertEquals("db unavailable", viewModel.state.value.errorMessage)
        viewModel.clear()
    }
}
