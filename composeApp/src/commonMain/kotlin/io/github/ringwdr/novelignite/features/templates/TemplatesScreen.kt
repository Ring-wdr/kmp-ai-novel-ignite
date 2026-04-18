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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlinx.coroutines.launch

@Composable
fun TemplatesScreen() {
    var templates by remember { mutableStateOf(loadLocalTemplates()) }
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()
    val editorViewModel = remember { TemplateEditorViewModel() }
    val editorState by editorViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var promptBlockInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Templates",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text("Author reusable prompt blocks and remix them locally.")

        if (activeTemplate != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Workshop active: ${activeTemplate?.title.orEmpty()}",
                    modifier = Modifier.weight(1f),
                    fontWeight = FontWeight.Medium,
                )
                OutlinedButton(onClick = ActiveWorkshopTemplateStore::clear) {
                    Text("Clear")
                }
            }
        }

        NewTemplateCard(
            state = editorState,
            promptBlockInput = promptBlockInput,
            onTitleChange = editorViewModel::updateTitle,
            onGenreChange = editorViewModel::updateGenre,
            onPremiseChange = editorViewModel::updatePremise,
            onPromptBlockInputChange = { promptBlockInput = it },
            onAddPromptBlock = {
                editorViewModel.addPromptBlock(promptBlockInput)
                promptBlockInput = ""
            },
            onSaveTemplate = {
                scope.launch {
                    editorViewModel.saveTemplate { draft ->
                        val savedTemplate = saveLocalTemplate(draft)
                        templates = loadLocalTemplates()
                        ActiveWorkshopTemplateStore.select(
                            ActiveWorkshopTemplate(
                                id = savedTemplate.id,
                                title = savedTemplate.title,
                            )
                        )
                    }
                }
            },
        )

        if (templates.isEmpty()) {
            Text("No local templates yet. Create one below, then bind it to Workshop.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templates, key = Template::id) { template ->
                    TemplateListItem(
                        template = template,
                        isActive = activeTemplate?.id == template.id,
                    )
                }
            }
        }
    }
}

@Composable
private fun NewTemplateCard(
    state: TemplateEditorState,
    promptBlockInput: String,
    onTitleChange: (String) -> Unit,
    onGenreChange: (String) -> Unit,
    onPremiseChange: (String) -> Unit,
    onPromptBlockInputChange: (String) -> Unit,
    onAddPromptBlock: () -> Unit,
    onSaveTemplate: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "New Template",
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
                    state.promptBlocks.forEach { block ->
                        Text("- $block")
                    }
                }
            }
            Button(
                onClick = onSaveTemplate,
                enabled = state.title.isNotBlank() &&
                    state.genre.isNotBlank() &&
                    state.premise.isNotBlank() &&
                    state.promptBlocks.isNotEmpty(),
            ) {
                Text("Save and Use in Workshop")
            }
        }
    }
}

@Composable
private fun TemplateListItem(
    template: Template,
    isActive: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                ActiveWorkshopTemplateStore.select(
                    ActiveWorkshopTemplate(id = template.id, title = template.title)
                )
            },
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
            Button(
                onClick = {
                    ActiveWorkshopTemplateStore.select(
                        ActiveWorkshopTemplate(id = template.id, title = template.title)
                    )
                },
            ) {
                Text(if (isActive) "Selected for Workshop" else "Use in Workshop")
            }
        }
    }
}
