package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import org.junit.Rule

class TemplateListItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersDeleteActionInTemplateListCard() {
        rule.setContent {
            TemplateListItem(
                template = Template(
                    id = 7L,
                    title = "Noir Seoul",
                    genre = "Urban Fantasy",
                    premise = "A ghost broker solves debts",
                    worldSetting = "",
                    characterCards = "",
                    relationshipNotes = "",
                    toneStyle = "",
                    bannedElements = "",
                    plotConstraints = "",
                    openingHook = "",
                    promptBlocks = listOf("Keep sensory detail high"),
                    createdAtEpochMs = 0L,
                    updatedAtEpochMs = 0L,
                ),
                isActive = false,
                isSelected = false,
                onOpenTemplate = {},
                onDeleteTemplate = {},
            )
        }

        rule.onNodeWithText("Delete").fetchSemanticsNode()
    }
}
