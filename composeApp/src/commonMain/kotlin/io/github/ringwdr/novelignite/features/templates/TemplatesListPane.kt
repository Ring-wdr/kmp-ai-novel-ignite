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
