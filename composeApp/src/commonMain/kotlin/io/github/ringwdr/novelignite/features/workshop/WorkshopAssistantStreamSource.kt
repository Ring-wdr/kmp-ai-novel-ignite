package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import java.lang.StringBuilder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

interface WorkshopAssistantStreamSource {
    fun stream(
        request: GenerationRequest,
        generationId: Int,
    ): Flow<WorkshopAssistantStreamEvent>
}

class DefaultWorkshopAssistantStreamSource(
    private val inferenceEngine: InferenceEngine,
    private val choiceBuilder: WorkshopChoiceBuilder = WorkshopChoiceBuilder(),
) : WorkshopAssistantStreamSource {
    override fun stream(
        request: GenerationRequest,
        generationId: Int,
    ): Flow<WorkshopAssistantStreamEvent> = flow {
        val messageId = workshopGenerationAssistantMessageId(generationId)
        emit(
            WorkshopAssistantStreamEvent.Start(
                requestId = workshopGenerationRequestId(generationId),
                messageId = messageId,
            )
        )

        val streamedMarkdown = StringBuilder()
        inferenceEngine.streamGenerate(request).collect { event ->
            when (event) {
                is GenerationEvent.Token -> {
                    streamedMarkdown.append(event.text)
                    emit(
                        WorkshopAssistantStreamEvent.MarkdownDelta(
                            messageId = messageId,
                            markdown = event.text,
                        )
                    )
                }

                is GenerationEvent.Final -> {
                    val finalMarkdown = event.text.ifBlank { streamedMarkdown.toString() }
                    if (streamedMarkdown.isEmpty() && finalMarkdown.isNotBlank()) {
                        emit(
                            WorkshopAssistantStreamEvent.MarkdownDelta(
                                messageId = messageId,
                                markdown = finalMarkdown,
                            )
                        )
                    }
                    emit(
                        WorkshopAssistantStreamEvent.ChoicesReplace(
                            messageId = messageId,
                            choices = choiceBuilder.build(finalMarkdown),
                        )
                    )
                    emit(WorkshopAssistantStreamEvent.Complete(messageId = messageId))
                }

                is GenerationEvent.Error -> emit(
                    WorkshopAssistantStreamEvent.Error(
                        messageId = messageId,
                        message = event.message,
                    )
                )
            }
        }
    }
}

class FixtureWorkshopAssistantStreamSource(
    private val choiceBuilder: WorkshopChoiceBuilder = WorkshopChoiceBuilder(),
) : WorkshopAssistantStreamSource {
    override fun stream(
        request: GenerationRequest,
        generationId: Int,
    ): Flow<WorkshopAssistantStreamEvent> = flow {
        val messageId = workshopGenerationAssistantMessageId(generationId)
        val finalMarkdown = buildString {
            appendLine("## Fixture turn")
            appendLine()
            appendLine("Template `${request.templateId}` keeps the story moving.")
            if (request.userPrompt.isNotBlank()) {
                appendLine()
                appendLine("Prompt: ${request.userPrompt}")
            }
            if (request.manuscriptExcerpt.isNotBlank()) {
                appendLine()
                appendLine("Excerpt: ${request.manuscriptExcerpt.trim().take(120)}")
            }
        }.trimEnd()

        emit(
            WorkshopAssistantStreamEvent.Start(
                requestId = workshopGenerationRequestId(generationId),
                messageId = messageId,
            )
        )
        emit(
            WorkshopAssistantStreamEvent.MarkdownDelta(
                messageId = messageId,
                markdown = "## Fixture turn\n\n",
            )
        )
        emit(
            WorkshopAssistantStreamEvent.MarkdownDelta(
                messageId = messageId,
                markdown = "Template `${request.templateId}` keeps the story moving.",
            )
        )
        if (request.userPrompt.isNotBlank()) {
            emit(
                WorkshopAssistantStreamEvent.MarkdownDelta(
                    messageId = messageId,
                    markdown = "\n\nPrompt: ${request.userPrompt}",
                )
            )
        }
        if (request.manuscriptExcerpt.isNotBlank()) {
            emit(
                WorkshopAssistantStreamEvent.MarkdownDelta(
                    messageId = messageId,
                    markdown = "\n\nExcerpt: ${request.manuscriptExcerpt.trim().take(120)}",
                )
            )
        }
        emit(
            WorkshopAssistantStreamEvent.ChoicesReplace(
                messageId = messageId,
                choices = choiceBuilder.build(finalMarkdown),
            )
        )
        emit(WorkshopAssistantStreamEvent.Complete(messageId = messageId))
    }
}

internal fun workshopGenerationRequestId(generationId: Int): String = "request-$generationId"

internal fun workshopGenerationUserMessageId(generationId: Int): String = "generation-$generationId-user"

internal fun workshopGenerationAssistantMessageId(generationId: Int): String = "generation-$generationId-assistant"
