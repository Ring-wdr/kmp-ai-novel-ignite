package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ChatPanel(
    messages: List<WorkshopChatMessage>,
    chatInputText: String,
    streamingStatus: WorkshopStreamingStatus,
    errorMessage: String?,
    onChatInputChange: (String) -> Unit,
    onSendChatMessage: () -> Unit,
    onContinueScene: () -> Unit,
    onAbortGeneration: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isReady = streamingStatus == WorkshopStreamingStatus.Idle
    val isGenerating = streamingStatus == WorkshopStreamingStatus.Streaming ||
        streamingStatus == WorkshopStreamingStatus.Recovering

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Workshop Chat")
            statusText(streamingStatus)?.let { status ->
                Text(
                    text = status,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            errorMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            WorkshopChatTimeline(messages = messages)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = onContinueScene,
                    enabled = isReady,
                ) {
                    Text("Continue scene")
                }
                if (isGenerating) {
                    Button(onClick = onAbortGeneration) {
                        Text("Abort")
                    }
                }
            }
            WorkshopChatComposer(
                chatInputText = chatInputText,
                enabled = isReady,
                onChatInputChange = onChatInputChange,
                onSendChatMessage = onSendChatMessage,
            )
        }
    }
}

private fun statusText(streamingStatus: WorkshopStreamingStatus): String? = when (streamingStatus) {
    WorkshopStreamingStatus.Idle -> null
    WorkshopStreamingStatus.Streaming -> "Streaming..."
    WorkshopStreamingStatus.Recovering -> "Recovering..."
}
