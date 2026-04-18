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
import androidx.compose.runtime.LaunchedEffect
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
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplatesScreen() {
    var templates by remember { mutableStateOf(loadLocalTemplates()) }
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()
    val newTemplateEditorViewModel = remember { TemplateEditorViewModel() }
    val newTemplateEditorState by newTemplateEditorViewModel.state.collectAsState()
    val detailTemplateEditorViewModel = remember { TemplateEditorViewModel() }
    val detailTemplateEditorState by detailTemplateEditorViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var newPromptBlockInput by remember { mutableStateOf("") }
    var detailPromptBlockInput by remember { mutableStateOf("") }
    var selectedTemplateId by remember { mutableStateOf<Long?>(null) }
    val selectedTemplate = selectedTemplateId?.let { templateId ->
        templates.firstOrNull { it.id == templateId }
    }
    var selectedTemplateVersions by remember { mutableStateOf<List<TemplateVersion>>(emptyList()) }
    val selectedTemplateVersionKey = selectedTemplate?.let { it.id to it.updatedAtEpochMs }

    LaunchedEffect(selectedTemplateVersionKey) {
        selectedTemplateVersions = selectedTemplate?.let { template ->
            withContext(Dispatchers.Default) {
                loadLocalTemplateVersions(template.id)
            }
        }.orEmpty()
    }

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

        TemplateEditorCard(
            title = "New Template",
            saveLabel = "Save and Use in Workshop",
            state = newTemplateEditorState,
            promptBlockInput = newPromptBlockInput,
            onTitleChange = newTemplateEditorViewModel::updateTitle,
            onGenreChange = newTemplateEditorViewModel::updateGenre,
            onPremiseChange = newTemplateEditorViewModel::updatePremise,
            onPromptBlockInputChange = { newPromptBlockInput = it },
            onAddPromptBlock = {
                newTemplateEditorViewModel.addPromptBlock(newPromptBlockInput)
                newPromptBlockInput = ""
            },
            onPromptBlockChange = newTemplateEditorViewModel::updatePromptBlock,
            onRemovePromptBlock = newTemplateEditorViewModel::removePromptBlock,
            onSaveTemplate = {
                scope.launch {
                    val savedTemplate = newTemplateEditorViewModel.saveTemplate(
                        onSave = { draft ->
                            saveLocalTemplate(draft = draft)
                        },
                    )
                    templates = loadLocalTemplates()
                    ActiveWorkshopTemplateStore.select(
                        ActiveWorkshopTemplate(
                            id = savedTemplate.id,
                            title = savedTemplate.title,
                        )
                    )
                }
            },
        )

        if (selectedTemplate != null) {
            TemplateEditorCard(
                title = "Template Details",
                saveLabel = "Save Changes",
                state = detailTemplateEditorState,
                promptBlockInput = detailPromptBlockInput,
                onTitleChange = detailTemplateEditorViewModel::updateTitle,
                onGenreChange = detailTemplateEditorViewModel::updateGenre,
                onPremiseChange = detailTemplateEditorViewModel::updatePremise,
                onPromptBlockInputChange = { detailPromptBlockInput = it },
                onAddPromptBlock = {
                    detailTemplateEditorViewModel.addPromptBlock(detailPromptBlockInput)
                    detailPromptBlockInput = ""
                },
                onPromptBlockChange = detailTemplateEditorViewModel::updatePromptBlock,
                onRemovePromptBlock = detailTemplateEditorViewModel::removePromptBlock,
                onSaveTemplate = {
                    scope.launch {
                        val templateId = selectedTemplateId ?: return@launch
                        val savedTemplate = detailTemplateEditorViewModel.saveTemplate(
                            onSave = { draft ->
                                saveLocalTemplate(
                                    draft = draft,
                                    templateId = templateId,
                                    originalTemplate = selectedTemplate,
                                )
                            },
                            resetAfterSave = false,
                        )
                        templates = loadLocalTemplates()
                        selectedTemplateId = savedTemplate.id
                        detailTemplateEditorViewModel.loadTemplate(savedTemplate)
                        detailPromptBlockInput = ""
                        if (activeTemplate?.id == savedTemplate.id) {
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

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Version history",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (selectedTemplateVersions.isEmpty()) {
                        Text("No saved versions yet.")
                    } else {
                        Text("${selectedTemplateVersions.size} saved snapshots")
                        selectedTemplateVersions.take(3).forEach { version ->
                            Text("v${version.versionNumber} · ${version.title}")
                        }
                    }
                }
            }
        }

        if (templates.isEmpty()) {
            Text("No local templates yet. Create one below, then bind it to Workshop.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(templates, key = Template::id) { template ->
                    TemplateListItem(
                        template = template,
                        isActive = activeTemplate?.id == template.id,
                        isSelected = selectedTemplate?.id == template.id,
                        onOpenTemplate = {
                            selectedTemplateId = template.id
                            detailTemplateEditorViewModel.loadTemplate(template)
                            detailPromptBlockInput = ""
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun TemplateEditorCard(
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

@Composable
private fun TemplateListItem(
    template: Template,
    isActive: Boolean,
    isSelected: Boolean,
    onOpenTemplate: () -> Unit,
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
            if (isSelected) {
                Text(
                    text = "Editing this template",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
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
