# Templates List Editor Separation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restructure `Templates` so the list is the default surface, template creation and editing share a dedicated editor screen, and destructive or unsaved-change flows are explicit instead of being mixed into the catalog view.

**Architecture:** Keep top-level app navigation unchanged and implement this as internal state inside `TemplatesScreen`. Split the current large screen into a small route shell plus focused list and editor pane composables, and add a lightweight `TemplatesScreenState` model so remix, save feedback, highlight state, delete confirmation, and back-navigation prompts stay deterministic.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform Material 3, Compose Desktop UI tests, kotlinx.coroutines test, existing `TemplateEditorViewModel`, `TemplateRemixStore`, and `ActiveWorkshopTemplateStore`.

---

## File Structure

### Create

- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenState.kt`
  Internal routing and feedback state for switching between the list and editor surfaces.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesListPane.kt`
  List-first `Templates` catalog UI with the `New Template` CTA, active workshop banner, and card rendering.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt`
  Shared editor-shell UI with back navigation, header delete action, remix banner, form card, and version history.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenStateTest.kt`
  Pure state tests for create, edit, save-success, and list-return transitions.
- `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`
  Compose Desktop regression tests for list-first navigation, remix entry, save feedback, delete confirmation, and discard confirmation.

### Modify

- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModel.kt`
  Track editor baseline drafts, expose dirty-state checks, and reset cleanly after save or discard.
- `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt`
  Become a route shell that loads data, listens for remix, and chooses between the list and editor panes.
- `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModelTest.kt`
  Cover dirty-state tracking and post-save baseline resets.
- `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateListItemTest.kt`
  Update list-card expectations after delete is removed from the catalog card.

## Task 1: Add Screen Routing State And Dirty-Tracking Foundations

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenState.kt`
- Create: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenStateTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModel.kt`
- Modify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModelTest.kt`

- [ ] **Step 1: Add failing pure-state transition tests**

Create `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenStateTest.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemplatesScreenStateTest {
    @Test
    fun openCreate_switchesToEditorCreateMode_andClearsEditingId() {
        val state = TemplatesScreenState().openCreate()

        assertEquals(TemplatesSurface.Editor, state.surface)
        assertEquals(TemplateEditorMode.Create, state.editorMode)
        assertNull(state.editingTemplateId)
        assertNull(state.remixBanner)
    }

    @Test
    fun openEdit_switchesToEditorEditMode_andKeepsTemplateId() {
        val state = TemplatesScreenState().openEdit(templateId = 7L)

        assertEquals(TemplatesSurface.Editor, state.surface)
        assertEquals(TemplateEditorMode.Edit, state.editorMode)
        assertEquals(7L, state.editingTemplateId)
    }

    @Test
    fun onSaveSuccess_returnsToList_withHighlightAndFeedback() {
        val state = TemplatesScreenState(
            surface = TemplatesSurface.Editor,
            editorMode = TemplateEditorMode.Edit,
            editingTemplateId = 7L,
            remixBanner = "Board: Gothic Mystery v3",
        ).onSaveSuccess(
            savedTemplateId = 7L,
            message = "Template saved",
        )

        assertEquals(TemplatesSurface.List, state.surface)
        assertEquals(7L, state.highlightedTemplateId)
        assertEquals("Template saved", state.feedbackMessage)
        assertNull(state.remixBanner)
    }
}
```

- [ ] **Step 2: Add failing dirty-state coverage to the editor ViewModel tests**

Append these tests to `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModelTest.kt`:

```kotlin
    @Test
    fun hasUnsavedChanges_isFalseImmediatelyAfterLoadDraft_andTrueAfterEdit() {
        val viewModel = TemplateEditorViewModel()

        viewModel.loadDraft(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            )
        )

        assertEquals(false, viewModel.hasUnsavedChanges())

        viewModel.updatePremise("A broker chases a deeper debt")

        assertEquals(true, viewModel.hasUnsavedChanges())
    }

    @Test
    fun saveTemplate_withoutReset_refreshesDirtyBaselineToSavedDraft() = runTest {
        val viewModel = TemplateEditorViewModel()

        viewModel.loadDraft(
            TemplateDraft(
                title = "Noir Seoul",
                genre = "Urban Fantasy",
                premise = "A ghost broker solves debts",
                promptBlocks = listOf("Keep sensory detail high"),
            )
        )
        viewModel.updateTitle("Noir Seoul Revised")

        assertEquals(true, viewModel.hasUnsavedChanges())

        viewModel.saveTemplate(
            onSave = { draft ->
                Template(
                    id = 42,
                    title = draft.title,
                    genre = draft.genre,
                    premise = draft.premise,
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = draft.promptBlocks,
                    createdAtEpochMs = 1,
                    updatedAtEpochMs = 2,
                )
            },
            resetAfterSave = false,
        )

        assertEquals(false, viewModel.hasUnsavedChanges())
    }
```

- [ ] **Step 3: Run the tests to verify the new state APIs do not exist yet**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenStateTest" --tests "io.github.ringwdr.novelignite.features.templates.TemplateEditorViewModelTest"
```

Expected: FAIL because `TemplatesScreenState`, `TemplatesSurface`, `TemplateEditorMode`, and `TemplateEditorViewModel.hasUnsavedChanges()` do not exist yet.

- [ ] **Step 4: Implement the screen-state model and editor dirty tracking**

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenState.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

internal enum class TemplatesSurface {
    List,
    Editor,
}

internal enum class TemplateEditorMode {
    Create,
    Edit,
}

internal data class TemplatesScreenState(
    val surface: TemplatesSurface = TemplatesSurface.List,
    val editorMode: TemplateEditorMode = TemplateEditorMode.Create,
    val editingTemplateId: Long? = null,
    val remixBanner: String? = null,
    val highlightedTemplateId: Long? = null,
    val feedbackMessage: String? = null,
) {
    fun openCreate(remixBanner: String? = null): TemplatesScreenState = copy(
        surface = TemplatesSurface.Editor,
        editorMode = TemplateEditorMode.Create,
        editingTemplateId = null,
        remixBanner = remixBanner,
        feedbackMessage = null,
    )

    fun openEdit(templateId: Long): TemplatesScreenState = copy(
        surface = TemplatesSurface.Editor,
        editorMode = TemplateEditorMode.Edit,
        editingTemplateId = templateId,
        remixBanner = null,
        feedbackMessage = null,
    )

    fun returnToList(): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        remixBanner = null,
    )

    fun onSaveSuccess(savedTemplateId: Long, message: String): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        editingTemplateId = null,
        remixBanner = null,
        highlightedTemplateId = savedTemplateId,
        feedbackMessage = message,
    )

    fun onDeleteSuccess(message: String): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        editingTemplateId = null,
        remixBanner = null,
        highlightedTemplateId = null,
        feedbackMessage = message,
    )
}
```

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModel.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

data class TemplateDraft(
    val title: String = "",
    val genre: String = "",
    val premise: String = "",
    val promptBlocks: List<String> = emptyList(),
)

class TemplateEditorViewModel {
    val state = MutableStateFlow(TemplateEditorState())
    private var baselineDraft: TemplateDraft = TemplateDraft()

    fun loadTemplate(template: Template) {
        loadDraft(
            TemplateDraft(
                title = template.title,
                genre = template.genre,
                premise = template.premise,
                promptBlocks = template.promptBlocks,
            )
        )
    }

    fun loadDraft(draft: TemplateDraft) {
        baselineDraft = draft.normalized()
        state.value = TemplateEditorState(
            title = draft.title,
            genre = draft.genre,
            premise = draft.premise,
            promptBlocks = draft.promptBlocks,
        )
    }

    fun reset() {
        loadDraft(TemplateDraft())
    }

    fun hasUnsavedChanges(): Boolean = snapshotDraft().normalized() != baselineDraft

    fun updateTitle(value: String) {
        state.update { it.copy(title = value) }
    }

    fun updateGenre(value: String) {
        state.update { it.copy(genre = value) }
    }

    fun updatePremise(value: String) {
        state.update { it.copy(premise = value) }
    }

    fun addPromptBlock(value: String): Boolean {
        val sanitized = value.trim()
        if (sanitized.isBlank()) return false
        state.update { it.copy(promptBlocks = it.promptBlocks + sanitized) }
        return true
    }

    fun updatePromptBlock(index: Int, value: String) {
        state.update { current ->
            if (index !in current.promptBlocks.indices) return@update current
            val updatedBlocks = current.promptBlocks.toMutableList()
            updatedBlocks[index] = value
            current.copy(promptBlocks = updatedBlocks)
        }
    }

    fun removePromptBlock(index: Int) {
        state.update { current ->
            if (index !in current.promptBlocks.indices) return@update current
            current.copy(
                promptBlocks = current.promptBlocks.filterIndexed { currentIndex, _ ->
                    currentIndex != index
                }
            )
        }
    }

    fun applyEnrichedDraft(draft: TemplateDraft) {
        loadDraft(draft)
    }

    fun snapshotDraft(): TemplateDraft = state.value.toDraft()

    suspend fun saveTemplate(
        onSave: suspend (TemplateDraft) -> Template,
        resetAfterSave: Boolean = true,
    ): Template {
        val normalizedDraft = state.value.toDraft().normalized()
        val savedTemplate = onSave(normalizedDraft)
        if (resetAfterSave) {
            reset()
        } else {
            loadTemplate(savedTemplate)
        }
        return savedTemplate
    }
}

private fun TemplateEditorState.toDraft(): TemplateDraft = TemplateDraft(
    title = title,
    genre = genre,
    premise = premise,
    promptBlocks = promptBlocks,
)

private fun TemplateDraft.normalized(): TemplateDraft = TemplateDraft(
    title = title.trim(),
    genre = genre.trim(),
    premise = premise.trim(),
    promptBlocks = promptBlocks.map { it.trim() }.filter { it.isNotBlank() },
)
```

- [ ] **Step 5: Run the tests to verify the foundation passes**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenStateTest" --tests "io.github.ringwdr.novelignite.features.templates.TemplateEditorViewModelTest"
```

Expected: PASS with the new state-transition and dirty-tracking tests green.

- [ ] **Step 6: Commit the routing and dirty-state foundation**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenState.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModel.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenStateTest.kt composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModelTest.kt
git commit -m "feat: add templates screen state foundation"
```

## Task 2: Extract A List-First Catalog Pane And Remove Delete From List Cards

Note: This task is still a midpoint before the full Task 3 route split. While the extraction should stay focused, it may add narrow safety wiring in `TemplatesScreen.kt` if that is the smallest way to avoid temporary user-visible regressions. In particular, it is acceptable to keep `New Template` usable, to keep delete reachable somewhere outside the list card, and to pass live feedback/highlight state into `TemplatesListPane(...)` instead of hard-coded `null` when that preserves current behavior.

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesListPane.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt`
- Modify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateListItemTest.kt`

- [ ] **Step 1: Replace the old list-card test with the new catalog expectations**

Replace `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateListItemTest.kt` with:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import org.junit.Rule

class TemplateListItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersWorkshopSelectionAction_withoutDeleteAction() {
        rule.setContent {
            TemplateListItem(
                template = Template(
                    id = 7L,
                    title = "Noir Seoul",
                    genre = "Urban Fantasy",
                    premise = "A ghost broker solves debts",
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = listOf("Keep sensory detail high"),
                    createdAtEpochMs = 0L,
                    updatedAtEpochMs = 0L,
                ),
                isActive = true,
                isHighlighted = false,
                onOpenTemplate = {},
                onUseInWorkshop = {},
            )
        }

        rule.onNodeWithText("Active in Workshop").assertExists()
        rule.onNodeWithText("Selected for Workshop").assertExists()
        rule.onNodeWithText("Delete").assertDoesNotExist()
    }
}
```

- [ ] **Step 2: Run the list-card test to verify the old API still conflicts**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplateListItemTest"
```

Expected: FAIL because `TemplateListItem` still expects `isSelected` / `onDeleteTemplate` and still renders a `Delete` action.

- [ ] **Step 3: Create the dedicated list pane and move list-card rendering there**

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesListPane.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate

internal const val TEMPLATE_NEW_BUTTON_TAG = "template_new_button"
internal const val TEMPLATE_FEEDBACK_TAG = "template_feedback"

@Composable
internal fun TemplatesListPane(
    templates: List<Template>,
    activeTemplate: ActiveWorkshopTemplate?,
    highlightedTemplateId: Long?,
    feedbackMessage: String?,
    onCreateTemplate: () -> Unit,
    onOpenTemplate: (Template) -> Unit,
    onUseInWorkshop: (Template) -> Unit,
    onClearWorkshopTemplate: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Templates",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            Button(
                onClick = onCreateTemplate,
                modifier = Modifier.testTag(TEMPLATE_NEW_BUTTON_TAG),
            ) {
                Text("New Template")
            }
        }
        Text("Author reusable prompt blocks and remix them locally.")
        feedbackMessage?.let { message ->
            Text(
                text = message,
                modifier = Modifier.testTag(TEMPLATE_FEEDBACK_TAG),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }
        if (activeTemplate != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Workshop active: ${activeTemplate.title}",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                OutlinedButton(onClick = onClearWorkshopTemplate) {
                    Text("Clear")
                }
            }
        }
        if (templates.isEmpty()) {
            Text("No local templates yet. Create one to use in Workshop.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templates, key = Template::id) { template ->
                    TemplateListItem(
                        template = template,
                        isActive = activeTemplate?.id == template.id,
                        isHighlighted = highlightedTemplateId == template.id,
                        onOpenTemplate = { onOpenTemplate(template) },
                        onUseInWorkshop = { onUseInWorkshop(template) },
                    )
                }
            }
        }
    }
}

@Composable
internal fun TemplateListItem(
    template: Template,
    isActive: Boolean,
    isHighlighted: Boolean,
    onOpenTemplate: () -> Unit,
    onUseInWorkshop: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenTemplate),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = template.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = template.genre.ifBlank { "Uncategorized" },
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = template.premise.ifBlank { "No premise yet." },
                style = MaterialTheme.typography.bodySmall,
            )
            if (isActive) {
                Text(
                    text = "Active in Workshop",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            if (isHighlighted) {
                Text(
                    text = "Recently saved",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
            Button(
                onClick = onUseInWorkshop,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (isActive) "Selected for Workshop" else "Use in Workshop")
            }
        }
    }
}
```

Update the list portion of `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt` by deleting the in-file `TemplateListItem(...)` implementation and replacing the top-level list section with a `TemplatesListPane(...)` call. The minimum required wiring is:

```kotlin
        TemplatesListPane(
            templates = templates,
            activeTemplate = activeTemplate,
            highlightedTemplateId = null,
            feedbackMessage = null,
            onCreateTemplate = {},
            onOpenTemplate = { template ->
                selectedTemplateId = template.id
                detailTemplateEditorViewModel.loadTemplate(template)
                detailPromptBlockInput = TextFieldValue("")
                detailPromptBlockError = null
            },
            onUseInWorkshop = { template ->
                ActiveWorkshopTemplateStore.select(
                    ActiveWorkshopTemplate(id = template.id, title = template.title)
                )
            },
            onClearWorkshopTemplate = ActiveWorkshopTemplateStore::clear,
        )
```

Then remove the old `TemplateListItem(...)` function from the bottom of `TemplatesScreen.kt`.

If needed to preserve current behavior until Task 3 lands, the concrete wiring may also:

- use a meaningful `onCreateTemplate` callback instead of a no-op
- pass non-null `highlightedTemplateId` / `feedbackMessage`
- keep delete reachable elsewhere on the screen, as long as it does not return to the list card

- [ ] **Step 4: Run the list-card regression test**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplateListItemTest"
```

Expected: PASS with `rendersWorkshopSelectionAction_withoutDeleteAction`.

- [ ] **Step 5: Commit the catalog-pane extraction**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesListPane.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateListItemTest.kt
git commit -m "refactor: separate templates list pane"
```

## Task 3: Route Templates Between List And Shared Editor Surfaces

**Files:**
- Create: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt`
- Create: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt`

- [ ] **Step 1: Add failing screen-navigation tests for the new list/editor split**

Create `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateSelectionPersistence
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlin.test.AfterTest
import kotlin.test.Test
import org.junit.Rule

class TemplatesScreenTest {
    @get:Rule
    val rule = createComposeRule()

    @AfterTest
    fun tearDown() {
        TemplateRemixStore.clear()
        ActiveWorkshopTemplateStore.resetForTests()
    }

    @Test
    fun defaultsToList_andOpensCreateEditorFromHeaderButton() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Templates").assertExists()
        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()

        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).assertExists()
        rule.onNodeWithText("New Template").assertExists()
        rule.onNodeWithText("Noir Seoul").assertDoesNotExist()
    }

    @Test
    fun clickingListCard_opensSharedEditorInEditMode() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { listOf(sampleVersion()) },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Noir Seoul").performClick()

        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).assertExists()
        rule.onNodeWithText("Edit Template").assertExists()
        rule.onNodeWithText("Version history").assertExists()
    }

    @Test
    fun useInWorkshop_updatesBannerWithoutLeavingList() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { listOf(sampleTemplate()) },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Use in Workshop").performClick()

        rule.onNodeWithText("Workshop active: Noir Seoul").assertExists()
        rule.onNodeWithText("Templates").assertExists()
    }
}

private object NoOpPersistence : ActiveWorkshopTemplateSelectionPersistence {
    override fun load() = null
    override fun save(selection: io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate?) = Unit
}

private fun sampleTemplate(
    id: Long = 7L,
    title: String = "Noir Seoul",
    genre: String = "Urban Fantasy",
    premise: String = "A ghost broker solves debts",
    promptBlocks: List<String> = listOf("Keep sensory detail high"),
): Template = Template(
    id = id,
    title = title,
    genre = genre,
    premise = premise,
    worldSetting = "",
    characterCards = "",
    relationshipNotes = "",
    toneStyle = "",
    bannedElements = "",
    plotConstraints = "",
    openingHook = "",
    promptBlocks = promptBlocks,
    createdAtEpochMs = 1L,
    updatedAtEpochMs = 2L,
)

private fun sampleVersion(): TemplateVersion = TemplateVersion(
    id = 10L,
    templateId = 7L,
    versionNumber = 1L,
    title = "Noir Seoul",
    genre = "Urban Fantasy",
    premise = "A ghost broker solves debts",
    worldSetting = "",
    characterCards = "",
    relationshipNotes = "",
    toneStyle = "",
    bannedElements = "",
    plotConstraints = "",
    openingHook = "",
    promptBlocks = listOf("Keep sensory detail high"),
    createdAtEpochMs = 1L,
)
```

- [ ] **Step 2: Run the screen test to verify the current mixed screen still fails the new UX contract**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenTest"
```

Expected: FAIL because `TemplatesScreen` does not expose internal list/editor routing yet and there is no `TEMPLATE_BACK_BUTTON_TAG`.

- [ ] **Step 3: Build the shared editor pane and route the screen through list vs editor**

Create `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt`:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.TemplateVersion

internal const val TEMPLATE_BACK_BUTTON_TAG = "template_back_button"
internal const val TEMPLATE_DELETE_BUTTON_TAG = "template_delete_button"

@Composable
internal fun TemplateEditorPane(
    mode: TemplateEditorMode,
    editorState: TemplateEditorState,
    promptBlockInput: TextFieldValue,
    promptBlockErrorMessage: String?,
    remixBanner: String?,
    versions: List<TemplateVersion>,
    isEnriching: Boolean,
    enrichErrorMessage: String?,
    showDeleteAction: Boolean,
    onBack: () -> Unit,
    onDelete: () -> Unit,
    onTitleChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onPremiseChange: (String) -> Unit,
    onPromptBlockInputChange: (TextFieldValue) -> Unit,
    onAddPromptBlock: suspend () -> Unit,
    onPromptBlockChange: (Int, String) -> Unit,
    onRemovePromptBlock: (Int) -> Unit,
    onSaveTemplate: () -> Unit,
    onEnrichTemplate: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.testTag(TEMPLATE_BACK_BUTTON_TAG),
            ) {
                Text("Back to Templates")
            }
            if (showDeleteAction) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(TEMPLATE_DELETE_BUTTON_TAG),
                ) {
                    Text("Delete")
                }
            }
        }
        Text(
            text = if (mode == TemplateEditorMode.Create) "New Template" else "Edit Template",
            fontWeight = FontWeight.SemiBold,
        )
        Text("Templates act as reusable story modes for Workshop.")
        remixBanner?.let { source ->
            Text(
                text = "Remix from $source",
                fontWeight = FontWeight.Medium,
            )
        }
        TemplateEditorCard(
            title = "Template details",
            saveLabel = if (mode == TemplateEditorMode.Create) "Save Template" else "Save Changes",
            state = editorState,
            promptBlockInput = promptBlockInput,
            promptBlockErrorMessage = promptBlockErrorMessage,
            onTitleChange = onTitleChange,
            onGenreChange = onGenreChange,
            onPremiseChange = onPremiseChange,
            onPromptBlockInputChange = onPromptBlockInputChange,
            onAddPromptBlock = onAddPromptBlock,
            onPromptBlockChange = onPromptBlockChange,
            onRemovePromptBlock = onRemovePromptBlock,
            onSaveTemplate = onSaveTemplate,
            onEnrichTemplate = onEnrichTemplate,
            isEnriching = isEnriching,
            enrichErrorMessage = enrichErrorMessage,
        )
        if (mode == TemplateEditorMode.Edit) {
            androidx.compose.material3.Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Version history", fontWeight = FontWeight.SemiBold)
                    if (versions.isEmpty()) {
                        Text("No saved versions yet.")
                    } else {
                        Text("${versions.size} saved snapshots")
                        versions.take(3).forEach { version ->
                            Text("v${version.versionNumber} · ${version.title}")
                        }
                    }
                }
            }
        }
    }
}
```

Replace the entire contents of `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt` with:

```kotlin
package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplatesScreen(
    loadTemplates: () -> List<Template> = ::loadLocalTemplates,
    loadTemplateVersions: (Long) -> List<TemplateVersion> = ::loadLocalTemplateVersions,
    saveTemplate: suspend (
        draft: TemplateDraft,
        templateId: Long?,
        originalTemplate: Template?,
        originalVersion: TemplateVersion?,
    ) -> Template = { draft, templateId, originalTemplate, originalVersion ->
        saveLocalTemplate(
            draft = draft,
            templateId = templateId,
            originalTemplate = originalTemplate,
            originalVersion = originalVersion,
        )
    },
    deleteTemplate: suspend (Long) -> Unit = ::deleteLocalTemplate,
    enrichTemplate: suspend (TemplateDraft) -> TemplateDraft = ::enrichTemplateDraft,
) {
    var templates by remember { mutableStateOf(loadTemplates()) }
    var screenState by remember { mutableStateOf(TemplatesScreenState()) }
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()
    val editorViewModel = remember { TemplateEditorViewModel() }
    val editorState by editorViewModel.state.collectAsState()
    val remixSelection by TemplateRemixStore.selection.collectAsState()
    val scope = rememberCoroutineScope()

    var promptBlockInput by remember { mutableStateOf(TextFieldValue("")) }
    var promptBlockError by remember { mutableStateOf<String?>(null) }
    var enrichError by remember { mutableStateOf<String?>(null) }
    var isEnriching by remember { mutableStateOf(false) }
    var remixSourceVersion by remember { mutableStateOf<TemplateVersion?>(null) }
    var selectedTemplateVersions by remember { mutableStateOf<List<TemplateVersion>>(emptyList()) }

    val selectedTemplate = screenState.editingTemplateId?.let { templateId ->
        templates.firstOrNull { it.id == templateId }
    }
    val selectedTemplateVersionKey = selectedTemplate?.let { template -> template.id to template.updatedAtEpochMs }

    fun clearTransientEditorState() {
        promptBlockInput = TextFieldValue("")
        promptBlockError = null
        enrichError = null
        isEnriching = false
        remixSourceVersion = null
        selectedTemplateVersions = emptyList()
    }

    fun openCreateEditor(remixBanner: String? = null) {
        editorViewModel.reset()
        clearTransientEditorState()
        screenState = screenState.openCreate(remixBanner = remixBanner)
    }

    fun openEditEditor(template: Template) {
        editorViewModel.loadTemplate(template)
        clearTransientEditorState()
        screenState = screenState.openEdit(template.id)
    }

    fun returnToList() {
        editorViewModel.reset()
        clearTransientEditorState()
        screenState = screenState.returnToList()
    }

    LaunchedEffect(selectedTemplateVersionKey) {
        selectedTemplateVersions = selectedTemplate?.let { template ->
            withContext(Dispatchers.Default) {
                loadTemplateVersions(template.id)
            }
        }.orEmpty()
    }

    LaunchedEffect(remixSelection) {
        val selection = remixSelection ?: return@LaunchedEffect
        editorViewModel.loadDraft(selection.draft)
        clearTransientEditorState()
        remixSourceVersion = selection.sourceVersion
        screenState = screenState.openCreate(remixBanner = selection.sourceLabel)
        TemplateRemixStore.clear()
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (screenState.surface) {
            TemplatesSurface.List -> {
                TemplatesListPane(
                    templates = templates,
                    activeTemplate = activeTemplate,
                    highlightedTemplateId = screenState.highlightedTemplateId,
                    feedbackMessage = screenState.feedbackMessage,
                    onCreateTemplate = { openCreateEditor() },
                    onOpenTemplate = ::openEditEditor,
                    onUseInWorkshop = { template ->
                        ActiveWorkshopTemplateStore.select(
                            ActiveWorkshopTemplate(id = template.id, title = template.title)
                        )
                    },
                    onClearWorkshopTemplate = ActiveWorkshopTemplateStore::clear,
                )
            }

            TemplatesSurface.Editor -> {
                TemplateEditorPane(
                    mode = screenState.editorMode,
                    editorState = editorState,
                    promptBlockInput = promptBlockInput,
                    promptBlockErrorMessage = promptBlockError,
                    remixBanner = screenState.remixBanner,
                    versions = selectedTemplateVersions,
                    isEnriching = isEnriching,
                    enrichErrorMessage = enrichError,
                    showDeleteAction = false,
                    onBack = ::returnToList,
                    onDelete = {},
                    onTitleChange = editorViewModel::updateTitle,
                    onGenreChange = editorViewModel::updateGenre,
                    onPremiseChange = editorViewModel::updatePremise,
                    onPromptBlockInputChange = {
                        promptBlockInput = it
                        promptBlockError = null
                    },
                    onAddPromptBlock = {
                        val added = editorViewModel.addPromptBlock(promptBlockInput.text)
                        if (added) {
                            promptBlockInput = TextFieldValue("")
                            promptBlockError = null
                        } else {
                            promptBlockError = "Add a prompt block before continuing."
                        }
                    },
                    onPromptBlockChange = editorViewModel::updatePromptBlock,
                    onRemovePromptBlock = editorViewModel::removePromptBlock,
                    onSaveTemplate = {
                        scope.launch {
                            val savedTemplate = editorViewModel.saveTemplate(
                                onSave = { draft ->
                                    saveTemplate(
                                        draft,
                                        screenState.editingTemplateId,
                                        selectedTemplate,
                                        remixSourceVersion,
                                    )
                                },
                            )
                            templates = loadTemplates()
                            if (activeTemplate?.id == savedTemplate.id) {
                                ActiveWorkshopTemplateStore.select(
                                    ActiveWorkshopTemplate(
                                        id = savedTemplate.id,
                                        title = savedTemplate.title,
                                    )
                                )
                            }
                            clearTransientEditorState()
                            screenState = screenState.onSaveSuccess(
                                savedTemplateId = savedTemplate.id,
                                message = "Template saved",
                            )
                        }
                    },
                    onEnrichTemplate = {
                        scope.launch {
                            isEnriching = true
                            enrichError = null
                            runCatching {
                                enrichTemplate(editorViewModel.snapshotDraft())
                            }.onSuccess { enriched ->
                                editorViewModel.applyEnrichedDraft(enriched)
                            }.onFailure { error ->
                                enrichError = error.message ?: "Template enrichment failed."
                            }
                            isEnriching = false
                        }
                    },
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run the new screen-navigation regression tests**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenTest"
```

Expected: PASS with:
- `defaultsToList_andOpensCreateEditorFromHeaderButton`
- `clickingListCard_opensSharedEditorInEditMode`
- `useInWorkshop_updatesBannerWithoutLeavingList`

- [ ] **Step 5: Commit the list/editor route split**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt
git commit -m "feat: split templates list and editor flows"
```

## Task 4: Add Save Feedback, Remix Entry, Delete Confirmation, And Discard Confirmation

**Files:**
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt`
- Modify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt`
- Modify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`

- [ ] **Step 1: Expand the screen test with the high-risk UX flows**

Append these tests to `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`:

```kotlin
    @Test
    fun remixSelection_opensCreateEditor_withSourceBanner() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        TemplateRemixStore.select(
            TemplateRemixSelection(
                draft = TemplateDraft(
                    title = "Gothic Mystery",
                    genre = "Mystery",
                    premise = "A locked tower refuses to stay silent",
                    promptBlocks = listOf("Keep every reveal eerie"),
                ),
                sourceLabel = "Board: Gothic Mystery v3",
            )
        )

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { emptyList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Remix from Board: Gothic Mystery v3").assertExists()
        rule.onNodeWithText("New Template").assertExists()
    }

    @Test
    fun saveTemplate_returnsToList_withFeedbackAndHighlight() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        val templates = mutableListOf(sampleTemplate())

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { templates.toList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, templateId, _, _ ->
                    val saved = sampleTemplate(
                        id = templateId ?: 99L,
                        title = draft.title,
                        genre = draft.genre,
                        premise = draft.premise,
                        promptBlocks = draft.promptBlocks,
                    )
                    templates.removeAll { it.id == saved.id }
                    templates += saved
                    saved
                },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()
        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG).performTextInput("Moon Archive")
        rule.onNodeWithTag(TEMPLATE_GENRE_FIELD_TAG).performTextInput("Fantasy")
        rule.onNodeWithTag(TEMPLATE_PREMISE_FIELD_TAG).performTextInput("A moon archivist wakes an old debt.")
        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).performTextInput("Keep the tone haunted")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Save Template").performClick()

        rule.onNodeWithText("Template saved").assertExists()
        rule.onNodeWithText("Moon Archive").assertExists()
        rule.onNodeWithText("Recently saved").assertExists()
    }

    @Test
    fun deleteFlow_confirmsBeforeRemovingTemplate_andClearsWorkshopSelection() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)
        val templates = mutableListOf(sampleTemplate())
        ActiveWorkshopTemplateStore.select(
            io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate(
                id = 7L,
                title = "Noir Seoul",
            )
        )

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { templates.toList() },
                loadTemplateVersions = { listOf(sampleVersion()) },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 7L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = { templateId ->
                    templates.removeAll { it.id == templateId }
                },
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithText("Noir Seoul").performClick()
        rule.onNodeWithTag(TEMPLATE_DELETE_BUTTON_TAG).performClick()
        rule.onNodeWithText("Delete template?").assertExists()
        rule.onNodeWithText("'Noir Seoul' will be removed. If it is active in Workshop, that selection will also be cleared.").assertExists()
        rule.onNodeWithText("Delete").performClick()

        rule.onNodeWithText("Template deleted").assertExists()
        rule.onNodeWithText("Noir Seoul").assertDoesNotExist()
        rule.onNodeWithText("Workshop active: Noir Seoul").assertDoesNotExist()
    }

    @Test
    fun backFromDirtyEditor_showsDiscardDialog_beforeReturningToList() {
        ActiveWorkshopTemplateStore.configure(NoOpPersistence)

        rule.setContent {
            TemplatesScreen(
                loadTemplates = { emptyList() },
                loadTemplateVersions = { emptyList() },
                saveTemplate = { draft, _, _, _ -> sampleTemplate(id = 99L, title = draft.title, genre = draft.genre, premise = draft.premise, promptBlocks = draft.promptBlocks) },
                deleteTemplate = {},
                enrichTemplate = { it },
            )
        }

        rule.onNodeWithTag(TEMPLATE_NEW_BUTTON_TAG).performClick()
        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG).performTextInput("Moon Archive")
        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).performClick()

        rule.onNodeWithText("Discard changes?").assertExists()
        rule.onNodeWithText("Keep editing").performClick()
        rule.onNodeWithText("New Template").assertExists()
        rule.onNodeWithTag(TEMPLATE_BACK_BUTTON_TAG).performClick()
        rule.onNodeWithText("Discard changes").performClick()
        rule.onNodeWithText("Templates").assertExists()
    }
```

- [ ] **Step 2: Run the expanded screen tests to verify the critical flows are still missing**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenTest"
```

Expected: FAIL because delete confirmation, discard confirmation, remix banner copy, and saved-card feedback/highlight are not all wired yet.

- [ ] **Step 3: Wire delete and discard confirmation dialogs plus final feedback behavior**

Update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt` so the delete button is shown only in edit mode:

```kotlin
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.testTag(TEMPLATE_BACK_BUTTON_TAG),
            ) {
                Text("Back to Templates")
            }
            if (mode == TemplateEditorMode.Edit && showDeleteAction) {
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag(TEMPLATE_DELETE_BUTTON_TAG),
                ) {
                    Text("Delete")
                }
            }
        }
```

Then update `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt` with these focused changes.

Add dialog state near the other remembered variables:

```kotlin
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
```

Replace `returnToList()` with:

```kotlin
    fun forceReturnToList() {
        editorViewModel.reset()
        clearTransientEditorState()
        screenState = screenState.returnToList()
    }

    fun requestReturnToList() {
        if (editorViewModel.hasUnsavedChanges()) {
            showDiscardDialog = true
        } else {
            forceReturnToList()
        }
    }
```

Update the editor-pane call:

```kotlin
                TemplateEditorPane(
                    mode = screenState.editorMode,
                    editorState = editorState,
                    promptBlockInput = promptBlockInput,
                    promptBlockErrorMessage = promptBlockError,
                    remixBanner = screenState.remixBanner,
                    versions = selectedTemplateVersions,
                    isEnriching = isEnriching,
                    enrichErrorMessage = enrichError,
                    showDeleteAction = true,
                    onBack = ::requestReturnToList,
                    onDelete = { showDeleteDialog = true },
                    onTitleChange = editorViewModel::updateTitle,
                    onGenreChange = editorViewModel::updateGenre,
                    onPremiseChange = editorViewModel::updatePremise,
                    onPromptBlockInputChange = {
                        promptBlockInput = it
                        promptBlockError = null
                    },
                    onAddPromptBlock = {
                        val added = editorViewModel.addPromptBlock(promptBlockInput.text)
                        if (added) {
                            promptBlockInput = TextFieldValue("")
                            promptBlockError = null
                        } else {
                            promptBlockError = "Add a prompt block before continuing."
                        }
                    },
                    onPromptBlockChange = editorViewModel::updatePromptBlock,
                    onRemovePromptBlock = editorViewModel::removePromptBlock,
                    onSaveTemplate = {
                        scope.launch {
                            val savedTemplate = editorViewModel.saveTemplate(
                                onSave = { draft ->
                                    saveTemplate(
                                        draft,
                                        screenState.editingTemplateId,
                                        selectedTemplate,
                                        remixSourceVersion,
                                    )
                                },
                            )
                            templates = loadTemplates()
                            if (activeTemplate?.id == savedTemplate.id) {
                                ActiveWorkshopTemplateStore.select(
                                    ActiveWorkshopTemplate(
                                        id = savedTemplate.id,
                                        title = savedTemplate.title,
                                    )
                                )
                            }
                            clearTransientEditorState()
                            screenState = screenState.onSaveSuccess(
                                savedTemplateId = savedTemplate.id,
                                message = "Template saved",
                            )
                        }
                    },
                    onEnrichTemplate = {
                        scope.launch {
                            isEnriching = true
                            enrichError = null
                            runCatching {
                                enrichTemplate(editorViewModel.snapshotDraft())
                            }.onSuccess { enriched ->
                                editorViewModel.applyEnrichedDraft(enriched)
                            }.onFailure { error ->
                                enrichError = error.message ?: "Template enrichment failed."
                            }
                            isEnriching = false
                        }
                    },
                )
```

At the bottom of the root `Column`, add the two dialogs:

```kotlin
        if (showDeleteDialog && selectedTemplate != null) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete template?") },
                text = {
                    Text(
                        "'${selectedTemplate.title}' will be removed. If it is active in Workshop, that selection will also be cleared."
                    )
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                deleteTemplate(selectedTemplate.id)
                                templates = loadTemplates()
                                if (activeTemplate?.id == selectedTemplate.id) {
                                    ActiveWorkshopTemplateStore.clear()
                                }
                                showDeleteDialog = false
                                forceReturnToList()
                                screenState = screenState.onDeleteSuccess("Template deleted")
                            }
                        },
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
            )
        }

        if (showDiscardDialog) {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = { showDiscardDialog = false },
                title = { Text("Discard changes?") },
                text = {
                    Text("Your draft edits are not saved yet.")
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            showDiscardDialog = false
                            forceReturnToList()
                        },
                    ) {
                        Text("Discard changes")
                    }
                },
                dismissButton = {
                    OutlinedButton(onClick = { showDiscardDialog = false }) {
                        Text("Keep editing")
                    }
                },
            )
        }
```

- [ ] **Step 4: Run the expanded screen regression suite**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenTest"
```

Expected: PASS with all of these green:
- `defaultsToList_andOpensCreateEditorFromHeaderButton`
- `clickingListCard_opensSharedEditorInEditMode`
- `useInWorkshop_updatesBannerWithoutLeavingList`
- `remixSelection_opensCreateEditor_withSourceBanner`
- `saveTemplate_returnsToList_withFeedbackAndHighlight`
- `deleteFlow_confirmsBeforeRemovingTemplate_andClearsWorkshopSelection`
- `backFromDirtyEditor_showsDiscardDialog_beforeReturningToList`

- [ ] **Step 5: Commit the high-risk templates UX flows**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt
git commit -m "feat: harden templates editor navigation"
```

## Task 5: Run Narrow Regressions And Final JVM Verification

**Files:**
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreen.kt`
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesListPane.kt`
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorPane.kt`
- Verify: `composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModel.kt`
- Verify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenStateTest.kt`
- Verify: `composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateEditorViewModelTest.kt`
- Verify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplateListItemTest.kt`
- Verify: `composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates/TemplatesScreenTest.kt`

- [ ] **Step 1: Run the focused templates regression suite**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenStateTest" --tests "io.github.ringwdr.novelignite.features.templates.TemplateEditorViewModelTest" --tests "io.github.ringwdr.novelignite.features.templates.TemplateListItemTest" --tests "io.github.ringwdr.novelignite.features.templates.TemplatesScreenTest"
```

Expected: PASS with all templates-specific tests green.

- [ ] **Step 2: Run the broader JVM suite for shared and desktop regressions**

Run:

```bash
.\gradlew.bat :composeApp:jvmTest
```

Expected: PASS.

- [ ] **Step 3: Record the environment-limited verification boundary**

Note in the implementation summary:

```text
Desktop and common-template coverage is verified through :composeApp:jvmTest. If :composeApp:allTests is blocked by missing Android SDK configuration in this worktree, call that out explicitly instead of guessing.
```

- [ ] **Step 4: Only continue to the final commit if verification stays unchanged**

If any focused or full JVM regression fails, stop and return to the task that owns the failing file. Do not paper over verification failures inside this wrap-up task.

- [ ] **Step 5: Commit the verified templates UX restructure**

```bash
git add composeApp/src/commonMain/kotlin/io/github/ringwdr/novelignite/features/templates composeApp/src/commonTest/kotlin/io/github/ringwdr/novelignite/features/templates composeApp/src/desktopTest/kotlin/io/github/ringwdr/novelignite/features/templates
git commit -m "feat: separate templates list and editor ux"
```

## Self-Review

### Spec coverage

- List-first `Templates` default entry point: covered by Tasks 2, 3, and 4.
- Separate create/edit surface with one shared editor layout: covered by Task 3.
- Card click opens editor while `Use in Workshop` stays on the list: covered by Tasks 2 and 3.
- Remix enters the editor directly with source context: covered by Task 4.
- Save returns to list with feedback and highlight: covered by Tasks 1, 3, and 4.
- Delete moves to the editor header with confirmation and workshop deselection: covered by Task 4.
- Back from dirty editor asks for confirmation: covered by Tasks 1 and 4.
- JVM-focused verification aligned to current repo guidance: covered by Task 5.

No uncovered requirements remain for the approved templates UX scope.

### Placeholder scan

- No `TODO`, `TBD`, or “implement later” placeholders remain.
- Every task includes exact file paths, code blocks, commands, and expected outcomes.

### Type consistency

- `TemplatesScreenState`, `TemplatesSurface`, and `TemplateEditorMode` are introduced once in Task 1 and reused consistently.
- `TemplateEditorViewModel.reset()` and `hasUnsavedChanges()` are introduced in Task 1 and reused consistently in Tasks 3 and 4.
- `TEMPLATE_NEW_BUTTON_TAG`, `TEMPLATE_BACK_BUTTON_TAG`, and `TEMPLATE_DELETE_BUTTON_TAG` are introduced with the pane files and reused consistently in screen tests.
