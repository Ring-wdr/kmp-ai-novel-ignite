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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.launch

class WorkshopViewModel(
    private val inferenceEngine: InferenceEngine,
    private val templateId: String = "workshop-default-template",
    private val templatePromptBlocks: List<String> = emptyList(),
    initialState: WorkshopUiState = WorkshopUiState(),
    private val persistState: suspend (WorkshopUiState) -> Unit = {},
    private val scope: CoroutineScope = defaultWorkshopScope(),
) {
    private val _state = MutableStateFlow(initialState)
    private var nextGenerationId = initialState.restoredGenerationIdFloor()
    private var activeGenerationId = 0
    private var activeAssistantMessageId: String? = null
    private var activeGenerationJob: Job? = null
    private val persistMutex = Mutex()
    val state: StateFlow<WorkshopUiState> = _state

    fun updateDraft(text: String) {
        _state.update { it.copy(draftText = text) }
        persistCurrentState()
    }

    fun updateChatInput(text: String) {
        _state.update { it.copy(chatInputText = text) }
    }

    fun sendChatMessage() {
        val prompt = _state.value.chatInputText.trim()
        if (prompt.isBlank()) return
        startGeneration(userFacingPrompt = prompt, actionType = "chat")
    }

    fun continueScene() {
        startGeneration(userFacingPrompt = "Continue scene", actionType = "continue")
    }

    fun abortGeneration() {
        if (activeGenerationId == 0) return
        val generationId = activeGenerationId
        cleanupStreamingAssistant(
            generationId = generationId,
            errorMessage = null,
        )
        activeGenerationJob?.cancel(CancellationException("User aborted generation"))
    }

    fun clear() {
        scope.cancel()
    }

    private fun startGeneration(
        userFacingPrompt: String,
        actionType: String,
    ) {
        if (activeGenerationJob != null) return

        val generationId = ++nextGenerationId
        val userMessageId = "generation-$generationId-user"
        val assistantMessageId = "generation-$generationId-assistant"
        val manuscriptExcerpt = _state.value.draftText
        activeGenerationId = generationId
        activeAssistantMessageId = assistantMessageId

        _state.update { current ->
            current.copy(
                chatInputText = "",
                errorMessage = null,
                streamingStatus = WorkshopStreamingStatus.Streaming,
                messages = current.messages + listOf(
                    WorkshopChatMessage.user(
                        id = userMessageId,
                        text = userFacingPrompt,
                    ),
                    WorkshopChatMessage.assistant(
                        id = assistantMessageId,
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "",
                            phase = WorkshopAssistantPhase.Streaming,
                        ),
                        isStreaming = true,
                    ),
                ),
            )
        }
        persistCurrentState()

        val generationJob = scope.launch(start = CoroutineStart.LAZY) {
            try {
                inferenceEngine.streamGenerate(
                    GenerationRequest(
                        projectId = "workshop-project",
                        templateId = templateId,
                        actionType = actionType,
                        userPrompt = userFacingPrompt,
                        manuscriptExcerpt = manuscriptExcerpt,
                        promptBlocks = templatePromptBlocks,
                    )
                ).collect { event ->
                    when (event) {
                        is GenerationEvent.Token -> appendAssistantToken(assistantMessageId, event.text)
                        is GenerationEvent.Final -> finalizeAssistantTurn(
                            generationId = generationId,
                            assistantMessageId = assistantMessageId,
                            finalText = event.text,
                        )
                        is GenerationEvent.Error -> cleanupStreamingAssistant(
                            generationId = generationId,
                            errorMessage = event.message,
                        )
                    }
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException && throwable.message == "User aborted generation") {
                    return@launch
                }
                if (activeGenerationId == generationId) {
                    cleanupStreamingAssistant(
                        generationId = generationId,
                        errorMessage = throwable.message ?: "Generation failed.",
                    )
                }
            } finally {
                activeGenerationJob = null
                activeGenerationId = 0
                activeAssistantMessageId = null
                _state.update { current ->
                    current.copy(
                        streamingStatus = WorkshopStreamingStatus.Idle,
                    )
                }
            }
        }
        activeGenerationJob = generationJob
        generationJob.start()
    }

    private fun persistCurrentState() {
        val snapshot = _state.value
        scope.launch {
            persistMutex.withLock {
                persistState(snapshot)
            }
        }
    }

    private fun appendAssistantToken(
        assistantMessageId: String,
        token: String,
    ) {
        if (activeAssistantMessageId != assistantMessageId) return
        _state.update {
            it.copy(
                messages = it.messages.map { message ->
                    if (message.id == assistantMessageId && message.isStreaming) {
                        val assistant = message.assistant ?: return@map message
                        message.withAssistant(
                            assistant.copy(
                                renderedMarkdown = assistant.renderedMarkdown + token,
                                phase = WorkshopAssistantPhase.Streaming,
                                failureMessage = null,
                            )
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun finalizeAssistantTurn(
        generationId: Int,
        assistantMessageId: String,
        finalText: String,
    ) {
        if (activeGenerationId != generationId) return
        if (activeAssistantMessageId != assistantMessageId) return
        if (!_state.value.isStreamingAssistant(assistantMessageId)) return

        _state.update {
            it.copy(
                messages = it.messages.map { message ->
                    if (message.id == assistantMessageId) {
                        val assistant = message.assistant ?: return@map message
                        message.withAssistant(
                            assistant.copy(
                                renderedMarkdown = finalText,
                                phase = WorkshopAssistantPhase.Completed,
                                failureMessage = null,
                            )
                        )
                    } else {
                        message
                    }
                },
                streamingStatus = WorkshopStreamingStatus.Recovering,
            )
        }
        persistCurrentState()
    }

    private fun cleanupStreamingAssistant(
        generationId: Int,
        errorMessage: String?,
    ) {
        if (activeGenerationId != generationId) return
        if (errorMessage != null && !_state.value.isStreamingAssistant(activeAssistantMessageId)) return

        val assistantMessageId = activeAssistantMessageId
        _state.update {
            it.copy(
                messages = it.messages.filterNot { message ->
                    message.id == assistantMessageId && message.isStreaming
                },
                streamingStatus = WorkshopStreamingStatus.Recovering,
                errorMessage = errorMessage,
            )
        }
        persistCurrentState()
    }
}

private fun defaultWorkshopScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun WorkshopUiState.restoredGenerationIdFloor(): Int =
    messages.maxOfOrNull { message -> message.id.generationIdOrNull() ?: 0 } ?: 0

private fun String.generationIdOrNull(): Int? {
    val match = GenerationMessageIdPattern.matchEntire(this) ?: return null
    return match.groupValues[1].toIntOrNull()
}

private val GenerationMessageIdPattern = Regex("""generation-(\d+)-(?:user|assistant)""")

private fun WorkshopChatMessage.withAssistant(assistant: WorkshopAssistantTurn): WorkshopChatMessage =
    copy(
        text = assistant.renderedMarkdown,
        assistant = assistant,
        isStreaming = assistant.phase == WorkshopAssistantPhase.Streaming,
    )

private fun WorkshopUiState.isStreamingAssistant(messageId: String?): Boolean {
    if (messageId == null) return false
    return messages.any { message ->
        message.id == messageId &&
            message.role == WorkshopMessageRole.Assistant &&
            message.assistant?.phase == WorkshopAssistantPhase.Streaming &&
            message.isStreaming
    }
}
