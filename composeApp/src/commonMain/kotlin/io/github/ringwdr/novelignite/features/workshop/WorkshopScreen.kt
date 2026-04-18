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
    viewModel: WorkshopViewModel = rememberWorkshopViewModel(),
) {
    val state by viewModel.state.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.clear() }
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
            onContinueScene = viewModel::continueScene,
        )
        ManuscriptEditor(
            text = state.draftText,
            onTextChange = viewModel::updateDraft,
        )
    }
}

@Composable
private fun rememberWorkshopViewModel(): WorkshopViewModel = remember {
    WorkshopViewModel(FakeInferenceEngine())
}
