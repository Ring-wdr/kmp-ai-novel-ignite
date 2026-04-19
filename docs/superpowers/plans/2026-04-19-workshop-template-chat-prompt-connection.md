# Workshop Template Chat Prompt Connection Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make the selected Workshop template act as the default prompt contract for Workshop chat and continue actions, so free-form chat always runs on top of the chosen template’s rules.

**Architecture:** Resolve the selected template’s prompt context in the JVM workshop factory, then inject that context into `WorkshopViewModel` as plain `templateId` and `templatePromptBlocks` values. `WorkshopViewModel` keeps owning request assembly: `continue` uses the template rules as-is, and free chat appends the user’s chat message after the template rules so the model sees both the template contract and the ad-hoc instruction.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, kotlinx.coroutines test, SQLDelight `NovelIgniteDatabase`, `TemplateRepositoryImpl`, JVM `jvmTest` / `desktopTest` sources.

---

## File Structure

### Create

- `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt`
  JVM regression tests for resolving selected template prompt blocks from the local template database.

### Modify

- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
  Accept resolved template prompt context and build generation requests from template rules plus free-form chat input.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`
  Cover template prompt propagation for `sendChatMessage()` and `continueScene()`.
- `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`
  Resolve the selected template’s `promptBlocks` and actual template ID, then pass them into `WorkshopViewModel`.

## Task 1: Add Failing ViewModel Tests For Template Prompt Propagation

**Files:**
- Modify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`

- [ ] **Step 1: Add a failing free-chat template propagation test**

Add this test near the top of `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`:

```kotlin
    @Test
    fun sendChatMessage_prependsTemplatePromptBlocks_andUsesResolvedTemplateId() = runTest {
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
        assertEquals(
            listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
                "Make the reply more ominous.",
            ),
            requests.single().promptBlocks,
        )
    }
```

- [ ] **Step 2: Add a failing continue-scene template propagation test**

Add this second test in the same file:

```kotlin
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
        assertEquals(
            listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
            requests.single().promptBlocks,
        )
    }
```

- [ ] **Step 3: Update the test helper so the new tests can compile**

Replace the `newViewModel` helper at the bottom of `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt` with:

```kotlin
private fun newViewModel(
    testScheduler: TestCoroutineScheduler,
    inferenceEngine: InferenceEngine,
    templateId: String = "workshop-default-template",
    templatePromptBlocks: List<String> = emptyList(),
): WorkshopViewModel {
    val scope = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
    return WorkshopViewModel(
        inferenceEngine = inferenceEngine,
        templateId = templateId,
        templatePromptBlocks = templatePromptBlocks,
        scope = scope,
    )
}
```

- [ ] **Step 4: Run the ViewModel test to verify it fails**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest"
```

Expected: FAIL with constructor/signature mismatches because `WorkshopViewModel` does not yet accept `templateId` / `templatePromptBlocks`, and request assembly still ignores the selected template rules.

- [ ] **Step 5: Commit the failing test checkpoint**

```bash
git add composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt
git commit -m "test: cover workshop template prompt propagation"
```

## Task 2: Resolve Selected Template Prompt Context In The JVM Factory

**Files:**
- Create: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt`
- Modify: `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`

- [ ] **Step 1: Write the failing factory-side template resolution test**

Create `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt` with:

```kotlin
package io.github.ringwdr.novelignite.features.workshop

import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.data.local.TestDatabaseFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest

class WorkshopViewModelFactoryTemplatePromptTest {
    @Test
    fun resolveWorkshopTemplatePromptConfig_loadsPromptBlocksFromSelectedTemplate() = runTest {
        val database = TestDatabaseFactory.create()
        val repository = TemplateRepositoryImpl(database)
        val template = repository.saveTemplate(
            title = "Noir Seoul",
            genre = "Urban Fantasy",
            premise = "A ghost broker solves debts",
            worldSetting = "Night markets and hidden contracts",
            characterCards = "Jin, Hyeon, Broker",
            relationshipNotes = "Debt binds broker and ghost",
            toneStyle = "Moody and elegant",
            bannedElements = "No slapstick",
            plotConstraints = "Reveal one secret per scene",
            openingHook = "Rain on neon stone",
            promptBlocks = listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
        )

        val config = resolveWorkshopTemplatePromptConfig(
            database = database,
            activeTemplate = ActiveWorkshopTemplate(
                id = template.id,
                title = template.title,
            ),
        )

        assertEquals(template.id.toString(), config.templateId)
        assertEquals(
            listOf(
                "Keep the prose lyrical.",
                "Stay in close third person.",
            ),
            config.promptBlocks,
        )
    }
}
```

- [ ] **Step 2: Run the factory test to verify it fails**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelFactoryTemplatePromptTest"
```

Expected: FAIL with unresolved references for `resolveWorkshopTemplatePromptConfig`.

- [ ] **Step 3: Implement prompt-context resolution in the JVM factory**

Update `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt` as follows.

Add the repository import near the top:

```kotlin
import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
```

Insert this config type and resolver below `DefaultWorkshopProjectTitle` / `workshopStateJson`:

```kotlin
internal data class WorkshopTemplatePromptConfig(
    val templateId: String,
    val promptBlocks: List<String>,
)

internal fun resolveWorkshopTemplatePromptConfig(
    database: NovelIgniteDatabase,
    activeTemplate: ActiveWorkshopTemplate?,
): WorkshopTemplatePromptConfig {
    if (activeTemplate == null) {
        return WorkshopTemplatePromptConfig(
            templateId = "workshop-default-template",
            promptBlocks = emptyList(),
        )
    }

    val repository = TemplateRepositoryImpl(database)
    val template = repository.listTemplates().firstOrNull { it.id == activeTemplate.id }

    return WorkshopTemplatePromptConfig(
        templateId = activeTemplate.id.toString(),
        promptBlocks = template?.promptBlocks.orEmpty(),
    )
}
```

Then update `createDefaultWorkshopViewModel(...)` so it resolves the prompt config once and passes it into the view model:

```kotlin
actual fun createDefaultWorkshopViewModel(inferenceEngine: InferenceEngine): WorkshopViewModel {
    val database = openDesktopDatabase()
    val activeTemplate = ActiveWorkshopTemplateStore.selection.value
    val promptConfig = resolveWorkshopTemplatePromptConfig(
        database = database,
        activeTemplate = activeTemplate,
    )
    val project = ensureWorkshopProject(
        database = database,
        activeTemplate = activeTemplate,
    )
    val repository = DraftSessionRepositoryImpl(database)
    val latestSession = repository.latestDraftSession(project.id)
    var currentSessionId = latestSession?.id
    val initialState = latestSession
        ?.let { parseStoredState(it.content) }
        ?: WorkshopUiState()

    return WorkshopViewModel(
        inferenceEngine = inferenceEngine,
        initialState = initialState,
        templateId = promptConfig.templateId,
        templatePromptBlocks = promptConfig.promptBlocks,
        persistState = { state ->
            val content = buildStoredContent(state)
            val saved = repository.saveDraftSession(
                projectId = project.id,
                sessionId = currentSessionId,
                content = content,
            )
            currentSessionId = saved.id
        },
    )
}
```

- [ ] **Step 4: Run the factory test to verify it passes**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelFactoryTemplatePromptTest"
```

Expected: PASS with `resolveWorkshopTemplatePromptConfig_loadsPromptBlocksFromSelectedTemplate`.

- [ ] **Step 5: Commit the factory prompt-context wiring**

```bash
git add composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt
git commit -m "feat: resolve workshop template prompt context"
```

## Task 3: Thread Template Prompt Context Through WorkshopViewModel

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`

- [ ] **Step 1: Add template context parameters to the ViewModel**

Update the constructor signature in `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt` to:

```kotlin
class WorkshopViewModel(
    private val inferenceEngine: InferenceEngine,
    initialState: WorkshopUiState = WorkshopUiState(),
    private val templateId: String = "workshop-default-template",
    private val templatePromptBlocks: List<String> = emptyList(),
    private val persistState: suspend (WorkshopUiState) -> Unit = {},
    private val scope: CoroutineScope = defaultWorkshopScope(),
) {
```

- [ ] **Step 2: Build request prompt blocks from template rules plus free chat**

Inside `startGeneration(...)`, replace the local prompt assembly:

```kotlin
        val manuscriptExcerpt = _state.value.draftText
        val promptBlocks = if (actionType == "chat") listOf(userFacingPrompt) else emptyList()
```

with:

```kotlin
        val manuscriptExcerpt = _state.value.draftText
        val promptBlocks = when (actionType) {
            "chat" -> templatePromptBlocks + userFacingPrompt
            else -> templatePromptBlocks
        }
```

- [ ] **Step 3: Use the resolved template ID in generation requests**

Still inside `startGeneration(...)`, replace the `GenerationRequest` construction:

```kotlin
                    GenerationRequest(
                        projectId = "workshop-project",
                        templateId = "workshop-default-template",
                        actionType = actionType,
                        manuscriptExcerpt = manuscriptExcerpt,
                        promptBlocks = promptBlocks,
                    )
```

with:

```kotlin
                    GenerationRequest(
                        projectId = "workshop-project",
                        templateId = templateId,
                        actionType = actionType,
                        manuscriptExcerpt = manuscriptExcerpt,
                        promptBlocks = promptBlocks,
                    )
```

- [ ] **Step 4: Run the ViewModel test suite to verify it passes**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest"
```

Expected: PASS, including:
- `sendChatMessage_prependsTemplatePromptBlocks_andUsesResolvedTemplateId`
- `continueScene_usesTemplatePromptBlocks_withoutAppendingContinueLabel`

- [ ] **Step 5: Commit the ViewModel prompt wiring**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt
git commit -m "feat: apply workshop templates to chat prompts"
```

## Task 4: Run Regression Verification For Template-Driven Workshop Chat

**Files:**
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt`
- Verify: `composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt`
- Verify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt`
- Verify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt`

- [ ] **Step 1: Run the targeted template-prompt tests together**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelTest" --tests "io.github.ringwdr.novelignite.features.workshop.WorkshopViewModelFactoryTemplatePromptTest"
```

Expected: PASS with all template prompt propagation tests green.

- [ ] **Step 2: Run the full JVM suite as the broadest available automated verification**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest
```

Expected: PASS.

- [ ] **Step 3: Record the environment-limited gap explicitly**

Note in the implementation summary:

```text
:composeApp:allTests remains environment-blocked in this worktree unless Android SDK configuration is provided via local.properties / ANDROID_HOME.
```

- [ ] **Step 4: Commit the verified template prompt connection**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModel.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelTest.kt composeApp/src/jvmMain/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactory.jvm.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/workshop/WorkshopViewModelFactoryTemplatePromptTest.kt
git commit -m "feat: connect workshop templates to chat prompts"
```

## Self-Review

### Spec coverage

- Selected template should act as the default chat contract: covered by Tasks 2 and 3.
- Free-form chat should be layered on top of template rules: covered by Task 1 and Task 3.
- `continue` should still use template rules without echoing the UI label into prompt blocks: covered by Task 1 and Task 3.
- Actual selected template ID and prompt blocks should be resolved from storage, not hard-coded: covered by Task 2.
- Automated verification should fit the current worktree constraints: covered by Task 4.

No uncovered requirements remain for this template-connection scope.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task includes exact file paths, code blocks, commands, and expected outcomes.

### Type consistency

- `WorkshopTemplatePromptConfig`, `templateId`, and `templatePromptBlocks` are introduced once and reused consistently.
- `GenerationRequest.templateId` stays `String`, matching the existing domain contract.
- The ViewModel helper in tests uses the same new constructor parameters defined in Task 3.
