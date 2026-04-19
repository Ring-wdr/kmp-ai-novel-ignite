package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import kotlin.test.assertFailsWith
import org.junit.Rule

class TemplateListItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rendersWorkshopSelectionAction_withoutDeleteAction() {
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
                isActive = true,
                isHighlighted = false,
                onOpenTemplate = {},
                onUseInWorkshop = {},
            )
        }

        rule.onNodeWithText("Active in Workshop").fetchSemanticsNode()
        rule.onNodeWithText("Selected for Workshop").fetchSemanticsNode()
        assertFailsWith<AssertionError> {
            rule.onNodeWithText("Delete").fetchSemanticsNode()
        }
    }
}
