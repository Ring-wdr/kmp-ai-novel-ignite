package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkshopViewModel(
    private val inferenceEngine: InferenceEngine,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(WorkshopUiState())
    val state: StateFlow<WorkshopUiState> = _state

    fun updateDraft(text: String) {
        _state.update { it.copy(draftText = text) }
    }

    fun continueScene() {
        _state.update { it.copy(isGenerating = true) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            inferenceEngine.streamGenerate(
                GenerationRequest(
                    projectId = "local-project",
                    templateId = "workshop-default-template",
                    actionType = "continue",
                    manuscriptExcerpt = _state.value.draftText,
                    promptBlocks = emptyList(),
                )
            ).collect { event ->
                when (event) {
                    is GenerationEvent.Final -> _state.update {
                        it.copy(
                            generatedText = event.text,
                            isGenerating = false,
                        )
                    }
                    else -> Unit
                }
            }
        }
    }
}
