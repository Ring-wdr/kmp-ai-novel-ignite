package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

internal const val TEMPLATE_TITLE_FIELD_TAG = "template_title_field"
internal const val TEMPLATE_GENRE_FIELD_TAG = "template_genre_field"
internal const val TEMPLATE_PREMISE_FIELD_TAG = "template_premise_field"
internal const val TEMPLATE_PROMPT_INPUT_FIELD_TAG = "template_prompt_input_field"

@Composable
internal fun TemplateEditorCard(
    title: String,
    saveLabel: String,
    state: TemplateEditorState,
    promptBlockInput: TextFieldValue,
    promptBlockErrorMessage: String?,
    onTitleChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onPremiseChange: (String) -> Unit,
    onPromptBlockInputChange: (TextFieldValue) -> Unit,
    onAddPromptBlock: suspend () -> Unit,
    onPromptBlockChange: (Int, String) -> Unit,
    onRemovePromptBlock: (Int) -> Unit,
    onSaveTemplate: () -> Unit,
) {
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text("Title") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEMPLATE_TITLE_FIELD_TAG)
                    .moveFocusOnTab(focusManager),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.genre,
                onValueChange = onGenreChange,
                label = { Text("Genre") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEMPLATE_GENRE_FIELD_TAG)
                    .moveFocusOnTab(focusManager),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.premise,
                onValueChange = onPremiseChange,
                label = { Text("Premise") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(TEMPLATE_PREMISE_FIELD_TAG)
                    .moveFocusOnTab(focusManager),
                minLines = 3,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = promptBlockInput,
                    onValueChange = onPromptBlockInputChange,
                    label = { Text("Prompt block") },
                    modifier = Modifier
                        .weight(1f)
                        .testTag(TEMPLATE_PROMPT_INPUT_FIELD_TAG)
                        .moveFocusOnTab(focusManager),
                    isError = promptBlockErrorMessage != null,
                    singleLine = true,
                    supportingText = promptBlockErrorMessage?.let { message ->
                        { Text(message) }
                    },
                )
                Button(
                    onClick = {
                        scope.launch {
                            focusManager.clearFocus(force = true)
                            yield()
                            onAddPromptBlock()
                        }
                    },
                ) {
                    Text("Add")
                }
            }
            if (state.promptBlocks.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    state.promptBlocks.forEachIndexed { index, block ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedTextField(
                                value = block,
                                onValueChange = { onPromptBlockChange(index, it) },
                                label = { Text("Prompt block ${index + 1}") },
                                modifier = Modifier
                                    .weight(1f)
                                    .moveFocusOnTab(focusManager),
                                singleLine = true,
                            )
                            OutlinedButton(onClick = { onRemovePromptBlock(index) }) {
                                Text("Remove")
                            }
                        }
                    }
                }
            }
            Button(
                onClick = onSaveTemplate,
                enabled = state.title.isNotBlank() &&
                    state.genre.isNotBlank() &&
                    state.premise.isNotBlank() &&
                    state.promptBlocks.isNotEmpty() &&
                    state.promptBlocks.all { it.trim().isNotBlank() },
            ) {
                Text(saveLabel)
            }
        }
    }
}

private fun Modifier.moveFocusOnTab(focusManager: FocusManager): Modifier = onPreviewKeyEvent { event ->
    if (event.type != KeyEventType.KeyDown || event.key != Key.Tab) {
        return@onPreviewKeyEvent false
    }

    focusManager.moveFocus(
        if (event.isShiftPressed) FocusDirection.Previous else FocusDirection.Next
    )
    true
}
