package io.github.ringwdr.novelignite.features.workshop

object WorkshopAssistantStreamReducer {
    fun apply(
        state: WorkshopUiState,
        event: WorkshopAssistantStreamEvent,
    ): WorkshopUiState = when (event) {
        is WorkshopAssistantStreamEvent.Start -> state.copy(
            streamingStatus = WorkshopStreamingStatus.Streaming,
            errorMessage = null,
            messages = state.messages + WorkshopChatMessage(
                id = event.messageId,
                role = WorkshopMessageRole.Assistant,
                assistant = WorkshopAssistantTurn(
                    phase = WorkshopAssistantPhase.Streaming,
                ),
                isStreaming = true,
            ),
        )

        is WorkshopAssistantStreamEvent.MarkdownDelta -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                renderedMarkdown = current.renderedMarkdown + event.markdown,
                phase = WorkshopAssistantPhase.Streaming,
                failureMessage = null,
            )
        }

        is WorkshopAssistantStreamEvent.ChoicesReplace -> state.updateAssistant(event.messageId) { current ->
            current.copy(choices = event.choices)
        }

        is WorkshopAssistantStreamEvent.MetadataPatch -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                metadata = current.metadata.copy(
                    title = event.title ?: current.metadata.title,
                    badge = event.badge ?: current.metadata.badge,
                ),
            )
        }

        is WorkshopAssistantStreamEvent.Complete -> state.updateAssistant(event.messageId) { current ->
            current.copy(
                phase = WorkshopAssistantPhase.Completed,
                failureMessage = null,
            )
        }.copy(
            streamingStatus = WorkshopStreamingStatus.Idle,
        )

        is WorkshopAssistantStreamEvent.AbortAck -> state.copy(
            messages = state.messages.filterNot { message ->
                message.id == event.messageId && message.role == WorkshopMessageRole.Assistant
            },
            streamingStatus = WorkshopStreamingStatus.Idle,
        )

        is WorkshopAssistantStreamEvent.Error -> state.copy(
            messages = state.messages.filterNot { message ->
                message.id == event.messageId && message.role == WorkshopMessageRole.Assistant
            },
            streamingStatus = WorkshopStreamingStatus.Idle,
            errorMessage = event.message,
        )
    }
}

private inline fun WorkshopUiState.updateAssistant(
    messageId: String,
    transform: (WorkshopAssistantTurn) -> WorkshopAssistantTurn,
): WorkshopUiState {
    var updated = false
    val updatedMessages = messages.map { message ->
        if (message.id != messageId || message.role != WorkshopMessageRole.Assistant) {
            message
        } else {
            updated = true
            message.withAssistant(transform(message.assistant ?: WorkshopAssistantTurn()))
        }
    }
    return if (updated) copy(messages = updatedMessages) else this
}

private fun WorkshopChatMessage.withAssistant(assistant: WorkshopAssistantTurn): WorkshopChatMessage =
    copy(
        text = assistant.renderedMarkdown,
        assistant = assistant,
        isStreaming = assistant.phase == WorkshopAssistantPhase.Streaming,
    )
