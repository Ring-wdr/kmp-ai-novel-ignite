package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.withContext
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopViewModelTest {
    @Test
    fun sendChatMessage_keepsTemplatePromptBlocksSeparate_andUsesResolvedTemplateId() = runTest {
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    emit(GenerationEvent.Final("ok"))
                }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    emit(GenerationEvent.Final("ok"))
                }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    emit(GenerationEvent.Final("The gate opened."))
                }
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
    fun abortGeneration_entersRecoveringUntilCollectorFinishes_thenAllowsRetry() = runTest {
        val release = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    when (requests.size) {
                        1 -> {
                            emit(GenerationEvent.Token("The gate opened."))
                            withContext(NonCancellable) {
                                release.receive()
                            }
                            emit(GenerationEvent.Final("The gate opened."))
                        }
                        else -> emit(GenerationEvent.Final("The gate opened."))
                    }
                }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    when (requests.size) {
                        1 -> {
                            emit(GenerationEvent.Token("The gate opened."))
                            emit(GenerationEvent.Error("boom"))
                            release.receive()
                            emit(GenerationEvent.Final("ignored"))
                        }
                        else -> emit(GenerationEvent.Final("The gate opened."))
                    }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Token("The gate opened."))
                    throw CancellationException("boom")
                }
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
    fun synchronousFinalGeneration_entersRecoveringUntilCollectorFinishes_thenAllowsRetry() = runTest {
        val release = Channel<Unit>()
        val collectorFinished = Channel<Unit>()
        val requests = mutableListOf<GenerationRequest>()
        val viewModel = newViewModel(
            testScheduler = testScheduler,
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    if (requests.size == 1) {
                        emit(GenerationEvent.Final("one"))
                        release.receive()
                        collectorFinished.send(Unit)
                    } else {
                        emit(GenerationEvent.Final("one"))
                    }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    if (requests.size == 1) {
                        emit(GenerationEvent.Final("one"))
                        release.receive()
                        collectorFinished.send(Unit)
                    } else {
                        emit(GenerationEvent.Final("one"))
                    }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requests += request
                    emit(GenerationEvent.Final("Reply three"))
                }
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
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flowOf()
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
}

private fun newViewModel(
    testScheduler: TestCoroutineScheduler,
    inferenceEngine: InferenceEngine,
    templateId: String = "workshop-default-template",
    templatePromptBlocks: List<String> = emptyList(),
    initialState: WorkshopUiState = WorkshopUiState(),
    persistState: suspend (WorkshopUiState) -> Unit = {},
): WorkshopViewModel {
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    return WorkshopViewModel(
        inferenceEngine = inferenceEngine,
        templateId = templateId,
        templatePromptBlocks = templatePromptBlocks,
        initialState = initialState,
        persistState = persistState,
        scope = scope,
    )
}
