package io.github.ringwdr.novelignite.features.templates

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TemplatesScreenStateTest {
    @Test
    fun openCreate_switchesToEditorCreateMode_andClearsEditingId() {
        val state = TemplatesScreenState().openCreate()

        assertEquals(TemplatesSurface.Editor, state.surface)
        assertEquals(TemplateEditorMode.Create, state.editorMode)
        assertNull(state.editingTemplateId)
        assertNull(state.remixBanner)
    }

    @Test
    fun openEdit_switchesToEditorEditMode_andKeepsTemplateId() {
        val state = TemplatesScreenState().openEdit(templateId = 7L)

        assertEquals(TemplatesSurface.Editor, state.surface)
        assertEquals(TemplateEditorMode.Edit, state.editorMode)
        assertEquals(7L, state.editingTemplateId)
    }

    @Test
    fun onSaveSuccess_returnsToList_withHighlightAndFeedback() {
        val state = TemplatesScreenState(
            surface = TemplatesSurface.Editor,
            editorMode = TemplateEditorMode.Edit,
            editingTemplateId = 7L,
            remixBanner = "Board: Gothic Mystery v3",
        ).onSaveSuccess(
            savedTemplateId = 7L,
            message = "Template saved",
        )

        assertEquals(TemplatesSurface.List, state.surface)
        assertEquals(7L, state.highlightedTemplateId)
        assertEquals("Template saved", state.feedbackMessage)
        assertNull(state.remixBanner)
    }
}
