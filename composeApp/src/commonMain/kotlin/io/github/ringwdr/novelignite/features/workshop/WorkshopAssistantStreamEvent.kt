package io.github.ringwdr.novelignite.features.workshop

import kotlinx.serialization.Serializable

@Serializable
sealed interface WorkshopAssistantStreamEvent {
    val messageId: String

    @Serializable
    data class Start(
        val requestId: String,
        override val messageId: String,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class MarkdownDelta(
        override val messageId: String,
        val markdown: String,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class ChoicesReplace(
        override val messageId: String,
        val choices: List<WorkshopChoice>,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class MetadataPatch(
        override val messageId: String,
        val title: String? = null,
        val badge: String? = null,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class Complete(
        override val messageId: String,
        val finalMarkdown: String? = null,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class AbortAck(
        override val messageId: String,
    ) : WorkshopAssistantStreamEvent

    @Serializable
    data class Error(
        override val messageId: String,
        val message: String,
    ) : WorkshopAssistantStreamEvent
}
