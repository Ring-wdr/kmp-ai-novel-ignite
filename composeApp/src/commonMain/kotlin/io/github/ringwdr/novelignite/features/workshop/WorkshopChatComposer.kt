package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

const val WORKSHOP_CHAT_INPUT_TAG = "workshop-chat-input"

@Composable
internal fun WorkshopChatComposer(
    chatInputText: String,
    enabled: Boolean,
    onChatInputChange: (String) -> Unit,
    onSendChatMessage: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Reply")
            OutlinedTextField(
                value = chatInputText,
                onValueChange = onChatInputChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(WORKSHOP_CHAT_INPUT_TAG),
                enabled = enabled,
                placeholder = { Text("Type a chat message") },
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                Button(
                    onClick = onSendChatMessage,
                    enabled = enabled && chatInputText.isNotBlank(),
                ) {
                    Text("Send")
                }
            }
        }
    }
}
