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
                .filter { message ->
                    message.role != WorkshopMessageRole.Assistant ||
                        message.assistant?.phase == WorkshopAssistantPhase.Completed
                }
                .map { it.toPersistedMessage() },
            errorMessage = null,
        )
    }
}

private fun WorkshopChatMessage.toPersistedMessage(): WorkshopPersistedMessage = WorkshopPersistedMessage(
    id = id,
    role = role,
    text = assistant?.renderedMarkdown ?: text,
    assistant = assistant?.takeIf { it.phase == WorkshopAssistantPhase.Completed },
)

private fun List<WorkshopPersistedMessage>.toUiMessages(): List<WorkshopChatMessage> {
    val seenIds = mutableSetOf<String>()
    return mapIndexed { index, message ->
        val normalizedId = if (seenIds.add(message.id)) {
            message.id
        } else {
            nextUniqueRestoredId(
                index = index,
                role = message.role,
                seenIds = seenIds,
            )
        }
        message.toUiMessage(id = normalizedId)
    }
}

private fun nextUniqueRestoredId(
    index: Int,
    role: WorkshopMessageRole,
    seenIds: MutableSet<String>,
): String {
    val baseId = "restored-message-${index + 1}-${role.name.lowercase()}"
    var candidate = baseId
    var attempt = 2
    while (!seenIds.add(candidate)) {
        candidate = "$baseId-$attempt"
        attempt += 1
    }
    return candidate
}

private fun WorkshopPersistedMessage.toUiMessage(id: String): WorkshopChatMessage =
    if (role == WorkshopMessageRole.Assistant) {
        val restoredAssistant = (assistant ?: WorkshopAssistantTurn(
            renderedMarkdown = text,
            phase = WorkshopAssistantPhase.Completed,
        )).normalizedForRestore()
        WorkshopChatMessage.assistant(
            id = id,
            assistant = restoredAssistant,
        )
    } else {
        WorkshopChatMessage.user(id = id, text = text)
    }

private fun WorkshopAssistantTurn.normalizedForRestore(): WorkshopAssistantTurn =
    if (phase == WorkshopAssistantPhase.Completed) {
        this
    } else {
        copy(
            phase = WorkshopAssistantPhase.Completed,
            failureMessage = null,
        )
    }
