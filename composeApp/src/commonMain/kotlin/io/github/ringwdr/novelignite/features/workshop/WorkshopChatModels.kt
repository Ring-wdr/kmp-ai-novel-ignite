package io.github.ringwdr.novelignite.features.workshop

import kotlinx.serialization.Serializable

@Serializable
enum class WorkshopMessageRole {
    User,
    Assistant,
}

@Serializable
enum class WorkshopStreamingStatus {
    Idle,
    Streaming,
    Recovering,
}

@Serializable
data class WorkshopChatMessage(
    val id: String,
    val role: WorkshopMessageRole,
    val text: String,
    val isStreaming: Boolean = false,
) {
    companion object {
        fun user(id: String, text: String): WorkshopChatMessage =
            WorkshopChatMessage(
                id = id,
                role = WorkshopMessageRole.User,
                text = text,
            )

        fun assistant(id: String, text: String, isStreaming: Boolean = false): WorkshopChatMessage =
            WorkshopChatMessage(
                id = id,
                role = WorkshopMessageRole.Assistant,
                text = text,
                isStreaming = isStreaming,
            )
    }
}

@Serializable
data class WorkshopPersistedMessage(
    val id: String,
    val role: WorkshopMessageRole,
    val text: String,
)
