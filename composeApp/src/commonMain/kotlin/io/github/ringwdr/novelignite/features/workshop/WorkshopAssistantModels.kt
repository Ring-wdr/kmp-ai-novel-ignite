package io.github.ringwdr.novelignite.features.workshop

import kotlinx.serialization.Serializable

@Serializable
enum class WorkshopAssistantPhase {
    Streaming,
    Completed,
    Failed,
}

@Serializable
enum class WorkshopChoiceStyle {
    Primary,
    Secondary,
}

@Serializable
data class WorkshopChoice(
    val id: String,
    val label: String,
    val prompt: String,
    val style: WorkshopChoiceStyle = WorkshopChoiceStyle.Secondary,
)

@Serializable
data class WorkshopAssistantMetadata(
    val title: String? = null,
    val badge: String? = null,
)

@Serializable
data class WorkshopAssistantTurn(
    val renderedMarkdown: String = "",
    val choices: List<WorkshopChoice> = emptyList(),
    val metadata: WorkshopAssistantMetadata = WorkshopAssistantMetadata(),
    val phase: WorkshopAssistantPhase = WorkshopAssistantPhase.Streaming,
    val failureMessage: String? = null,
)
