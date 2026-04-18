package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopViewModelTest {
    @Test
    fun continueScene_appendsGeneratedTextToDraft() = runTest {
        val viewModel = WorkshopViewModel(FakeInferenceEngine())

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()

        assertEquals(
            "The gate opened. A silver wind answered.",
            viewModel.state.value.generatedText
        )
    }

    @Test
    fun continueScene_clearsGeneratingWhenEngineSignalsError() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Error("boom"))
                }
            },
            scope,
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()
        runCurrent()

        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun continueScene_allowsRetryAfterEngineSignalsError() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val hold = Channel<Unit>()
        var attempt = 0
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    attempt += 1
                    if (attempt == 1) {
                        emit(GenerationEvent.Error("boom"))
                        hold.receive()
                    } else {
                        emit(GenerationEvent.Final("recovered"))
                    }
                }
            },
            scope,
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()
        runCurrent()
        viewModel.continueScene()
        runCurrent()

        assertEquals("recovered", viewModel.state.value.generatedText)
        assertFalse(viewModel.state.value.isGenerating)
        assertEquals(2, attempt)
    }

    @Test
    fun continueScene_clearsGeneratingWhenEngineThrows() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    throw IllegalStateException("boom")
                }
            },
            scope,
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()
        runCurrent()

        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun clear_cancelsWorkshopScope() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = WorkshopViewModel(FakeInferenceEngine(), scope)

        viewModel.clear()

        assertTrue(scope.coroutineContext[Job]!!.isCancelled)
    }

    @Test
    fun continueScene_ignoresSecondCallWhileGenerationIsActive() = runTest {
        val release = Channel<Unit>()
        val requestCount = mutableListOf<GenerationRequest>()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requestCount += request
                    emit(GenerationEvent.Token("one"))
                    release.receive()
                    emit(GenerationEvent.Final("one"))
                }
            },
            scope,
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()
        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requestCount.size)
        assertTrue(viewModel.state.value.isGenerating)
        assertEquals("", viewModel.state.value.generatedText)

        release.trySend(Unit)
        runCurrent()

        assertEquals("one", viewModel.state.value.generatedText)
        assertFalse(viewModel.state.value.isGenerating)
    }
}
