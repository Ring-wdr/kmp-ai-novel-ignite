package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine

@Composable
fun WorkshopScreen(
    viewModel: WorkshopViewModel? = null,
) {
    val internalViewModel = if (viewModel == null) rememberWorkshopViewModel() else null
    val activeViewModel = viewModel ?: internalViewModel!!
    val state by activeViewModel.state.collectAsState()

    if (internalViewModel != null) {
        DisposableEffect(internalViewModel) {
            onDispose { internalViewModel.clear() }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ChatPanel(
            generatedText = state.generatedText,
            isGenerating = state.isGenerating,
            onContinueScene = activeViewModel::continueScene,
        )
        ManuscriptEditor(
            text = state.draftText,
            onTextChange = activeViewModel::updateDraft,
        )
    }
}

@Composable
private fun rememberWorkshopViewModel(): WorkshopViewModel = remember {
    WorkshopViewModel(FakeInferenceEngine())
}
