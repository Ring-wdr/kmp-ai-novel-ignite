package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopTypedStreamPrototypeSmokeTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun fixtureModeShowsMarkdownAndChoiceSurface() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        val viewModel = WorkshopViewModel(
            streamSource = FixtureWorkshopAssistantStreamSource(),
            scope = scope,
        )

        try {
            rule.setContent {
                WorkshopScreen(viewModel = viewModel)
            }

            viewModel.continueScene()
            runCurrent()
            advanceUntilIdle()
            rule.waitForIdle()

            rule.onNodeWithTag("workshop-choice-continue-fixture-turn").assertIsEnabled()

            rule.onNodeWithTag("workshop-choice-continue-fixture-turn").performClick()
            runCurrent()
            advanceUntilIdle()
            rule.waitForIdle()

            val userPrompts = viewModel.state.value.messages
                .filter { it.role == WorkshopMessageRole.User }
                .map { it.text }
            assertEquals(
                listOf(
                    "Continue scene",
                    "Continue the scene from Fixture turn.",
                ),
                userPrompts,
            )

            val latestAssistant = viewModel.state.value.messages.last().assistant
            assertTrue(
                latestAssistant?.renderedMarkdown.orEmpty().contains("Prompt: Continue the scene from Fixture turn."),
                "Expected the fixture assistant turn to echo the choice prompt.",
            )
        } finally {
            viewModel.clear()
        }
    }
}
