package io.github.ringwdr.novelignite.features.workshop

import kotlinx.serialization.Serializable

@Serializable
data class WorkshopStateSnapshot(
    val draftText: String,
    val messages: List<WorkshopPersistedMessage>,
    val errorMessage: String? = null,
) {
    fun toUiState(): WorkshopUiState = WorkshopUiState(
        draftText = draftText,
        chatInputText = "",
        messages = messages.toUiMessages(),
        streamingStatus = WorkshopStreamingStatus.Idle,
        errorMessage = null,
    )

    companion object {
        fun from(state: WorkshopUiState): WorkshopStateSnapshot = WorkshopStateSnapshot(
            draftText = state.draftText,
            messages = state.messages
                .filterNot { it.isStreaming }
                .map { it.toPersistedMessage() },
            errorMessage = null,
        )
    }
}

private fun WorkshopChatMessage.toPersistedMessage(): WorkshopPersistedMessage = WorkshopPersistedMessage(
    id = id,
    role = role,
    text = text,
)

private fun List<WorkshopPersistedMessage>.toUiMessages(): List<WorkshopChatMessage> {
    val seenIds = mutableSetOf<String>()
    return mapIndexed { index, message ->
        val normalizedId = if (seenIds.add(message.id)) {
            message.id
        } else {
            "restored-message-${index + 1}-${message.role.name.lowercase()}"
        }
        message.toUiMessage(id = normalizedId)
    }
}

private fun WorkshopPersistedMessage.toUiMessage(): WorkshopChatMessage =
    if (role == WorkshopMessageRole.Assistant) {
        WorkshopChatMessage.assistant(
            id = id,
            assistant = WorkshopAssistantTurn(
                renderedMarkdown = text,
                phase = WorkshopAssistantPhase.Completed,
            ),
            isStreaming = false,
        )
    } else {
        WorkshopChatMessage.user(id = id, text = text)
    }

private fun WorkshopPersistedMessage.toUiMessage(id: String): WorkshopChatMessage =
    if (role == WorkshopMessageRole.Assistant) {
        WorkshopChatMessage.assistant(
            id = id,
            assistant = WorkshopAssistantTurn(
                renderedMarkdown = text,
                phase = WorkshopAssistantPhase.Completed,
            ),
            isStreaming = false,
        )
    } else {
        WorkshopChatMessage.user(id = id, text = text)
    }
