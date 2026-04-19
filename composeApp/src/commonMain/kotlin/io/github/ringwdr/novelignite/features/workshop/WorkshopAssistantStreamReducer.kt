package io.github.ringwdr.novelignite.features.workshop

object WorkshopAssistantStreamReducer {
    fun apply(
        state: WorkshopUiState,
        event: WorkshopAssistantStreamEvent,
    ): WorkshopUiState = when (event) {
        is WorkshopAssistantStreamEvent.Start -> {
            val hasActiveAssistant = state.messages.any { message ->
                message.role == WorkshopMessageRole.Assistant && message.isStreaming
            }
            val hasMessageId = state.messages.any { message -> message.id == event.messageId }
            if (hasActiveAssistant || hasMessageId) {
                state
            } else {
                state.copy(
                    streamingStatus = WorkshopStreamingStatus.Streaming,
                    errorMessage = null,
                    messages = state.messages + WorkshopChatMessage.assistant(
                        id = event.messageId,
                        assistant = WorkshopAssistantTurn(
                            phase = WorkshopAssistantPhase.Streaming,
                        ),
                        isStreaming = true,
                    ),
                )
            }
        }

        is WorkshopAssistantStreamEvent.MarkdownDelta -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                renderedMarkdown = current.renderedMarkdown + event.markdown,
                phase = WorkshopAssistantPhase.Streaming,
                failureMessage = null,
            )
        } ?: state

        is WorkshopAssistantStreamEvent.ChoicesReplace -> state.updateAssistant(event.messageId) { current ->
            current.copy(choices = event.choices)
        } ?: state

        is WorkshopAssistantStreamEvent.MetadataPatch -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                metadata = current.metadata.copy(
                    title = event.title ?: current.metadata.title,
                    badge = event.badge ?: current.metadata.badge,
                ),
            )
        } ?: state

        is WorkshopAssistantStreamEvent.Complete -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                renderedMarkdown = event.finalMarkdown ?: current.renderedMarkdown,
                phase = WorkshopAssistantPhase.Completed,
                failureMessage = null,
            )
        }?.copy(
            streamingStatus = WorkshopStreamingStatus.Idle,
        ) ?: state

        is WorkshopAssistantStreamEvent.AbortAck -> state.removeStreamingAssistant(event.messageId)?.copy(
            streamingStatus = WorkshopStreamingStatus.Idle,
        ) ?: state

        is WorkshopAssistantStreamEvent.Error -> state.removeStreamingAssistant(event.messageId)?.copy(
            streamingStatus = WorkshopStreamingStatus.Idle,
            errorMessage = event.message,
        ) ?: state
    }
}

private inline fun WorkshopUiState.updateAssistant(
    messageId: String,
    transform: (WorkshopAssistantTurn) -> WorkshopAssistantTurn,
): WorkshopUiState? {
    val index = messages.indexOfFirst { message ->
        message.id == messageId &&
            message.role == WorkshopMessageRole.Assistant &&
            message.assistant?.phase == WorkshopAssistantPhase.Streaming
    }
    if (index < 0) return null

    val current = messages[index]
    val assistant = current.assistant ?: return null
    val updatedMessages = messages.toMutableList()
    updatedMessages[index] = current.withAssistant(transform(assistant))
    return copy(messages = updatedMessages)
}

private fun WorkshopChatMessage.withAssistant(assistant: WorkshopAssistantTurn): WorkshopChatMessage =
    copy(
        text = assistant.renderedMarkdown,
        assistant = assistant,
        isStreaming = assistant.phase == WorkshopAssistantPhase.Streaming,
    )

private fun WorkshopUiState.removeStreamingAssistant(messageId: String): WorkshopUiState? {
    val index = messages.indexOfFirst { message ->
        message.id == messageId &&
            message.role == WorkshopMessageRole.Assistant &&
            message.assistant?.phase == WorkshopAssistantPhase.Streaming
    }
    if (index < 0) return null

    val updatedMessages = messages.toMutableList()
    updatedMessages.removeAt(index)
    return copy(messages = updatedMessages)
}
