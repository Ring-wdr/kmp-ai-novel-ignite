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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplate
import io.github.ringwdr.novelignite.features.workshop.ActiveWorkshopTemplateStore

@Composable
fun TemplatesScreen() {
    val templates = remember { loadLocalTemplates() }
    val activeTemplate by ActiveWorkshopTemplateStore.selection.collectAsState()

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

        if (templates.isEmpty()) {
            Text("No local templates yet. Save one first, then return here to bind it to Workshop.")
            return@Column
        }

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
