package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkshopViewModel(
    private val inferenceEngine: InferenceEngine,
    private val scope: CoroutineScope = defaultWorkshopScope(),
) {
    private val _state = MutableStateFlow(WorkshopUiState())
    val state: StateFlow<WorkshopUiState> = _state

    fun updateDraft(text: String) {
        _state.update { it.copy(draftText = text) }
    }

    fun continueScene() {
        _state.update { it.copy(isGenerating = true) }
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
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
                            it.copy(generatedText = event.text)
                        }
                        is GenerationEvent.Error -> _state.update {
                            it.copy(isGenerating = false)
                        }
                        else -> Unit
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException) throw throwable
            } finally {
                _state.update { it.copy(isGenerating = false) }
            }
        }
    }

    fun clear() {
        scope.cancel()
    }
}

private fun defaultWorkshopScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
