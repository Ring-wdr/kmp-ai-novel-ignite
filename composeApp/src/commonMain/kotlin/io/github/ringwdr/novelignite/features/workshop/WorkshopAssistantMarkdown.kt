package io.github.ringwdr.novelignite.features.workshop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.halilibo.richtext.commonmark.Markdown
import com.halilibo.richtext.ui.material3.RichText
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

internal const val WORKSHOP_ASSISTANT_MARKDOWN_TAG = "workshop-assistant-markdown"

@Composable
internal fun WorkshopAssistantMarkdown(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    RichText(
        modifier = modifier
            .testTag(WORKSHOP_ASSISTANT_MARKDOWN_TAG)
            .semantics { contentDescription = markdownPreview(markdown) },
    ) {
        Markdown(markdown)
    }
}

private fun markdownPreview(markdown: String): String =
    markdown.lineSequence()
        .map { it.trim() }
        .mapNotNull { line ->
            line.takeIf { it.isNotBlank() }
                ?.trimStart('#')
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }
        .joinToString(" ")
