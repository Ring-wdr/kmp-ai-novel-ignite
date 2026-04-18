package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine

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
}
