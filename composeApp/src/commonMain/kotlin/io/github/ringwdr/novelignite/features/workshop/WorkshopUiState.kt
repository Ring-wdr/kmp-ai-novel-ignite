package io.github.ringwdr.novelignite.features.workshop

data class WorkshopUiState(
    val draftText: String = "",
    val chatInputText: String = "",
    val messages: List<WorkshopChatMessage> = emptyList(),
    val streamingStatus: WorkshopStreamingStatus = WorkshopStreamingStatus.Idle,
    val errorMessage: String? = null,
)

val WorkshopUiState.isGenerating: Boolean
    get() = streamingStatus == WorkshopStreamingStatus.Streaming

val WorkshopUiState.latestAssistantText: String
    get() = messages.lastOrNull { it.role == WorkshopMessageRole.Assistant && !it.isStreaming }?.text.orEmpty()
