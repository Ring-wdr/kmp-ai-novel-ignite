package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText

internal const val WORKSHOP_ASSISTANT_MARKDOWN_TAG = "workshop-assistant-markdown"

@Composable
internal fun WorkshopAssistantMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    RichText(
        modifier = modifier
            .testTag(WORKSHOP_ASSISTANT_MARKDOWN_TAG),
    ) {
        Markdown(markdown)
    }
}
