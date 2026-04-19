package io.github.ringwdr.novelignite.features.templates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val selectedTemplate = screenState.editingTemplateId?.let { templateId ->
        templates.firstOrNull { it.id == templateId }
    }
    val selectedTemplateVersionKey = selectedTemplate?.let { template -> template.id to template.updatedAtEpochMs }
    val hasPendingPromptBlockInput = promptBlockInput.text.isNotBlank()

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

    fun forceReturnToList() {
        editorViewModel.reset()
        clearTransientEditorState()
        screenState = screenState.returnToList()
    }

    fun requestReturnToList() {
        if (editorViewModel.hasUnsavedChanges() || hasPendingPromptBlockInput) {
            showDiscardDialog = true
        } else {
            forceReturnToList()
        }
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
                    showDeleteAction = !showDeleteDialog,
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
                            if (screenState.editorMode == TemplateEditorMode.Create || activeTemplate?.id == savedTemplate.id) {
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

        if (showDeleteDialog && selectedTemplate != null) {
            AlertDialog(
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
            AlertDialog(
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
    }
}
