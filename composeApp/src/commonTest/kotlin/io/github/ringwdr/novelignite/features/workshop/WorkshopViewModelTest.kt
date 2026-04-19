package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.CoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopViewModelTest {
    @Test
    fun sendChatMessage_keepsTemplatePromptBlocksSeparate_andUsesResolvedTemplateId() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { request, generationId ->
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), workshopGenerationAssistantMessageId(generationId)))
                emit(WorkshopAssistantStreamEvent.Complete(workshopGenerationAssistantMessageId(generationId)))
            },
            templateId = "42",
            templatePromptBlocks = listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.updateChatInput("Make the reply more ominous.")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals("42", requests.single().templateId)
        assertEquals("Make the reply more ominous.", requests.single().userPrompt)
        assertEquals(
            listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
            requests.single().promptBlocks,
        )
    }

    @Test
    fun continueScene_usesTemplatePromptBlocks_withoutAppendingContinueLabel() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { request, generationId ->
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), workshopGenerationAssistantMessageId(generationId)))
                emit(WorkshopAssistantStreamEvent.Complete(workshopGenerationAssistantMessageId(generationId)))
            },
            templateId = "42",
            templatePromptBlocks = listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals("42", requests.single().templateId)
        assertEquals("Continue scene", requests.single().userPrompt)
        assertEquals(
            listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
            requests.single().promptBlocks,
        )
    }

    @Test
    fun sendChatMessage_includesTypedPromptAndCapturesDraftAtStart() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { request, generationId ->
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), workshopGenerationAssistantMessageId(generationId)))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(workshopGenerationAssistantMessageId(generationId), "The gate opened."))
                emit(WorkshopAssistantStreamEvent.Complete(workshopGenerationAssistantMessageId(generationId)))
            },
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        viewModel.updateDraft("Changed after send")
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals("chat", requests.single().actionType)
        assertEquals("Continue the scene", requests.single().userPrompt)
        assertEquals("The gate opened.", requests.single().manuscriptExcerpt)
        assertEquals(emptyList(), requests.single().promptBlocks)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue the scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "The gate opened.",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun useChoice_includesTypedChoicePromptAndUsesChatActionType() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), workshopGenerationAssistantMessageId(generationId)))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(workshopGenerationAssistantMessageId(generationId), "Choice follow-up."))
                emit(WorkshopAssistantStreamEvent.Complete(workshopGenerationAssistantMessageId(generationId)))
            },
        )

        viewModel.useChoice("Continue the scene from the checkpoint.")
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals("chat", requests.single().actionType)
        assertEquals("Continue the scene from the checkpoint.", requests.single().userPrompt)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(
                    id = "generation-1-user",
                    text = "Continue the scene from the checkpoint.",
                ),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "Choice follow-up.",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
    }

    @Test
    fun sendChatMessage_usesFinalAuthoritativeMarkdownAfterTokens() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "## Gate"))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "\n\nThe wind answered."))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "\n\nA silver wind answered."))
                emit(WorkshopAssistantStreamEvent.ChoicesReplace(messageId, WorkshopChoiceBuilder().build("## Gate\n\nThe wind answered.\n\nA silver wind answered.")))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        val assistant = viewModel.state.value.messages.last().assistant!!
        assertEquals("## Gate\n\nThe wind answered.\n\nA silver wind answered.", assistant.renderedMarkdown)
        assertEquals(
            listOf("Continue scene", "Deepen tension", "Shift perspective"),
            assistant.choices.map { it.label },
        )
        assertEquals(WorkshopAssistantPhase.Completed, assistant.phase)
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
    }

    @Test
    fun sendChatMessage_blankFinalOutputRemovesAssistantAndSurfacesError() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(
                    WorkshopAssistantStreamEvent.Complete(
                        messageId = messageId,
                        finalMarkdown = "",
                    )
                )
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue the scene"),
            ),
            viewModel.state.value.messages,
        )
        assertEquals("Generation returned no content.", viewModel.state.value.errorMessage)
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
    }

    @Test
    fun sendChatMessage_replacesStreamedMarkdownWithAuthoritativeFinalMarkdown() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "The gate..."))
                emit(
                    WorkshopAssistantStreamEvent.Complete(
                        messageId = messageId,
                        finalMarkdown = "A gate opened",
                    )
                )
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        val assistant = viewModel.state.value.messages.last().assistant!!
        assertEquals("A gate opened", assistant.renderedMarkdown)
        assertEquals(WorkshopAssistantPhase.Completed, assistant.phase)
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun sendChatMessage_ignoresWrongMessageIdsFromTypedEvents() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta("generation-999-assistant", "wrong"))
                emit(WorkshopAssistantStreamEvent.ChoicesReplace("generation-999-assistant", listOf(
                    WorkshopChoice(id = "wrong", label = "Wrong", prompt = "Wrong"),
                )))
                emit(WorkshopAssistantStreamEvent.Error("generation-999-assistant", "wrong boom"))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "right"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        val assistant = viewModel.state.value.messages.last().assistant!!
        assertEquals("right", assistant.renderedMarkdown)
        assertEquals(emptyList(), assistant.choices)
        assertEquals(WorkshopAssistantPhase.Completed, assistant.phase)
        assertNull(viewModel.state.value.errorMessage)
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
    }

    @Test
    fun sendChatMessage_ignoresWrongStartMessageIds() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), "generation-999-assistant"))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "right"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(
            listOf("generation-1-user"),
            viewModel.state.value.messages.map { it.id },
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun wrongTerminalId_doesNotStrandStreamingAssistantWhenCollectorFinishes() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                if (requests.size == 1) {
                    emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                    emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "Draft"))
                    emit(WorkshopAssistantStreamEvent.Complete("generation-999-assistant"))
                } else {
                    emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                    emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "Recovered"))
                    emit(WorkshopAssistantStreamEvent.Complete(messageId))
                }
            },
        )

        viewModel.updateChatInput("First")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "First"),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)

        viewModel.continueScene()
        runCurrent()

        assertEquals(2, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "First"),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-2-assistant",
                    text = "Recovered",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
    }

    @Test
    fun abortGeneration_entersRecoveringUntilCollectorFinishes_thenAllowsRetry() = runTest {
        val release = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "The gate opened."))
                if (requests.size == 1) {
                    withContext(NonCancellable) {
                        release.receive()
                    }
                }
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
            },
        )

        viewModel.continueScene()
        runCurrent()
        viewModel.abortGeneration()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Recovering, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)

        viewModel.continueScene()
        runCurrent()
        assertEquals(1, requests.size)

        release.send(Unit)
        runCurrent()
        advanceUntilIdle()

        viewModel.continueScene()
        runCurrent()

        assertEquals(2, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-2-assistant",
                    text = "The gate opened.",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun generationError_entersRecoveringUntilCollectorFinishes_thenAllowsRetry() = runTest {
        val release = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "The gate opened."))
                if (requests.size == 1) {
                    emit(WorkshopAssistantStreamEvent.Error(messageId, "boom"))
                    withContext(NonCancellable) {
                        release.receive()
                    }
                } else {
                    emit(WorkshopAssistantStreamEvent.Complete(messageId))
                }
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(WorkshopStreamingStatus.Recovering, viewModel.state.value.streamingStatus)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue the scene"),
            ),
            viewModel.state.value.messages,
        )
        assertEquals("boom", viewModel.state.value.errorMessage)

        viewModel.continueScene()
        runCurrent()
        assertEquals(1, requests.size)

        release.send(Unit)
        runCurrent()
        advanceUntilIdle()

        viewModel.continueScene()
        runCurrent()

        assertEquals(2, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue the scene"),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-2-assistant",
                    text = "The gate opened.",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun generationCancellationException_isHandledLikeAnError() = runTest {
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "The gate opened."))
                throw CancellationException("boom")
            },
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue the scene"),
            ),
            viewModel.state.value.messages,
        )
        assertEquals("boom", viewModel.state.value.errorMessage)
    }

    @Test
    fun lateTerminalEvents_doNotOverwriteCompletedAssistantTurnOrSurfaceErrors() = runTest {
        val collectorFinished = Channel<Unit>()
        val release = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "done"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
                withContext(NonCancellable) {
                    release.receive()
                }
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
                emit(WorkshopAssistantStreamEvent.Error(messageId, "late-error"))
                collectorFinished.send(Unit)
            },
        )

        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(WorkshopStreamingStatus.Recovering, viewModel.state.value.streamingStatus)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "done",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )

        release.send(Unit)
        collectorFinished.receive()
        runCurrent()
        advanceUntilIdle()

        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "done",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun synchronousFinalGeneration_entersRecoveringUntilCollectorFinishes_thenAllowsRetry() = runTest {
        val release = Channel<Unit>()
        val collectorFinished = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "one"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
                if (requests.size == 1) {
                    withContext(NonCancellable) {
                        release.receive()
                    }
                    collectorFinished.send(Unit)
                }
            },
        )

        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(WorkshopStreamingStatus.Recovering, viewModel.state.value.streamingStatus)
        viewModel.continueScene()
        runCurrent()
        assertEquals(1, requests.size)

        release.send(Unit)
        collectorFinished.receive()
        runCurrent()
        advanceUntilIdle()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)

        viewModel.continueScene()
        runCurrent()

        assertEquals(2, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "one",
                    isStreaming = false,
                ),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-2-assistant",
                    text = "one",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun continueScene_staysRecoveringAndRejectsRetryUntilFinalCollectorFinishes() = runTest {
        val release = Channel<Unit>()
        val collectorFinished = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "one"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
                if (requests.size == 1) {
                    withContext(NonCancellable) {
                        release.receive()
                    }
                    collectorFinished.send(Unit)
                }
            },
        )

        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(WorkshopStreamingStatus.Recovering, viewModel.state.value.streamingStatus)

        viewModel.continueScene()
        runCurrent()
        assertEquals(1, requests.size)

        release.send(Unit)
        collectorFinished.receive()
        runCurrent()
        advanceUntilIdle()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)

        viewModel.continueScene()
        runCurrent()

        assertEquals(2, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-1-assistant",
                    text = "one",
                    isStreaming = false,
                ),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Continue scene"),
                WorkshopChatMessage.assistant(
                    id = "generation-2-assistant",
                    text = "one",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
    }

    @Test
    fun sendChatMessage_usesNextGenerationIdAfterRestoredMessages() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val initialState = WorkshopUiState(
            draftText = "Recovered draft",
            messages = listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "First"),
                WorkshopChatMessage.assistant(id = "generation-1-assistant", text = "Reply one"),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Second"),
                WorkshopChatMessage.assistant(id = "generation-2-assistant", text = "Reply two"),
            ),
        )
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = recordingSource(requests) { _, generationId ->
                val messageId = workshopGenerationAssistantMessageId(generationId)
                emit(WorkshopAssistantStreamEvent.Start(workshopGenerationRequestId(generationId), messageId))
                emit(WorkshopAssistantStreamEvent.MarkdownDelta(messageId, "Reply three"))
                emit(WorkshopAssistantStreamEvent.Complete(messageId))
            },
            initialState = initialState,
        )

        viewModel.updateChatInput("Third")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(1, requests.size)
        assertEquals(
            listOf(
                WorkshopChatMessage.user(id = "generation-1-user", text = "First"),
                WorkshopChatMessage.assistant(id = "generation-1-assistant", text = "Reply one"),
                WorkshopChatMessage.user(id = "generation-2-user", text = "Second"),
                WorkshopChatMessage.assistant(id = "generation-2-assistant", text = "Reply two"),
                WorkshopChatMessage.user(id = "generation-3-user", text = "Third"),
                WorkshopChatMessage.assistant(
                    id = "generation-3-assistant",
                    text = "Reply three",
                    isStreaming = false,
                ),
            ),
            viewModel.state.value.messages,
        )
    }

    @Test
    fun updateDraft_serializesPersistCalls() = runTest {
        val firstPersistEntered = Channel<Unit>(capacity = 1)
        val releasePersist = Channel<Unit>(capacity = 1)
        var inFlight = 0
        var maxInFlight = 0
        val persistedDrafts = mutableListOf<String>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ ->
                emit(WorkshopAssistantStreamEvent.Complete("generation-1-assistant"))
            },
            persistState = { state ->
                inFlight += 1
                maxInFlight = maxOf(maxInFlight, inFlight)
                persistedDrafts += state.draftText
                if (persistedDrafts.size == 1) {
                    firstPersistEntered.send(Unit)
                    releasePersist.receive()
                }
                inFlight -= 1
            },
        )

        val secondUpdate = async {
            firstPersistEntered.receive()
            viewModel.updateDraft("second")
            runCurrent()
        }

        viewModel.updateDraft("first")
        runCurrent()
        secondUpdate.await()

        assertEquals(1, maxInFlight)

        releasePersist.send(Unit)
        runCurrent()
        advanceUntilIdle()

        assertEquals(listOf("first", "second"), persistedDrafts)
    }

    @Test
    fun updateDraft_persistsOnlyLatestSnapshotWhenWorkerStartsAfterMultipleUpdates() = runTest {
        val persistenceDispatcher = ReorderingDispatcher()
        val persistedDrafts = mutableListOf<String>()
        val thirdPersisted = Channel<Unit>(capacity = 1)
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ -> },
            persistState = { state ->
                persistedDrafts += state.draftText
                if (state.draftText == "third") {
                    thirdPersisted.trySend(Unit)
                }
            },
            persistenceScope = CoroutineScope(SupervisorJob() + persistenceDispatcher),
        )

        viewModel.updateDraft("first")
        viewModel.updateDraft("second")
        viewModel.updateDraft("third")

        persistenceDispatcher.runAll()
        withTimeout(5_000) {
            thirdPersisted.receive()
        }

        assertEquals(listOf("third"), persistedDrafts)
    }

    @Test
    fun updateDraft_surfacesPersistenceFailure_andClearsItAfterSuccessfulRetry() = runTest {
        val persistedDrafts = mutableListOf<String>()
        val secondPersisted = Channel<Unit>(capacity = 1)
        var shouldFailFirstPersist = true
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ -> },
            persistState = { state ->
                if (state.draftText == "first" && shouldFailFirstPersist) {
                    shouldFailFirstPersist = false
                    throw IllegalStateException("boom")
                }
                persistedDrafts += state.draftText
                if (state.draftText == "second") {
                    secondPersisted.send(Unit)
                }
            },
            persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

        viewModel.updateDraft("first")
        withTimeout(5_000) {
            while (viewModel.state.value.errorMessage != "Failed to save workshop changes.") {
                kotlinx.coroutines.yield()
            }
        }
        assertEquals("Failed to save workshop changes.", viewModel.state.value.errorMessage)

        viewModel.updateDraft("second")

        withTimeout(5_000) {
            secondPersisted.receive()
        }

        assertEquals(listOf("second"), persistedDrafts)
        assertEquals(null, viewModel.state.value.errorMessage)
        viewModel.clear()
    }

    @Test
    fun clear_retriesRetainedLatestSnapshotAfterTransientPersistenceFailure() = runTest {
        val persistedDrafts = mutableListOf<String>()
        val firstFailureObserved = Channel<Unit>(capacity = 1)
        var shouldFail = true
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ -> },
            persistState = { state ->
                if (shouldFail) {
                    shouldFail = false
                    firstFailureObserved.send(Unit)
                    throw IllegalStateException("boom")
                }
                persistedDrafts += state.draftText
            },
            persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

        viewModel.updateDraft("retry me")
        withTimeout(5_000) {
            firstFailureObserved.receive()
        }

        viewModel.clear()
        assertEquals(listOf("retry me"), persistedDrafts)
        assertEquals(null, viewModel.state.value.errorMessage)
    }

    @Test
    fun updateDraft_coalescesPendingSnapshotsWhilePersistIsBusy() = runTest {
        val firstPersistEntered = Channel<Unit>(capacity = 1)
        val releaseFirstPersist = Channel<Unit>(capacity = 1)
        val thirdPersisted = Channel<Unit>(capacity = 1)
        val persistedDrafts = mutableListOf<String>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ -> },
            persistState = { state ->
                persistedDrafts += state.draftText
                when (state.draftText) {
                    "first" -> {
                        firstPersistEntered.send(Unit)
                        releaseFirstPersist.receive()
                    }

                    "third" -> thirdPersisted.send(Unit)
                }
            },
            persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

        viewModel.updateDraft("first")
        withTimeout(5_000) {
            firstPersistEntered.receive()
        }
        viewModel.updateDraft("second")
        viewModel.updateDraft("third")

        releaseFirstPersist.send(Unit)
        withTimeout(5_000) {
            thirdPersisted.receive()
        }

        assertEquals(listOf("first", "third"), persistedDrafts)
        viewModel.clear()
    }

    @Test
    fun clear_doesNotReturnBeforeLatestQueuedSnapshotPersists() = runTest {
        val firstPersistEntered = Channel<Unit>(capacity = 1)
        val releaseFirstPersist = Channel<Unit>(capacity = 1)
        val latestPersisted = Channel<Unit>(capacity = 1)
        val persistedDrafts = mutableListOf<String>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            streamSource = source { _, _ -> },
            persistState = { state ->
                persistedDrafts += state.draftText
                when (state.draftText) {
                    "first" -> {
                        firstPersistEntered.send(Unit)
                        releaseFirstPersist.receive()
                    }

                    "third" -> latestPersisted.send(Unit)
                }
            },
            persistenceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
        )

        viewModel.updateDraft("first")
        withTimeout(5_000) {
            firstPersistEntered.receive()
        }
        viewModel.updateDraft("second")
        viewModel.updateDraft("third")

        val clearStarted = Channel<Unit>(capacity = 1)
        val clearJob = async(Dispatchers.Default) {
            clearStarted.send(Unit)
            viewModel.clear()
        }

        withTimeout(5_000) {
            clearStarted.receive()
        }
        assertFalse(clearJob.isCompleted)

        releaseFirstPersist.send(Unit)
        withTimeout(5_000) {
            latestPersisted.receive()
        }
        clearJob.await()

        assertEquals(listOf("first", "third"), persistedDrafts)
    }
}

private fun newViewModel(
    testScheduler: TestCoroutineScheduler,
    streamSource: WorkshopAssistantStreamSource,
    templateId: String = "workshop-default-template",
    templatePromptBlocks: List<String> = emptyList(),
    initialState: WorkshopUiState = WorkshopUiState(),
    persistState: suspend (WorkshopUiState) -> Unit = {},
    scope: CoroutineScope? = null,
    persistenceScope: CoroutineScope? = null,
): WorkshopViewModel {
    val activeScope = scope ?: CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    val activePersistenceScope = persistenceScope ?: activeScope
    return WorkshopViewModel(
        streamSource = streamSource,
        templateId = templateId,
        templatePromptBlocks = templatePromptBlocks,
        initialState = initialState,
        persistState = persistState,
        scope = activeScope,
        persistenceScope = activePersistenceScope,
    )
}

private fun source(
    block: suspend kotlinx.coroutines.flow.FlowCollector<WorkshopAssistantStreamEvent>.(GenerationRequest, Int) -> Unit,
): WorkshopAssistantStreamSource = object : WorkshopAssistantStreamSource {
    override fun stream(
        request: GenerationRequest,
        generationId: Int,
    ): Flow<WorkshopAssistantStreamEvent> = flow {
        block(request, generationId)
    }
}

private fun recordingSource(
    requests: MutableList<GenerationRequest>,
    block: suspend kotlinx.coroutines.flow.FlowCollector<WorkshopAssistantStreamEvent>.(GenerationRequest, Int) -> Unit,
): WorkshopAssistantStreamSource = source { request, generationId ->
    requests += request
    block(request, generationId)
}

private class ReorderingDispatcher : CoroutineDispatcher() {
    private val queuedTasks = ArrayDeque<Runnable>()

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        queuedTasks.addLast(block)
    }

    fun runLast() {
        queuedTasks.removeLastOrNull()?.run()
    }

    fun runAll() {
        while (queuedTasks.isNotEmpty()) {
            queuedTasks.removeFirst().run()
        }
    }
}
