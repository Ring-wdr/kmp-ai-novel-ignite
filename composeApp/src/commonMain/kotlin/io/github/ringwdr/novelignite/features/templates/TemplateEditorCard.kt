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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
internal fun TemplateEditorCard(
    title: String,
    saveLabel: String,
    state: TemplateEditorState,
    promptBlockInput: String,
    onTitleChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onPremiseChange: (String) -> Unit,
    onPromptBlockInputChange: (String) -> Unit,
    onAddPromptBlock: () -> Unit,
    onPromptBlockChange: (Int, String) -> Unit,
    onRemovePromptBlock: (Int) -> Unit,
    onSaveTemplate: () -> Unit,
) {
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
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.genre,
                onValueChange = onGenreChange,
                label = { Text("Genre") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.premise,
                onValueChange = onPremiseChange,
                label = { Text("Premise") },
                modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.weight(1f),
                )
                Button(onClick = onAddPromptBlock) {
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
                                modifier = Modifier.weight(1f),
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
