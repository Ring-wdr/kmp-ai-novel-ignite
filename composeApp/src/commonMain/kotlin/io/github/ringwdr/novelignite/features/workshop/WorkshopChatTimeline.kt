package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState

internal const val WORKSHOP_CHAT_TIMELINE_TAG = "workshop-chat-timeline"
private fun workshopChoiceButtonTag(choiceId: String): String = "workshop-choice-$choiceId"

@Composable
internal fun WorkshopChatTimeline(
    messages: List<WorkshopChatMessage>,
    onUseChoice: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val latestMessage = messages.lastOrNull()
    val latestAssistantMessageId = messages.lastOrNull { it.role == WorkshopMessageRole.Assistant }?.id

    LaunchedEffect(
        latestMessage?.id,
        latestMessage?.assistant?.renderedMarkdown,
        latestMessage?.assistant?.choices?.size,
        latestMessage?.isStreaming,
    ) {
        if (messages.isNotEmpty()) {
            listState.scrollToItem(messages.lastIndex)
        }
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Conversation",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )

            if (messages.isEmpty()) {
                Text("No messages yet.")
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .testTag(WORKSHOP_CHAT_TIMELINE_TAG),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(messages, key = WorkshopChatMessage::id) { message ->
                        val isUser = message.role == WorkshopMessageRole.User
                        val assistant = message.assistant
                        val bubbleText = assistant?.renderedMarkdown?.takeIf { it.isNotBlank() }
                            ?: message.text
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start,
                        ) {
                            Card(
                                modifier = Modifier.widthIn(max = 520.dp),
                                colors = if (isUser) {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                } else {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                },
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp),
                                ) {
                                    Text(
                                        text = if (isUser) "You" else "Assistant",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                    if (isUser) {
                                        Text(text = bubbleText)
                                    } else {
                                        val isStreaming = message.isStreaming
                                        if (bubbleText.isBlank() && isStreaming) {
                                            Text(text = "Generating...")
                                        } else if (bubbleText.isNotBlank()) {
                                            WorkshopAssistantMarkdown(
                                                markdown = bubbleText,
                                                modifier = Modifier.fillMaxWidth(),
                                            )
                                        }
                                        val showChoices = message.id == latestAssistantMessageId
                                        if (showChoices) {
                                            assistant?.choices
                                            .orEmpty()
                                            .takeIf { it.isNotEmpty() }
                                            ?.let { choices ->
                                                Column(
                                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                                ) {
                                                    choices.forEach { choice ->
                                                        when (choice.style) {
                                                            WorkshopChoiceStyle.Primary -> Button(
                                                                onClick = { onUseChoice(choice.prompt) },
                                                                enabled = !isStreaming,
                                                                modifier = Modifier.testTag(workshopChoiceButtonTag(choice.id)),
                                                            ) {
                                                                Text(choice.label)
                                                            }

                                                            WorkshopChoiceStyle.Secondary -> OutlinedButton(
                                                                onClick = { onUseChoice(choice.prompt) },
                                                                enabled = !isStreaming,
                                                                modifier = Modifier.testTag(workshopChoiceButtonTag(choice.id)),
                                                            ) {
                                                                Text(choice.label)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
