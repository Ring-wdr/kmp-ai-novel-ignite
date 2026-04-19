package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun TemplatesScreen() {
    var templates by remember { mutableStateOf(loadLocalTemplates()) }
    var screenState by remember { mutableStateOf(TemplatesScreenState()) }
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()
    val newTemplateEditorViewModel = remember { TemplateEditorViewModel() }
    val newTemplateEditorState by newTemplateEditorViewModel.state.collectAsState()
    val detailTemplateEditorViewModel = remember { TemplateEditorViewModel() }
    val detailTemplateEditorState by detailTemplateEditorViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()
    var newPromptBlockInput by remember { mutableStateOf(TextFieldValue("")) }
    var detailPromptBlockInput by remember { mutableStateOf(TextFieldValue("")) }
    var newPromptBlockError by remember { mutableStateOf<String?>(null) }
    var detailPromptBlockError by remember { mutableStateOf<String?>(null) }
    var newEnrichError by remember { mutableStateOf<String?>(null) }
    var detailEnrichError by remember { mutableStateOf<String?>(null) }
    var isEnrichingNewTemplate by remember { mutableStateOf(false) }
    var isEnrichingDetailTemplate by remember { mutableStateOf(false) }
    var selectedTemplateId by remember { mutableStateOf<Long?>(null) }
    var remixBanner by remember { mutableStateOf<String?>(null) }
    var remixSourceVersion by remember { mutableStateOf<TemplateVersion?>(null) }
    val remixSelection by TemplateRemixStore.selection.collectAsState()
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

    LaunchedEffect(remixSelection) {
        val selection = remixSelection ?: return@LaunchedEffect
        newTemplateEditorViewModel.loadDraft(selection.draft)
        screenState = screenState.openCreate().copy(highlightedTemplateId = null)
        newPromptBlockInput = TextFieldValue("")
        newPromptBlockError = null
        newEnrichError = null
        selectedTemplateId = null
        detailPromptBlockInput = TextFieldValue("")
        detailPromptBlockError = null
        detailEnrichError = null
        remixBanner = selection.sourceLabel
        remixSourceVersion = selection.sourceVersion
        TemplateRemixStore.clear()
    }

    Column(
        modifier = Modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (remixBanner != null) {
            Text(
                text = "Loaded remix from ${remixBanner.orEmpty()}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )
        }

        TemplateEditorCard(
            title = "New Template",
            saveLabel = "Save and Use in Workshop",
            state = newTemplateEditorState,
            promptBlockInput = newPromptBlockInput,
            promptBlockErrorMessage = newPromptBlockError,
            onTitleChange = newTemplateEditorViewModel::updateTitle,
            onGenreChange = newTemplateEditorViewModel::updateGenre,
            onPremiseChange = newTemplateEditorViewModel::updatePremise,
            onPromptBlockInputChange = {
                newPromptBlockInput = it
                newPromptBlockError = null
            },
            onAddPromptBlock = {
                val added = newTemplateEditorViewModel.addPromptBlock(newPromptBlockInput.text)
                if (added) {
                    newPromptBlockInput = TextFieldValue("")
                    newPromptBlockError = null
                } else {
                    newPromptBlockError = "Add a prompt block before continuing."
                }
            },
            onPromptBlockChange = newTemplateEditorViewModel::updatePromptBlock,
            onRemovePromptBlock = newTemplateEditorViewModel::removePromptBlock,
            onSaveTemplate = {
                scope.launch {
                    val savedTemplate = newTemplateEditorViewModel.saveTemplate(
                        onSave = { draft ->
                            saveLocalTemplate(
                                draft = draft,
                                originalVersion = remixSourceVersion,
                            )
                        },
                    )
                    templates = loadLocalTemplates()
                    remixSourceVersion = null
                    screenState = screenState.onSaveSuccess(
                        savedTemplateId = savedTemplate.id,
                        message = "Template saved",
                    )
                    ActiveWorkshopTemplateStore.select(
                        ActiveWorkshopTemplate(
                            id = savedTemplate.id,
                            title = savedTemplate.title,
                        )
                    )
                }
            },
            onEnrichTemplate = {
                scope.launch {
                    isEnrichingNewTemplate = true
                    newEnrichError = null
                    runCatching {
                        enrichTemplateDraft(newTemplateEditorViewModel.snapshotDraft())
                    }.onSuccess { enriched ->
                        newTemplateEditorViewModel.applyEnrichedDraft(enriched)
                    }.onFailure { error ->
                        newEnrichError = error.message ?: "Template enrichment failed."
                    }
                    isEnrichingNewTemplate = false
                }
            },
            isEnriching = isEnrichingNewTemplate,
            enrichErrorMessage = newEnrichError,
        )

        if (selectedTemplate != null) {
            TemplateEditorCard(
                title = "Template Details",
                saveLabel = "Save Changes",
                state = detailTemplateEditorState,
                promptBlockInput = detailPromptBlockInput,
                promptBlockErrorMessage = detailPromptBlockError,
                onTitleChange = detailTemplateEditorViewModel::updateTitle,
                onGenreChange = detailTemplateEditorViewModel::updateGenre,
                onPremiseChange = detailTemplateEditorViewModel::updatePremise,
                onPromptBlockInputChange = {
                    detailPromptBlockInput = it
                    detailPromptBlockError = null
                },
                onAddPromptBlock = {
                    val added = detailTemplateEditorViewModel.addPromptBlock(detailPromptBlockInput.text)
                    if (added) {
                        detailPromptBlockInput = TextFieldValue("")
                        detailPromptBlockError = null
                    } else {
                        detailPromptBlockError = "Add a prompt block before continuing."
                    }
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
                        screenState = screenState.onSaveSuccess(
                            savedTemplateId = savedTemplate.id,
                            message = "Template saved",
                        )
                        detailTemplateEditorViewModel.loadTemplate(savedTemplate)
                        detailPromptBlockInput = TextFieldValue("")
                        detailPromptBlockError = null
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
                onEnrichTemplate = {
                    scope.launch {
                        isEnrichingDetailTemplate = true
                        detailEnrichError = null
                        runCatching {
                            enrichTemplateDraft(detailTemplateEditorViewModel.snapshotDraft())
                        }.onSuccess { enriched ->
                            detailTemplateEditorViewModel.applyEnrichedDraft(enriched)
                        }.onFailure { error ->
                            detailEnrichError = error.message ?: "Template enrichment failed."
                        }
                        isEnrichingDetailTemplate = false
                    }
                },
                isEnriching = isEnrichingDetailTemplate,
                enrichErrorMessage = detailEnrichError,
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
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                val templateId = selectedTemplateId ?: return@launch
                                deleteLocalTemplate(templateId)
                                templates = loadLocalTemplates()
                                if (activeTemplate?.id == templateId) {
                                    ActiveWorkshopTemplateStore.clear()
                                }
                                selectedTemplateId = null
                                selectedTemplateVersions = emptyList()
                                detailTemplateEditorViewModel.reset()
                                detailPromptBlockInput = TextFieldValue("")
                                detailPromptBlockError = null
                                detailEnrichError = null
                                screenState = screenState.onDeleteSuccess("Template deleted")
                            }
                        },
                    ) {
                        Text("Delete Template")
                    }
                }
            }
        }

        TemplatesListPane(
            templates = templates,
            activeTemplate = activeTemplate,
            highlightedTemplateId = screenState.highlightedTemplateId,
            feedbackMessage = screenState.feedbackMessage,
            onCreateTemplate = {
                screenState = screenState.openCreate().copy(highlightedTemplateId = null)
                selectedTemplateId = null
                selectedTemplateVersions = emptyList()
                remixBanner = null
                remixSourceVersion = null
                newTemplateEditorViewModel.reset()
                newPromptBlockInput = TextFieldValue("")
                newPromptBlockError = null
                newEnrichError = null
                detailTemplateEditorViewModel.reset()
                detailPromptBlockInput = TextFieldValue("")
                detailPromptBlockError = null
                detailEnrichError = null
                TemplateRemixStore.clear()
            },
            onOpenTemplate = { template ->
                screenState = screenState.returnToList().copy(
                    highlightedTemplateId = null,
                    feedbackMessage = null,
                )
                selectedTemplateId = template.id
                detailTemplateEditorViewModel.loadTemplate(template)
                detailPromptBlockInput = TextFieldValue("")
                detailPromptBlockError = null
                detailEnrichError = null
                selectedTemplateVersions = emptyList()
            },
            onUseInWorkshop = { template ->
                ActiveWorkshopTemplateStore.select(
                    ActiveWorkshopTemplate(id = template.id, title = template.title)
                )
            },
            onClearWorkshopTemplate = ActiveWorkshopTemplateStore::clear,
        )
    }
}
