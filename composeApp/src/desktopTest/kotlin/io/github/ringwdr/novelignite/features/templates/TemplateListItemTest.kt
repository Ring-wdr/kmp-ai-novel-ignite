package io.github.ringwdr.novelignite.features.templates

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ringwdr.novelignite.domain.model.Template
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Rule

class TemplateListItemTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun newTemplateButton_invokesCreateCallback() {
        var createCalls = 0

        rule.setContent {
            TemplatesListPane(
                templates = emptyList(),
                activeTemplate = null,
                highlightedTemplateId = null,
                feedbackMessage = null,
                onCreateTemplate = { createCalls++ },
                onOpenTemplate = {},
                onUseInWorkshop = {},
                onClearWorkshopTemplate = {},
            )
        }

        rule.onNodeWithText("New Template").performClick()

        rule.runOnIdle {
            assertEquals(1, createCalls)
        }
    }

    @Test
    fun listPane_showsHighlightAndFeedback_fromStateInputs() {
        rule.setContent {
            TemplatesListPane(
                templates = listOf(
                    Template(
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
                ),
                activeTemplate = null,
                highlightedTemplateId = 7L,
                feedbackMessage = "Template saved",
                onCreateTemplate = {},
                onOpenTemplate = {},
                onUseInWorkshop = {},
                onClearWorkshopTemplate = {},
            )
        }

        rule.onNodeWithText("Recently saved").fetchSemanticsNode()
        rule.onNodeWithText("Template saved").fetchSemanticsNode()
    }

    @Test
    fun rendersWorkshopSelectionAction_withoutDeleteAction() {
        var workshopSelections = 0

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
                onUseInWorkshop = { workshopSelections++ },
            )
        }

        rule.onNodeWithText("Active in Workshop").fetchSemanticsNode()
        rule.onNodeWithText("Selected for Workshop").fetchSemanticsNode()
        rule.onNodeWithText("Selected for Workshop").performClick()
        rule.runOnIdle {
            assertEquals(1, workshopSelections)
        }
        assertFailsWith<AssertionError> {
            rule.onNodeWithText("Delete").fetchSemanticsNode()
        }
    }
}
