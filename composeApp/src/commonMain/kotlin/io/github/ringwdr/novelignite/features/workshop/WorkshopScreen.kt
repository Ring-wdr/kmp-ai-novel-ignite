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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.data.inference.FakeInferenceEngine
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import org.koin.mp.KoinPlatform.getKoin

@Composable
fun WorkshopScreen(
    viewModel: WorkshopViewModel? = null,
) {
    val internalViewModel = if (viewModel == null) rememberWorkshopViewModel() else null
    val activeViewModel = viewModel ?: internalViewModel!!
    val state by activeViewModel.state.collectAsState()
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()

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
        Text(
            text = activeTemplate?.let { "Workshop template: ${it.title}" } ?: "Workshop template: none",
            style = MaterialTheme.typography.bodyMedium,
        )
        ChatPanel(
            messages = state.messages,
            chatInputText = state.chatInputText,
            streamingStatus = state.streamingStatus,
            errorMessage = state.errorMessage,
            onChatInputChange = activeViewModel::updateChatInput,
            onSendChatMessage = activeViewModel::sendChatMessage,
            onUseChoice = activeViewModel::updateChatInput,
            onContinueScene = activeViewModel::continueScene,
            onAbortGeneration = activeViewModel::abortGeneration,
        )
        ManuscriptEditor(
            text = state.draftText,
            onTextChange = activeViewModel::updateDraft,
        )
    }
}

@Composable
private fun rememberWorkshopViewModel(): WorkshopViewModel = remember {
    val inferenceEngine = runCatching { getKoin().get<InferenceEngine>() }
        .getOrElse { FakeInferenceEngine() }
    createDefaultWorkshopViewModel(inferenceEngine)
}
