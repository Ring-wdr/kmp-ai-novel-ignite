package io.github.ringwdr.novelignite.features.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel? = null,
) {
    val internalViewModel = if (viewModel == null) rememberLibraryViewModel() else null
    val activeViewModel = viewModel ?: internalViewModel!!
    val state by activeViewModel.state.collectAsState()

    if (internalViewModel != null) {
        DisposableEffect(internalViewModel) {
            onDispose { internalViewModel.clear() }
        }
    }

    Column(
        modifier = Modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = "Browse local projects and templates that already live on this device.",
            style = MaterialTheme.typography.bodyMedium,
        )
        if (state.isLoading) {
            Text(
                text = "Loading local library...",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.errorMessage?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        LibrarySection(
            title = "Projects",
            emptyText = "No local projects yet.",
            isEmpty = state.projects.isEmpty() && !state.isLoading,
        ) {
            state.projects.forEach { project ->
                ProjectCard(project = project)
            }
        }

        LibrarySection(
            title = "Templates",
            emptyText = "No local templates yet.",
            isEmpty = state.templates.isEmpty() && !state.isLoading,
        ) {
            state.templates.forEach { template ->
                TemplateCard(template = template)
            }
        }
    }
}

@Composable
private fun rememberLibraryViewModel(): LibraryViewModel = remember {
    LibraryViewModel()
}

@Composable
private fun LibrarySection(
    title: String,
    emptyText: String,
    isEmpty: Boolean,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        if (isEmpty) {
            Text(emptyText, style = MaterialTheme.typography.bodyMedium)
        } else {
            content()
        }
    }
}

@Composable
private fun ProjectCard(project: LibraryProjectSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = project.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = project.templateTitle?.let { "Template: $it" } ?: "Template: none linked",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = if (project.hasLatestDraftSession) {
                    "Latest draft: ${project.latestDraftPreview ?: "Saved locally"}"
                } else {
                    "Latest draft: none yet"
                },
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TemplateCard(template: LibraryTemplateSummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
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
                text = "${template.promptBlockCount} prompt blocks saved locally",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
