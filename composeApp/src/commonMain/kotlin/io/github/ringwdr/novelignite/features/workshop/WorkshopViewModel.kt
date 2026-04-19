package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class WorkshopViewModel(
    private val streamSource: WorkshopAssistantStreamSource,
    private val templateId: String = "workshop-default-template",
    private val templatePromptBlocks: List<String> = emptyList(),
    initialState: WorkshopUiState = WorkshopUiState(),
    private val persistState: suspend (WorkshopUiState) -> Unit = {},
    scope: CoroutineScope? = null,
    persistenceScope: CoroutineScope? = null,
) {
    private val activeScope = scope ?: defaultWorkshopScope()
    private val activePersistenceScope = persistenceScope ?: CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(initialState)
    private var nextGenerationId = initialState.restoredGenerationIdFloor()
    private var activeGenerationId = 0
    private var activeAssistantMessageId: String? = null
    private var activeGenerationJob: Job? = null
    private var persistenceErrorMessage: String? = null
    private val persistQueue = Channel<WorkshopUiState>(capacity = Channel.CONFLATED)
    private val persistenceConsumerJob: Job
    val state: StateFlow<WorkshopUiState> = _state

    init {
        persistenceConsumerJob = activePersistenceScope.launch {
            for (snapshot in persistQueue) {
                try {
                    persistState(snapshot)
                    clearPersistenceFailure()
                } catch (_: Throwable) {
                    surfacePersistenceFailure()
                }
            }
        }
    }

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

    fun useChoice(prompt: String) {
        val choicePrompt = prompt.trim()
        if (choicePrompt.isBlank()) return
        startGeneration(userFacingPrompt = choicePrompt, actionType = "chat")
    }

    fun continueScene() {
        startGeneration(userFacingPrompt = "Continue scene", actionType = "continue")
    }

    fun abortGeneration() {
        if (activeGenerationId == 0) return
        activeAssistantMessageId?.let { assistantMessageId ->
            applyAssistantStreamEvent(
                event = WorkshopAssistantStreamEvent.AbortAck(messageId = assistantMessageId),
                persistAfter = true,
            )
        }
        activeGenerationJob?.cancel(CancellationException("User aborted generation"))
    }

    fun clear() {
        activeScope.cancel()
        persistQueue.close()
        runBlocking {
            persistenceConsumerJob.join()
        }
        activePersistenceScope.cancel()
    }

    private fun startGeneration(
        userFacingPrompt: String,
        actionType: String,
    ) {
        if (activeGenerationJob != null) return

        val generationId = ++nextGenerationId
        val userMessageId = workshopGenerationUserMessageId(generationId)
        val assistantMessageId = workshopGenerationAssistantMessageId(generationId)
        val manuscriptExcerpt = _state.value.draftText
        activeGenerationId = generationId
        activeAssistantMessageId = assistantMessageId

        _state.update { current ->
            current.copy(
                chatInputText = "",
                errorMessage = persistenceErrorMessage,
                streamingStatus = WorkshopStreamingStatus.Streaming,
                messages = current.messages + WorkshopChatMessage.user(
                    id = userMessageId,
                    text = userFacingPrompt,
                ),
            )
        }
        persistCurrentState()

        val generationJob = activeScope.launch(start = CoroutineStart.LAZY) {
            try {
                streamSource.stream(
                    request = GenerationRequest(
                        projectId = "workshop-project",
                        templateId = templateId,
                        actionType = actionType,
                        userPrompt = userFacingPrompt,
                        manuscriptExcerpt = manuscriptExcerpt,
                        promptBlocks = templatePromptBlocks,
                    ),
                    generationId = generationId,
                ).collect { event ->
                    handleAssistantStreamEvent(
                        generationId = generationId,
                        event = event,
                    )
                }
            } catch (throwable: Throwable) {
                if (throwable is CancellationException && throwable.message == "User aborted generation") {
                    return@launch
                }
                if (activeGenerationId == generationId) {
                    activeAssistantMessageId?.let { assistantMessageId ->
                        applyAssistantStreamEvent(
                            event = WorkshopAssistantStreamEvent.Error(
                                messageId = assistantMessageId,
                                message = throwable.message ?: "Generation failed.",
                            ),
                            persistAfter = true,
                        )
                    }
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
        persistQueue.trySend(_state.value)
    }

    private fun surfacePersistenceFailure() {
        persistenceErrorMessage = PersistenceFailureMessage
        _state.update { current ->
            current.copy(errorMessage = PersistenceFailureMessage)
        }
    }

    private fun clearPersistenceFailure() {
        val previousMessage = persistenceErrorMessage ?: return
        persistenceErrorMessage = null
        _state.update { current ->
            if (current.errorMessage == previousMessage) {
                current.copy(errorMessage = null)
            } else {
                current
            }
        }
    }

    private fun handleAssistantStreamEvent(
        generationId: Int,
        event: WorkshopAssistantStreamEvent,
    ) {
        if (activeGenerationId != generationId) return

        when (event) {
            is WorkshopAssistantStreamEvent.Start -> {
                if (event.messageId != activeAssistantMessageId) return
                applyAssistantStreamEvent(event)
            }

            is WorkshopAssistantStreamEvent.MarkdownDelta,
            is WorkshopAssistantStreamEvent.ChoicesReplace,
            is WorkshopAssistantStreamEvent.MetadataPatch -> applyAssistantStreamEvent(event)

            is WorkshopAssistantStreamEvent.Complete,
            is WorkshopAssistantStreamEvent.AbortAck,
            is WorkshopAssistantStreamEvent.Error -> applyAssistantStreamEvent(
                event = event,
                persistAfter = true,
            )
        }
    }

    private fun applyAssistantStreamEvent(
        event: WorkshopAssistantStreamEvent,
        persistAfter: Boolean = false,
    ) {
        var changed = false
        _state.update { current ->
            val reduced = WorkshopAssistantStreamReducer.apply(current, event)
            if (reduced == current) {
                current
            } else {
                changed = true
                when (event) {
                    is WorkshopAssistantStreamEvent.Start -> reduced.copy(
                        errorMessage = persistenceErrorMessage,
                    )

                    is WorkshopAssistantStreamEvent.Complete,
                    is WorkshopAssistantStreamEvent.AbortAck,
                    is WorkshopAssistantStreamEvent.Error -> reduced.copy(
                        streamingStatus = WorkshopStreamingStatus.Recovering,
                    )

                    else -> reduced
                }
            }
        }
        if (persistAfter && changed) {
            persistCurrentState()
        }
    }
}

private const val PersistenceFailureMessage = "Failed to save workshop changes."

private fun defaultWorkshopScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

private fun WorkshopUiState.restoredGenerationIdFloor(): Int =
    messages.maxOfOrNull { message -> message.id.generationIdOrNull() ?: 0 } ?: 0

private fun String.generationIdOrNull(): Int? {
    val match = GenerationMessageIdPattern.matchEntire(this) ?: return null
    return match.groupValues[1].toIntOrNull()
}

private val GenerationMessageIdPattern = Regex("""generation-(\d+)-(?:user|assistant)""")
