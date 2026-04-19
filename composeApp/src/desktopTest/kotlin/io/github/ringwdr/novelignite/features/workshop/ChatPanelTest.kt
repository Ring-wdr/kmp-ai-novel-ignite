package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
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
                onContinueScene = {},
                onAbortGeneration = {},
            )
        }

        rule.onNodeWithText("Generating...").assertExists()
    }
}
