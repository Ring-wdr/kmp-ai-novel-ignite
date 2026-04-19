package io.github.ringwdr.novelignite.features.templates

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class TemplateEditorCardTest {
    @get:Rule
    val rule = createComposeRule()

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun titleField_movesFocusToGenre_onTab() {
        rule.setContent {
            LocalInputModeManager.current.requestInputMode(InputMode.Keyboard)
            val state = remember {
                mutableStateOf(
                    TemplateEditorState(
                        title = "Noir Seoul",
                        genre = "Mystery",
                        premise = "A detective follows a hidden archive.",
                    )
                )
            }
            val promptInput = remember { mutableStateOf(TextFieldValue("")) }

            TemplateEditorCard(
                title = "New Template",
                saveLabel = "Save",
                state = state.value,
                promptBlockInput = promptInput.value,
                promptBlockErrorMessage = null,
                onTitleChange = { state.value = state.value.copy(title = it) },
                onGenreChange = { state.value = state.value.copy(genre = it) },
                onPremiseChange = { state.value = state.value.copy(premise = it) },
                onPromptBlockInputChange = { promptInput.value = it },
                onAddPromptBlock = {},
                onPromptBlockChange = { _, _ -> },
                onRemovePromptBlock = {},
                onSaveTemplate = {},
            )
        }

        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG)
            .requestFocus()
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }

        rule.onNodeWithTag(TEMPLATE_GENRE_FIELD_TAG).assertIsFocused()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun premiseField_movesFocusToPromptInput_withoutMutatingText_onTab() {
        lateinit var premiseText: String

        rule.setContent {
            LocalInputModeManager.current.requestInputMode(InputMode.Keyboard)
            val state = remember {
                mutableStateOf(
                    TemplateEditorState(
                        title = "Noir Seoul",
                        genre = "Mystery",
                        premise = "Keep this premise intact.",
                    )
                )
            }
            val promptInput = remember { mutableStateOf(TextFieldValue("")) }
            premiseText = state.value.premise

            TemplateEditorCard(
                title = "New Template",
                saveLabel = "Save",
                state = state.value,
                promptBlockInput = promptInput.value,
                promptBlockErrorMessage = null,
                onTitleChange = { state.value = state.value.copy(title = it) },
                onGenreChange = { state.value = state.value.copy(genre = it) },
                onPremiseChange = {
                    premiseText = it
                    state.value = state.value.copy(premise = it)
                },
                onPromptBlockInputChange = { promptInput.value = it },
                onAddPromptBlock = {},
                onPromptBlockChange = { _, _ -> },
                onRemovePromptBlock = {},
                onSaveTemplate = {},
            )
        }

        rule.onNodeWithTag(TEMPLATE_PREMISE_FIELD_TAG)
            .requestFocus()
            .performKeyInput { pressKey(androidx.compose.ui.input.key.Key.Tab) }

        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).assertIsFocused()
        rule.runOnIdle {
            assertEquals("Keep this premise intact.", premiseText)
        }
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun addButton_appendsPromptBlock_andClearsInput() {
        lateinit var promptText: String

        rule.setContent {
            LocalInputModeManager.current.requestInputMode(InputMode.Keyboard)
            val viewModel = remember { TemplateEditorViewModel() }
            val state by viewModel.state.collectAsState()
            var promptBlockInput by remember { mutableStateOf(TextFieldValue("")) }
            promptText = promptBlockInput.text

            TemplateEditorCard(
                title = "New Template",
                saveLabel = "Save",
                state = state,
                promptBlockInput = promptBlockInput,
                promptBlockErrorMessage = null,
                onTitleChange = viewModel::updateTitle,
                onGenreChange = viewModel::updateGenre,
                onPremiseChange = viewModel::updatePremise,
                onPromptBlockInputChange = {
                    promptBlockInput = it
                    promptText = it.text
                },
                onAddPromptBlock = {
                    if (viewModel.addPromptBlock(promptBlockInput.text)) {
                        promptBlockInput = TextFieldValue("")
                        promptText = ""
                    }
                },
                onPromptBlockChange = viewModel::updatePromptBlock,
                onRemovePromptBlock = viewModel::removePromptBlock,
                onSaveTemplate = {},
            )
        }

        rule.onNodeWithTag(TEMPLATE_TITLE_FIELD_TAG).performTextInput("Noir Seoul")
        rule.onNodeWithTag(TEMPLATE_GENRE_FIELD_TAG).performTextInput("Mystery")
        rule.onNodeWithTag(TEMPLATE_PREMISE_FIELD_TAG).performTextInput("Keep this premise intact.")
        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).performTextInput("Keep sensory detail high")

        rule.onNodeWithText("Add").performClick()

        rule.onNodeWithText("Prompt block 1").assertExists()
        rule.onNodeWithText("Keep sensory detail high").assertExists()
        rule.runOnIdle {
            assertEquals("", promptText)
        }

        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).performTextInput("Keep dialogue sharp")
        rule.onNodeWithText("Add").performClick()
        rule.onNodeWithText("Prompt block 2").assertExists()
        rule.onNodeWithText("Keep dialogue sharp").assertExists()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun addButton_keepsInput_andShowsError_whenAddFails() {
        lateinit var promptText: String

        rule.setContent {
            LocalInputModeManager.current.requestInputMode(InputMode.Keyboard)
            val viewModel = remember { TemplateEditorViewModel() }
            val state by viewModel.state.collectAsState()
            var promptBlockInput by remember { mutableStateOf(TextFieldValue("")) }
            var errorMessage by remember { mutableStateOf<String?>(null) }
            promptText = promptBlockInput.text

            TemplateEditorCard(
                title = "New Template",
                saveLabel = "Save",
                state = state,
                promptBlockInput = promptBlockInput,
                promptBlockErrorMessage = errorMessage,
                onTitleChange = viewModel::updateTitle,
                onGenreChange = viewModel::updateGenre,
                onPremiseChange = viewModel::updatePremise,
                onPromptBlockInputChange = {
                    promptBlockInput = it
                    promptText = it.text
                    errorMessage = null
                },
                onAddPromptBlock = {
                    if (viewModel.addPromptBlock(promptBlockInput.text)) {
                        promptBlockInput = TextFieldValue("")
                        promptText = ""
                        errorMessage = null
                    } else {
                        errorMessage = "Add a prompt block before continuing."
                    }
                },
                onPromptBlockChange = viewModel::updatePromptBlock,
                onRemovePromptBlock = viewModel::removePromptBlock,
                onSaveTemplate = {},
            )
        }

        rule.onNodeWithTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG).performTextInput("   ")
        rule.onNodeWithText("Add").performClick()

        rule.onNodeWithText("Add a prompt block before continuing.").assertExists()
        rule.runOnIdle {
            assertEquals("   ", promptText)
        }
    }
}
