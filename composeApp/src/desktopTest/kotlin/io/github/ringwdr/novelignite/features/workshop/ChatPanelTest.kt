package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule

class ChatPanelTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersConversationAndStreamingControls() {
        rule.setContent {
            ChatPanel(
                messages = listOf(
                    WorkshopChatMessage.user(
                        id = "user-1",
                        text = "Continue the scene.",
                    ),
                    WorkshopChatMessage.assistant(
                        id = "assistant-1",
                        text = "",
                        isStreaming = true,
                    ),
                ),
                chatInputText = "",
                streamingStatus = WorkshopStreamingStatus.Streaming,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithText("Continue the scene.").assertExists()
        rule.onNodeWithText("Streaming...").assertExists()
        rule.onNodeWithText("Generating...").assertExists()
        rule.onNodeWithText("Abort").assertExists()
        rule.onNodeWithTag(WORKSHOP_CHAT_INPUT_TAG).assertExists()
    }

    @Test
    fun rendersAssistantMarkdownAndChoices() {
        rule.setContent {
            ChatPanel(
                messages = listOf(
                    WorkshopChatMessage.assistant(
                        id = "assistant-old",
                        text = "",
                        isStreaming = false,
                    ).copy(
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "Earlier answer.",
                            choices = listOf(
                                WorkshopChoice(
                                    id = "choice-old",
                                    label = "Old follow-up",
                                    prompt = "Use the old follow-up.",
                                ),
                            ),
                            phase = WorkshopAssistantPhase.Completed,
                        ),
                    ),
                    WorkshopChatMessage.assistant(
                        id = "assistant-1",
                        text = "",
                        isStreaming = false,
                    ).copy(
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "# Bold response\n\nPick a path.",
                            choices = listOf(
                                WorkshopChoice(
                                    id = "choice-1",
                                    label = "Continue scene",
                                    prompt = "Continue the scene from the checkpoint.",
                                    style = WorkshopChoiceStyle.Primary,
                                ),
                                WorkshopChoice(
                                    id = "choice-2",
                                    label = "Shift perspective",
                                    prompt = "Shift the perspective around the checkpoint.",
                                ),
                            ),
                            phase = WorkshopAssistantPhase.Completed,
                        ),
                    ),
                ),
                chatInputText = "",
                streamingStatus = WorkshopStreamingStatus.Idle,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithContentDescription("Bold response Pick a path.").assertExists()
        rule.onNodeWithContentDescription("Continue scene").assertExists()
        rule.onNodeWithContentDescription("Shift perspective").assertExists()
        rule.onAllNodesWithTag("workshop-choice-choice-old").assertCountEquals(0)
    }

    @Test
    fun choiceButtons_emitPrompt_andDisableWhileStreaming() {
        var receivedPrompt: String? = null

        rule.setContent {
            ChatPanel(
                messages = listOf(
                    WorkshopChatMessage.assistant(
                        id = "assistant-2",
                        text = "",
                        isStreaming = false,
                    ).copy(
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "Try this next.",
                            choices = listOf(
                                WorkshopChoice(
                                    id = "choice-1",
                                    label = "Continue scene",
                                    prompt = "Continue the scene from the checkpoint.",
                                    style = WorkshopChoiceStyle.Primary,
                                ),
                            ),
                            phase = WorkshopAssistantPhase.Completed,
                        ),
                    ),
                ),
                chatInputText = "",
                streamingStatus = WorkshopStreamingStatus.Idle,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = { receivedPrompt = it },
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithContentDescription("Try this next.").assertExists()
        rule.onNodeWithContentDescription("Continue scene").assertExists()
        rule.onNodeWithTag("workshop-choice-choice-1").performClick()
        rule.runOnIdle {
            assertEquals("Continue the scene from the checkpoint.", receivedPrompt)
        }
    }

    @Test
    fun latestStreamingChoice_surfaceIsDisabledAndOldChoicesStayHidden() {
        rule.setContent {
            ChatPanel(
                messages = listOf(
                    WorkshopChatMessage.assistant(
                        id = "assistant-old",
                        text = "",
                        isStreaming = false,
                    ).copy(
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "Older answer.",
                            choices = listOf(
                                WorkshopChoice(
                                    id = "choice-old",
                                    label = "Old follow-up",
                                    prompt = "Use the old follow-up.",
                                ),
                            ),
                            phase = WorkshopAssistantPhase.Completed,
                        ),
                    ),
                    WorkshopChatMessage.assistant(
                        id = "assistant-latest",
                        text = "",
                        isStreaming = true,
                    ).copy(
                        assistant = WorkshopAssistantTurn(
                            renderedMarkdown = "Still streaming.",
                            choices = listOf(
                                WorkshopChoice(
                                    id = "choice-latest",
                                    label = "Shift perspective",
                                    prompt = "Shift the perspective around the checkpoint.",
                                ),
                            ),
                            phase = WorkshopAssistantPhase.Streaming,
                        ),
                        isStreaming = true,
                    ),
                ),
                chatInputText = "",
                streamingStatus = WorkshopStreamingStatus.Streaming,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithContentDescription("Still streaming.").assertExists()
        rule.onNodeWithContentDescription("Shift perspective").assertExists()
        rule.onNodeWithTag("workshop-choice-choice-latest").assertIsNotEnabled()
        rule.onAllNodesWithTag("workshop-choice-choice-old").assertCountEquals(0)
    }

    @Test
    fun composerSend_updatesFromTyping_andInvokesCallback_whenIdle() {
        var sent = 0
        var chatInputText by mutableStateOf("")

        rule.setContent {
            ChatPanel(
                messages = emptyList(),
                chatInputText = chatInputText,
                streamingStatus = WorkshopStreamingStatus.Idle,
                errorMessage = null,
                onChatInputChange = { chatInputText = it },
                onSendChatMessage = { sent++ },
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNodeWithTag(WORKSHOP_CHAT_INPUT_TAG).performTextInput("Keep going")
        rule.onNodeWithText("Send").assertIsEnabled()
        rule.onNodeWithText("Send").performClick()

        rule.runOnIdle {
            assertEquals(1, sent)
            assertEquals("Keep going", chatInputText)
        }
    }

    @Test
    fun recoveringState_exposesAbort_andKeepsSubmitControlsDisabled() {
        var aborts = 0

        rule.setContent {
            ChatPanel(
                messages = emptyList(),
                chatInputText = "Keep going",
                streamingStatus = WorkshopStreamingStatus.Recovering,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = { aborts++ },
            )
        }

        rule.onNodeWithTag(WORKSHOP_CHAT_INPUT_TAG).assertIsNotEnabled()
        rule.onNodeWithText("Send").assertIsNotEnabled()
        rule.onNodeWithText("Continue scene").assertIsNotEnabled()
        rule.onNodeWithText("Abort").performClick()

        rule.runOnIdle {
            assertEquals(1, aborts)
        }
    }

    @Test
    fun timeline_autoFollowsNewestMessage() {
        rule.setContent {
            ChatPanel(
                messages = (1..24).map { index ->
                    if (index == 24) {
                        WorkshopChatMessage.assistant(
                            id = "assistant-$index",
                            text = "",
                            isStreaming = true,
                        )
                    } else {
                        WorkshopChatMessage.user(
                            id = "user-$index",
                            text = "Message $index",
                        )
                    }
                },
                chatInputText = "",
                streamingStatus = WorkshopStreamingStatus.Streaming,
                errorMessage = null,
                onChatInputChange = {},
                onSendChatMessage = {},
                onUseChoice = {},
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithText("Generating...").assertExists()
    }
}
