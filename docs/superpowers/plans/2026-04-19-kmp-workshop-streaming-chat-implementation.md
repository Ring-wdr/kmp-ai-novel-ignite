# KMP Workshop Streaming Chat Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the `Workshop` streaming chat experience so users can submit chat turns, watch the latest assistant reply grow progressively, abort generation safely, recover from errors, and restore only durable chat history after restart.

**Architecture:** Keep the current provider-agnostic `InferenceEngine` boundary, but reshape `Workshop` state from a single `generatedText` string into durable chat messages plus a small streaming status model. Implement streaming in two layers: the inference layer emits progressive token events, and the `WorkshopViewModel` converts those events into a single in-progress assistant turn that the Compose UI renders. Persist only durable state through a dedicated snapshot codec so interrupted assistant turns never come back as completed history.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3, kotlinx.coroutines `Flow`, kotlinx.serialization, SQLDelight-backed `DraftSessionRepositoryImpl`, JVM `HttpURLConnection` for Ollama streaming on Desktop, Kotlin test, and Compose Desktop UI tests.

---

## File Structure

### Create

- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatModels.kt`
  Shared message role, streaming status, and chat message models for `Workshop`.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshot.kt`
  Durable serialization contract for `WorkshopUiState`, filtering out unfinished assistant turns.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatComposer.kt`
  Shared Compose input row for chat text entry and send action.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatTimeline.kt`
  Shared Compose message list for user turns, completed assistant turns, and the current streaming turn.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshotTest.kt`
  Regression tests for durable-only persistence and legacy state fallback.
- `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClientTest.kt`
  JVM integration-style tests for parsing Ollama NDJSON streaming responses.
- `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanelTest.kt`
  Compose Desktop UI tests for streaming controls and error visibility.

### Modify

- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopUiState.kt`
  Replace single-output state with durable messages, input text, streaming status, and error message.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
  Drive send, continue, token accumulation, abort cleanup, retry readiness, and durable persistence timing.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanel.kt`
  Render the streaming chat panel using the new timeline and composer components.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopScreen.kt`
  Wire the screen to the expanded `WorkshopViewModel` contract.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/FakeInferenceEngine.kt`
  Emit progressive token events so local UI and tests can exercise streaming behavior.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngine.kt`
  Map streamed Ollama chunks into `GenerationEvent.Token` and `GenerationEvent.Final`.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/OllamaModelsClient.kt`
  Expose a streaming generation API for the Desktop Ollama client.
- `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClient.kt`
  Parse Ollama NDJSON stream responses and emit chunk strings progressively.
- `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`
  Persist and restore `WorkshopStateSnapshot` instead of the old `generatedText` schema.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`
  Cover message lifecycle, abort cleanup, retry readiness, and single-stream enforcement.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngineTest.kt`
  Cover streaming event mapping from the client layer.

## Task 1: Add Durable Workshop Chat State And Snapshot Persistence

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatModels.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshot.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshotTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopUiState.kt`
- Modify: `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`

- [ ] **Step 1: Write the failing snapshot tests**

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkshopStateSnapshotTest {
    @Test
    fun fromUiState_persistsOnlyDurableTurns() {
        val snapshot = WorkshopStateSnapshot.from(
            WorkshopUiState(
                draftText = "The gate opened.",
                chatInputText = "unfinished prompt",
                messages = listOf(
                    WorkshopChatMessage.user(id = "user-1", text = "Continue scene"),
                    WorkshopChatMessage.assistant(id = "assistant-1", text = "The gate opened."),
                    WorkshopChatMessage.assistant(
                        id = "assistant-2",
                        text = " A silver",
                        isStreaming = true,
                    ),
                ),
                streamingStatus = WorkshopStreamingStatus.Streaming,
                errorMessage = "boom",
            )
        )

        assertEquals("The gate opened.", snapshot.draftText)
        assertEquals(listOf("user-1", "assistant-1"), snapshot.messages.map { it.id })
        assertNull(snapshot.errorMessage)
    }

    @Test
    fun toUiState_restoresIdleStateWithoutRevivingStreamingAssistantTurn() {
        val restored = WorkshopStateSnapshot(
            draftText = "The gate opened.",
            messages = listOf(
                WorkshopPersistedMessage(id = "user-1", role = WorkshopMessageRole.User, text = "Continue scene"),
                WorkshopPersistedMessage(id = "assistant-1", role = WorkshopMessageRole.Assistant, text = "The gate opened."),
            ),
        ).toUiState()

        assertEquals(WorkshopStreamingStatus.Idle, restored.streamingStatus)
        assertEquals(2, restored.messages.size)
        assertEquals("", restored.chatInputText)
        assertNull(restored.errorMessage)
    }
}
```

- [ ] **Step 2: Run the snapshot test to verify it fails**

Run: `.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopStateSnapshotTest"`

Expected: FAIL with unresolved references for `WorkshopStateSnapshot`, `WorkshopChatMessage`, `WorkshopStreamingStatus`, and the new `WorkshopUiState` properties.

- [ ] **Step 3: Implement the shared chat models, snapshot codec, and JVM factory wiring**

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatModels.kt`:

```kotlin
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
            WorkshopChatMessage(id = id, role = WorkshopMessageRole.User, text = text)

        fun assistant(id: String, text: String, isStreaming: Boolean = false): WorkshopChatMessage =
            WorkshopChatMessage(
                id = id,
                role = WorkshopMessageRole.Assistant,
                text = text,
                isStreaming = isStreaming,
            )
    }
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopUiState.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

data class WorkshopUiState(
    val draftText: String = "",
    val chatInputText: String = "",
    val messages: List<WorkshopChatMessage> = emptyList(),
    val streamingStatus: WorkshopStreamingStatus = WorkshopStreamingStatus.Idle,
    val errorMessage: String? = null,
) {
    val isGenerating: Boolean
        get() = streamingStatus == WorkshopStreamingStatus.Streaming
}
```

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshot.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import kotlinx.serialization.Serializable

@Serializable
data class WorkshopPersistedMessage(
    val id: String,
    val role: WorkshopMessageRole,
    val text: String,
)

@Serializable
data class WorkshopStateSnapshot(
    val draftText: String,
    val messages: List<WorkshopPersistedMessage>,
    val errorMessage: String? = null,
) {
    fun toUiState(): WorkshopUiState = WorkshopUiState(
        draftText = draftText,
        messages = messages.map { persisted ->
            WorkshopChatMessage(
                id = persisted.id,
                role = persisted.role,
                text = persisted.text,
                isStreaming = false,
            )
        },
        streamingStatus = WorkshopStreamingStatus.Idle,
        errorMessage = null,
    )

    companion object {
        fun from(state: WorkshopUiState): WorkshopStateSnapshot = WorkshopStateSnapshot(
            draftText = state.draftText,
            messages = state.messages
                .filterNot { it.isStreaming }
                .map { message ->
                    WorkshopPersistedMessage(
                        id = message.id,
                        role = message.role,
                        text = message.text,
                    )
                },
        )
    }
}
```

Update the persistence helpers in `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`:

```kotlin
private fun buildStoredContent(state: WorkshopUiState): String =
    workshopStateJson.encodeToString(WorkshopStateSnapshot.from(state))

private fun parseStoredState(content: String): WorkshopUiState =
    runCatching { workshopStateJson.decodeFromString<WorkshopStateSnapshot>(content).toUiState() }
        .getOrElse { WorkshopUiState(draftText = content) }
```

- [ ] **Step 4: Run the tests to verify the snapshot contract passes**

Run: `.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopStateSnapshotTest"`

Expected: PASS with 2 tests passing.

- [ ] **Step 5: Commit the durable state contract**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatModels.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshot.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopUiState.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopStateSnapshotTest.kt composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt
git commit -m "feat: add durable workshop chat state"
```

## Task 2: Refactor WorkshopViewModel For Streaming, Abort, And Recovery

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`

- [ ] **Step 1: Write failing ViewModel tests for message lifecycle and abort**

Replace `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt` with:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class WorkshopViewModelTest {
    @Test
    fun sendChatMessage_appendsUserTurnAndStreamsAssistantReply() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Token("The gate opened."))
                    emit(GenerationEvent.Token(" A silver wind answered."))
                    emit(GenerationEvent.Final("The gate opened. A silver wind answered."))
                }
            },
            scope = scope,
        )

        viewModel.updateDraft("The gate opened.")
        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertEquals(2, viewModel.state.value.messages.size)
        assertEquals(WorkshopMessageRole.User, viewModel.state.value.messages.first().role)
        assertEquals(WorkshopMessageRole.Assistant, viewModel.state.value.messages.last().role)
        assertFalse(viewModel.state.value.messages.last().isStreaming)
        assertEquals(
            "The gate opened. A silver wind answered.",
            viewModel.state.value.messages.last().text,
        )
    }

    @Test
    fun abortGeneration_discardsStreamingAssistantTurnAndKeepsUserTurn() = runTest {
        val release = Channel<Unit>()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Token("The gate"))
                    release.receive()
                    emit(GenerationEvent.Final("The gate opened."))
                }
            },
            scope = scope,
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()
        viewModel.abortGeneration()
        runCurrent()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertEquals(listOf("Continue the scene"), viewModel.state.value.messages.map { it.text })
        assertNull(viewModel.state.value.errorMessage)
    }

    @Test
    fun generationError_cleansUpStreamingAssistantAndLeavesRetryableState() = runTest {
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val viewModel = WorkshopViewModel(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    emit(GenerationEvent.Token("The gate"))
                    emit(GenerationEvent.Error("boom"))
                }
            },
            scope = scope,
        )

        viewModel.updateChatInput("Continue the scene")
        viewModel.sendChatMessage()
        runCurrent()

        assertEquals(WorkshopStreamingStatus.Idle, viewModel.state.value.streamingStatus)
        assertEquals(1, viewModel.state.value.messages.size)
        assertEquals("boom", viewModel.state.value.errorMessage)
        assertEquals("Continue the scene", viewModel.state.value.messages.single().text)
    }

    @Test
    fun continueScene_ignoresSecondRequestWhileStreamIsActive() = runTest {
        val release = Channel<Unit>()
        val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        var requestCount = 0
        val viewModel = WorkshopViewModel(
            inferenceEngine = object : InferenceEngine {
                override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
                    requestCount += 1
                    emit(GenerationEvent.Token("The gate"))
                    release.receive()
                    emit(GenerationEvent.Final("The gate opened."))
                }
            },
            scope = scope,
        )

        viewModel.continueScene()
        viewModel.continueScene()
        runCurrent()

        assertEquals(1, requestCount)
        assertTrue(viewModel.state.value.isGenerating)
    }
}
```

- [ ] **Step 2: Run the ViewModel tests to verify they fail**

Run: `.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest"`

Expected: FAIL with unresolved references for `updateChatInput`, `sendChatMessage`, `abortGeneration`, and the new `WorkshopUiState` fields.

- [ ] **Step 3: Implement streaming message lifecycle in the ViewModel**

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt` to:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WorkshopViewModel(
    private val inferenceEngine: InferenceEngine,
    initialState: WorkshopUiState = WorkshopUiState(),
    private val persistState: suspend (WorkshopUiState) -> Unit = {},
    private val scope: CoroutineScope = defaultWorkshopScope(),
) {
    private val _state = MutableStateFlow(initialState)
    private var nextGenerationId = 0
    private var activeGenerationId = 0
    private var activeAssistantMessageId: String? = null
    private var activeGenerationJob: Job? = null

    val state: StateFlow<WorkshopUiState> = _state

    fun updateDraft(text: String) {
        _state.update { it.copy(draftText = text) }
        persistCurrentState()
    }

    fun updateChatInput(text: String) {
        _state.update { it.copy(chatInputText = text) }
    }

    fun sendChatMessage() {
        val prompt = _state.value.chatInputText.trim()
        if (prompt.isBlank()) return
        startGeneration(userFacingPrompt = prompt, actionType = "chat")
    }

    fun continueScene() {
        startGeneration(userFacingPrompt = "Continue scene", actionType = "continue")
    }

    fun abortGeneration() {
        if (activeGenerationJob?.isActive != true) return
        val generationId = activeGenerationId
        activeGenerationJob?.cancel(CancellationException("User aborted generation"))
        cleanupStreamingAssistant(
            generationId = generationId,
            nextStatus = WorkshopStreamingStatus.Idle,
            errorMessage = null,
        )
    }

    fun clear() {
        scope.cancel()
    }

    private fun startGeneration(
        userFacingPrompt: String,
        actionType: String,
    ) {
        if (activeGenerationJob?.isActive == true) return

        val generationId = ++nextGenerationId
        val assistantId = "assistant-$generationId"
        activeGenerationId = generationId
        activeAssistantMessageId = assistantId

        _state.update { current ->
            current.copy(
                chatInputText = "",
                errorMessage = null,
                streamingStatus = WorkshopStreamingStatus.Streaming,
                messages = current.messages +
                    WorkshopChatMessage.user(
                        id = "user-$generationId",
                        text = userFacingPrompt,
                    ) +
                    WorkshopChatMessage.assistant(
                        id = assistantId,
                        text = "",
                        isStreaming = true,
                    ),
            )
        }
        persistCurrentState()

        activeGenerationJob = scope.launch(start = CoroutineStart.UNDISPATCHED) {
            try {
                inferenceEngine.streamGenerate(
                    GenerationRequest(
                        projectId = "workshop-project",
                        templateId = "workshop-default-template",
                        actionType = actionType,
                        manuscriptExcerpt = _state.value.draftText,
                        promptBlocks = emptyList(),
                    )
                ).collect { event ->
                    when (event) {
                        is GenerationEvent.Token -> appendAssistantToken(assistantId, event.text)
                        is GenerationEvent.Final -> finalizeAssistantTurn(assistantId, event.text)
                        is GenerationEvent.Error -> cleanupStreamingAssistant(
                            generationId = generationId,
                            nextStatus = WorkshopStreamingStatus.Idle,
                            errorMessage = event.message,
                        )
                    }
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (throwable: Throwable) {
                cleanupStreamingAssistant(
                    generationId = generationId,
                    nextStatus = WorkshopStreamingStatus.Idle,
                    errorMessage = throwable.message ?: "Generation failed.",
                )
            } finally {
                if (activeGenerationId == generationId) {
                    activeGenerationJob = null
                    activeGenerationId = 0
                    activeAssistantMessageId = null
                    _state.update { current ->
                        current.copy(
                            streamingStatus = WorkshopStreamingStatus.Idle,
                        )
                    }
                }
            }
        }
    }

    private fun appendAssistantToken(
        assistantId: String,
        token: String,
    ) {
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(text = message.text + token, isStreaming = true)
                    } else {
                        message
                    }
                },
            )
        }
    }

    private fun finalizeAssistantTurn(
        assistantId: String,
        finalText: String,
    ) {
        _state.update { current ->
            current.copy(
                messages = current.messages.map { message ->
                    if (message.id == assistantId) {
                        message.copy(text = finalText, isStreaming = false)
                    } else {
                        message
                    }
                },
                streamingStatus = WorkshopStreamingStatus.Idle,
            )
        }
        persistCurrentState()
    }

    private fun cleanupStreamingAssistant(
        generationId: Int,
        nextStatus: WorkshopStreamingStatus,
        errorMessage: String?,
    ) {
        if (activeGenerationId != generationId) return

        val assistantId = activeAssistantMessageId
        _state.update { current ->
            current.copy(
                messages = current.messages.filterNot { it.id == assistantId && it.isStreaming },
                streamingStatus = nextStatus,
                errorMessage = errorMessage,
            )
        }
        persistCurrentState()
    }

    private fun persistCurrentState() {
        val snapshot = _state.value
        scope.launch {
            persistState(snapshot)
        }
    }
}

private fun defaultWorkshopScope(): CoroutineScope =
    CoroutineScope(SupervisorJob() + Dispatchers.Default)
```

- [ ] **Step 4: Run the ViewModel tests to verify streaming lifecycle passes**

Run: `.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest"`

Expected: PASS with all ViewModel lifecycle tests green.

- [ ] **Step 5: Commit the streaming ViewModel behavior**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt
git commit -m "feat: add workshop streaming message lifecycle"
```

## Task 3: Stream Ollama Tokens Through The Inference Layer

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/OllamaModelsClient.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngine.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/FakeInferenceEngine.kt`
- Modify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngineTest.kt`
- Create: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClientTest.kt`
- Modify: `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClient.kt`

- [ ] **Step 1: Write the failing inference streaming tests**

Update `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngineTest.kt` to:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class LocalOllamaEngineTest {
    @Test
    fun streamGenerate_mapsChunkFlowToTokenAndFinalEvents() = runTest {
        val engine = LocalOllamaEngine(
            modelsClient = object : OllamaModelsClient {
                override suspend fun listModels(): List<String> = listOf("gemma3")

                override fun streamGenerate(model: String, prompt: String): Flow<String> = flow {
                    emit("The gate opened.")
                    emit(" A silver wind answered.")
                }
            },
            modelName = "gemma3",
        )

        val events = engine.streamGenerate(
            GenerationRequest(
                projectId = "workshop-project",
                templateId = "workshop-default-template",
                actionType = "continue",
                manuscriptExcerpt = "The gate opened.",
                promptBlocks = emptyList(),
            )
        ).toList()

        assertEquals(
            listOf(
                GenerationEvent.Token("The gate opened."),
                GenerationEvent.Token(" A silver wind answered."),
                GenerationEvent.Final("The gate opened. A silver wind answered."),
            ),
            events,
        )
    }
}
```

Create `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClientTest.kt`:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest

class DesktopOllamaModelsClientTest {
    @Test
    fun streamGenerate_parsesNdjsonChunksIntoTokenStrings() = runTest {
        val server = HttpServer.create(InetSocketAddress(0), 0)
        server.createContext("/api/generate") { exchange ->
            val body = """
                {"response":"The gate opened.","done":false}
                {"response":" A silver wind answered.","done":false}
                {"response":"","done":true}
            """.trimIndent()
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.bufferedWriter().use { writer ->
                body.lineSequence().forEach { line ->
                    writer.write(line)
                    writer.write("\n")
                    writer.flush()
                }
            }
        }
        server.start()

        try {
            val client = DesktopOllamaModelsClient("http://127.0.0.1:${server.address.port}")
            val chunks = client.streamGenerate(
                model = "gemma3",
                prompt = "Continue scene",
            ).toList()

            assertEquals(
                listOf("The gate opened.", " A silver wind answered."),
                chunks,
            )
        } finally {
            server.stop(0)
        }
    }
}
```

- [ ] **Step 2: Run the inference tests to verify they fail**

Run:

```bash
.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.data.inference.LocalOllamaEngineTest"
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.data.inference.DesktopOllamaModelsClientTest"
```

Expected: FAIL because `OllamaModelsClient.streamGenerate` does not exist and the Desktop client cannot parse streaming responses yet.

- [ ] **Step 3: Implement streaming-friendly Ollama clients and engine mapping**

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/OllamaModelsClient.kt`:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import kotlinx.coroutines.flow.Flow

interface OllamaModelsClient {
    suspend fun listModels(): List<String>
    fun streamGenerate(model: String, prompt: String): Flow<String>
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngine.kt`:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class LocalOllamaEngine(
    private val modelsClient: OllamaModelsClient,
    private val modelName: String,
) : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        val prompt = toPrompt(
            actionType = request.actionType,
            manuscriptExcerpt = request.manuscriptExcerpt,
            promptBlocks = request.promptBlocks,
        )

        val fullText = StringBuilder()
        try {
            modelsClient.streamGenerate(model = modelName, prompt = prompt).collect { chunk ->
                fullText.append(chunk)
                emit(GenerationEvent.Token(chunk))
            }
            emit(GenerationEvent.Final(fullText.toString()))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (throwable: Throwable) {
            emit(GenerationEvent.Error(throwable.message ?: "Ollama generation failed."))
        }
    }

    companion object {
        fun toPrompt(
            actionType: String,
            manuscriptExcerpt: String,
            promptBlocks: List<String>,
        ): String = buildString {
            appendLine("Action: $actionType")
            appendLine("Rules:")
            promptBlocks.forEach { appendLine("- $it") }
            appendLine("Excerpt:")
            append(manuscriptExcerpt)
        }
    }
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/FakeInferenceEngine.kt`:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import io.github.ringwdr.novelignite.domain.inference.GenerationEvent
import io.github.ringwdr.novelignite.domain.inference.GenerationRequest
import io.github.ringwdr.novelignite.domain.inference.InferenceEngine
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

internal class FakeInferenceEngine : InferenceEngine {
    override fun streamGenerate(request: GenerationRequest): Flow<GenerationEvent> = flow {
        emit(GenerationEvent.Token("The gate opened."))
        emit(GenerationEvent.Token(" A silver wind answered."))
        emit(GenerationEvent.Final("The gate opened. A silver wind answered."))
    }
}
```

Update `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClient.kt`:

```kotlin
package io.github.ringwdr.novelignite.data.inference

import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

internal class DesktopOllamaModelsClient(
    private val baseUrl: String,
) : OllamaModelsClient {
    private val json = Json { ignoreUnknownKeys = true }
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    override suspend fun listModels(): List<String> {
        val connection = URL("$normalizedBaseUrl/api/tags").openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        return connection.inputStream.bufferedReader().use { reader ->
            json.decodeFromString<TagsResponse>(reader.readText()).models.map { it.name }
        }
    }

    override fun streamGenerate(model: String, prompt: String): Flow<String> = flow {
        val connection = URL("$normalizedBaseUrl/api/generate").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json")
        val requestBody = json.encodeToString(
            GenerateRequest(
                model = model,
                prompt = prompt,
                stream = true,
            )
        )

        connection.outputStream.bufferedWriter().use { writer ->
            writer.write(requestBody)
        }

        if (connection.responseCode !in 200..299) {
            throw IllegalStateException("Ollama generate failed: ${connection.errorStream?.bufferedReader()?.readText().orEmpty()}")
        }

        try {
            connection.inputStream.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }.forEach { line ->
                    currentCoroutineContext().ensureActive()
                    val chunk = json.decodeFromString<GenerateStreamChunk>(line)
                    if (chunk.response.isNotEmpty()) {
                        emit(chunk.response)
                    }
                    if (chunk.done) return@useLines
                }
            }
        } finally {
            connection.disconnect()
        }
    }
}

@Serializable
private data class GenerateRequest(
    val model: String,
    val prompt: String,
    val stream: Boolean,
)

@Serializable
private data class GenerateStreamChunk(
    val response: String = "",
    val done: Boolean = false,
)

@Serializable
private data class TagsResponse(
    val models: List<TagModel> = emptyList(),
)

@Serializable
private data class TagModel(
    @SerialName("name")
    val name: String,
)
```

- [ ] **Step 4: Run the inference tests to verify progressive events pass**

Run:

```bash
.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.data.inference.LocalOllamaEngineTest"
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.data.inference.DesktopOllamaModelsClientTest"
```

Expected: PASS with both common and JVM inference tests green.

- [ ] **Step 5: Commit the inference streaming layer**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/OllamaModelsClient.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngine.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/FakeInferenceEngine.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngineTest.kt composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClient.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClientTest.kt
git commit -m "feat: stream ollama tokens into workshop chat"
```

## Task 4: Build The Compose Streaming Chat UI

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatComposer.kt`
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatTimeline.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanel.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopScreen.kt`
- Create: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanelTest.kt`

- [ ] **Step 1: Write the failing Compose UI test for streaming controls**

Create `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanelTest.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import kotlin.test.Test
import org.junit.Rule

class ChatPanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun streamingState_rendersConversationAndAbortAction() {
        rule.setContent {
            ChatPanel(
                messages = listOf(
                    WorkshopChatMessage.user(id = "user-1", text = "Continue scene"),
                    WorkshopChatMessage.assistant(
                        id = "assistant-1",
                        text = "The gate opened.",
                        isStreaming = true,
                    ),
                ),
                chatInputText = "Continue the scene",
                streamingStatus = WorkshopStreamingStatus.Streaming,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithText("Continue scene").assertExists()
        rule.onNodeWithText("The gate opened.").assertExists()
        rule.onNodeWithText("Abort").assertExists()
        rule.onNodeWithTag(WORKSHOP_CHAT_INPUT_TAG).assertExists()
    }
}
```

- [ ] **Step 2: Run the Compose UI test to verify it fails**

Run: `.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.ChatPanelTest"`

Expected: FAIL because `ChatPanel` does not accept message lists, input text, or abort callbacks yet.

- [ ] **Step 3: Implement the Workshop chat timeline, composer, and screen wiring**

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatComposer.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

const val WORKSHOP_CHAT_INPUT_TAG = "workshop-chat-input"

@Composable
fun WorkshopChatComposer(
    input: String,
    isStreaming: Boolean,
    onInputChange: (String) -> Unit,
    onSendChatMessage: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(androidx.compose.ui.unit.dp(12)),
    ) {
        OutlinedTextField(
            value = input,
            onValueChange = onInputChange,
            modifier = Modifier
                .weight(1f)
                .testTag(WORKSHOP_CHAT_INPUT_TAG),
            placeholder = { Text("Ask the workshop for a rewrite, continuation, or idea.") },
            enabled = !isStreaming,
        )
        Button(
            onClick = onSendChatMessage,
            enabled = !isStreaming && input.isNotBlank(),
        ) {
            Text("Send")
        }
    }
}
```

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatTimeline.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun WorkshopChatTimeline(
    messages: List<WorkshopChatMessage>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        messages.forEach { message ->
            val background = if (message.role == WorkshopMessageRole.User) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background, RoundedCornerShape(16.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = if (message.role == WorkshopMessageRole.User) "You" else "Assistant",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = message.text.ifBlank {
                        if (message.isStreaming) "Generating..." else ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Unspecified,
                )
            }
        }
    }
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanel.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatPanel(
    messages: List<WorkshopChatMessage>,
    chatInputText: String,
    streamingStatus: WorkshopStreamingStatus,
    errorMessage: String?,
    onChatInputChange: (String) -> Unit,
    onSendChatMessage: () -> Unit,
    onContinueScene: () -> Unit,
    onAbortGeneration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Workshop Chat")
            errorMessage?.let { Text(it) }
            WorkshopChatTimeline(messages = messages)
            WorkshopChatComposer(
                input = chatInputText,
                isStreaming = streamingStatus == WorkshopStreamingStatus.Streaming,
                onInputChange = onChatInputChange,
                onSendChatMessage = onSendChatMessage,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onContinueScene,
                    enabled = streamingStatus == WorkshopStreamingStatus.Idle,
                ) {
                    Text(if (streamingStatus == WorkshopStreamingStatus.Streaming) "Continuing..." else "Continue scene")
                }
                if (streamingStatus == WorkshopStreamingStatus.Streaming) {
                    OutlinedButton(onClick = onAbortGeneration) {
                        Text("Abort")
                    }
                }
            }
        }
    }
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopScreen.kt`:

```kotlin
ChatPanel(
    messages = state.messages,
    chatInputText = state.chatInputText,
    streamingStatus = state.streamingStatus,
    errorMessage = state.errorMessage,
    onChatInputChange = activeViewModel::updateChatInput,
    onSendChatMessage = activeViewModel::sendChatMessage,
    onContinueScene = activeViewModel::continueScene,
    onAbortGeneration = activeViewModel::abortGeneration,
)
```

- [ ] **Step 4: Run the UI and regression tests**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.ChatPanelTest"
.\gradlew.bat :composeApp:allTests --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest"
```

Expected: PASS with the new chat panel test and the ViewModel regression suite green.

- [ ] **Step 5: Commit the Compose streaming chat UI**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatComposer.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopChatTimeline.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanel.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopScreen.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanelTest.kt
git commit -m "feat: render workshop streaming chat ui"
```

## Task 5: Run End-To-End Verification And Clean Up Contract Drift

**Files:**
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanel.kt`
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference/LocalOllamaEngine.kt`
- Verify: `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/data/inference/DesktopOllamaModelsClient.kt`
- Verify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`
- Verify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/ChatPanelTest.kt`

- [ ] **Step 1: Run the full Compose app test suites**

Run:

```bash
.\gradlew.bat :composeApp:allTests
.\gradlew.bat :composeApp:jvmTest
```

Expected: PASS with no failing `Workshop`, inference, or Desktop Compose tests.

- [ ] **Step 2: Run the desktop smoke test to ensure the app shell still boots**

Run: `.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.smoke.AppSmokeTest"`

Expected: PASS with `bootstrap_containsExpectedMvpAreas`.

- [ ] **Step 3: Launch the desktop app for a manual streaming smoke pass**

Run: `.\gradlew.bat :composeApp:run`

Expected manual checks:

- Enter text in the manuscript editor and verify it still updates.
- Send a chat prompt and confirm a user turn appears immediately.
- Confirm the latest assistant turn grows progressively.
- Click `Abort` during generation and confirm the unfinished assistant turn disappears.
- Force an Ollama failure and confirm the error is visible while earlier conversation remains intact.

- [ ] **Step 4: Only continue to the final commit if every verification step passed unchanged**

If any automated test or manual smoke check fails, stop here and return to the task that owns the failing file instead of making the final commit. Do not patch verification failures ad hoc outside the owning task.

- [ ] **Step 5: Commit the verified streaming chat feature**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/data/inference composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/data/inference composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/data/inference
git commit -m "feat: ship workshop streaming chat experience"
```

## Self-Review

### Spec coverage

- User-visible streaming flow: covered by Tasks 2 and 4.
- Minimal state model (`idle`, `streaming`, `recovering`): covered by Tasks 1 and 2.
- Message lifecycle and progressive updates: covered by Task 2.
- Abort behavior: covered by Tasks 2 and 4.
- Error cleanup and retry readiness: covered by Tasks 2 and 5.
- Durable-only persistence and restoration: covered by Task 1.
- Local inference progressive tokens: covered by Task 3.
- Validation expectations: covered by Task 5.

No uncovered spec requirements remain for the current repository scope.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task includes exact file paths, code, commands, and expected outcomes.

### Type consistency

- `WorkshopChatMessage`, `WorkshopMessageRole`, `WorkshopStreamingStatus`, and `WorkshopStateSnapshot` are introduced in Task 1 and reused consistently later.
- `updateChatInput`, `sendChatMessage`, `continueScene`, and `abortGeneration` are introduced in Task 2 and reused consistently in Task 4.
- `OllamaModelsClient.streamGenerate` is introduced in Task 3 and reused consistently by `LocalOllamaEngine`.
