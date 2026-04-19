package io.github.ringwdr.novelignite.features.board

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.ringwdr.novelignite.features.templates.TemplateRemixStore

@Composable
fun BoardScreen(
    viewModel: BoardViewModel = remember { BoardViewModel() },
    onOpenTemplates: () -> Unit = {},
) {
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = state.title,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(state.subtitle)
        if (state.isLoading) {
            Text("Loading local snapshots...")
        } else if (state.snapshots.isEmpty()) {
            Text("Save a few templates first, then Board will surface local snapshots to remix.")
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.snapshots, key = BoardSnapshotCard::id) { card ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = card.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "${card.genre} · v${card.versionNumber}",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                text = card.premise.ifBlank { "No premise yet." },
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Button(
                                onClick = {
                                    viewModel.startRemix(card.id)?.let(TemplateRemixStore::select)
                                    onOpenTemplates()
                                },
                            ) {
                                Text("Remix in Templates")
                            }
                        }
                    }
                }
            }
        }
    }
}
