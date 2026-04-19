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
    private val scope: CoroutineScope = defaultWorkshopScope(),
    private val persistenceScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
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
        persistenceConsumerJob = persistenceScope.launch {
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
        val generationId = activeGenerationId
        cleanupStreamingAssistant(
            generationId = generationId,
            assistantMessageId = activeAssistantMessageId,
            errorMessage = null,
        )
        activeGenerationJob?.cancel(CancellationException("User aborted generation"))
    }

    fun clear() {
        scope.cancel()
        persistQueue.close()
        runBlocking {
            persistenceConsumerJob.join()
        }
        persistenceScope.cancel()
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
                    cleanupStreamingAssistant(
                        generationId = generationId,
                        assistantMessageId = activeAssistantMessageId,
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
        when (event) {
            is WorkshopAssistantStreamEvent.Start -> ensureStreamingAssistant(event.messageId)
            is WorkshopAssistantStreamEvent.MarkdownDelta -> appendAssistantToken(
                assistantMessageId = event.messageId,
                token = event.markdown,
            )
            is WorkshopAssistantStreamEvent.ChoicesReplace -> updateAssistantChoices(
                assistantMessageId = event.messageId,
                choices = event.choices,
            )
            is WorkshopAssistantStreamEvent.MetadataPatch -> updateAssistantMetadata(
                assistantMessageId = event.messageId,
                title = event.title,
                badge = event.badge,
            )
            is WorkshopAssistantStreamEvent.Complete -> finalizeAssistantTurn(
                generationId = generationId,
                assistantMessageId = event.messageId,
                finalMarkdown = event.finalMarkdown,
            )
            is WorkshopAssistantStreamEvent.AbortAck -> cleanupStreamingAssistant(
                generationId = generationId,
                assistantMessageId = event.messageId,
                errorMessage = null,
            )
            is WorkshopAssistantStreamEvent.Error -> cleanupStreamingAssistant(
                generationId = generationId,
                assistantMessageId = event.messageId,
                errorMessage = event.message,
            )
        }
    }

    private fun ensureStreamingAssistant(messageId: String) {
        if (activeAssistantMessageId != messageId) return
        if (_state.value.messages.any { message -> message.id == messageId }) return

        _state.update {
            it.copy(
                messages = it.messages + WorkshopChatMessage.assistant(
                    id = messageId,
                    assistant = WorkshopAssistantTurn(
                        renderedMarkdown = "",
                        phase = WorkshopAssistantPhase.Streaming,
                    ),
                    isStreaming = true,
                ),
            )
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

    private fun updateAssistantChoices(
        assistantMessageId: String,
        choices: List<WorkshopChoice>,
    ) {
        if (activeAssistantMessageId != assistantMessageId) return
        _state.update {
            it.copy(
                messages = it.messages.map { message ->
                    if (message.id == assistantMessageId && message.isStreaming) {
                        val assistant = message.assistant ?: return@map message
                        message.withAssistant(
                            assistant.copy(
                                choices = choices,
                            )
                        )
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun updateAssistantMetadata(
        assistantMessageId: String,
        title: String?,
        badge: String?,
    ) {
        if (activeAssistantMessageId != assistantMessageId) return
        _state.update {
            it.copy(
                messages = it.messages.map { message ->
                    if (message.id == assistantMessageId && message.isStreaming) {
                        val assistant = message.assistant ?: return@map message
                        message.withAssistant(
                            assistant.copy(
                                metadata = assistant.metadata.copy(
                                    title = title ?: assistant.metadata.title,
                                    badge = badge ?: assistant.metadata.badge,
                                ),
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
        finalMarkdown: String?,
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
                                renderedMarkdown = finalMarkdown ?: assistant.renderedMarkdown,
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
        assistantMessageId: String?,
        errorMessage: String?,
    ) {
        if (activeGenerationId != generationId) return
        if (activeAssistantMessageId != assistantMessageId) return
        if (errorMessage != null && !_state.value.isStreamingAssistant(assistantMessageId)) return

        _state.update {
            it.copy(
                messages = it.messages.filterNot { message ->
                    message.id == assistantMessageId && message.isStreaming
                },
                streamingStatus = WorkshopStreamingStatus.Recovering,
                errorMessage = errorMessage ?: persistenceErrorMessage,
            )
        }
        persistCurrentState()
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
