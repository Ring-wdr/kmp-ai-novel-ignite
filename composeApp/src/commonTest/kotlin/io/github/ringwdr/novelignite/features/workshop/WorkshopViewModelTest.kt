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
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Error("boom"))
                }
            }
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()

        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun continueScene_clearsGeneratingWhenEngineThrows() = runTest {
        val viewModel = WorkshopViewModel(
            object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    throw IllegalStateException("boom")
                }
            }
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()

        assertFalse(viewModel.state.value.isGenerating)
    }

    @Test
    fun clear_cancelsWorkshopScope() {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val viewModel = WorkshopViewModel(FakeInferenceEngine(), scope)

        viewModel.clear()

        assertTrue(scope.coroutineContext[Job]!!.isCancelled)
    }
}
