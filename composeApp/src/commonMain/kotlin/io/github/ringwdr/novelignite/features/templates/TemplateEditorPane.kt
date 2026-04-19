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
            if (mode == TemplateEditorMode.Edit && showDeleteAction) {
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
