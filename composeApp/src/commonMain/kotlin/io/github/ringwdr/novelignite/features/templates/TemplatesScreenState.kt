package io.github.ringwdr.novelignite.features.templates

internal enum class TemplatesSurface {
    List,
    Editor,
}

internal enum class TemplateEditorMode {
    Create,
    Edit,
}

internal data class TemplatesScreenState(
    val surface: TemplatesSurface = TemplatesSurface.List,
    val editorMode: TemplateEditorMode = TemplateEditorMode.Create,
    val editingTemplateId: Long? = null,
    val remixBanner: String? = null,
    val highlightedTemplateId: Long? = null,
    val feedbackMessage: String? = null,
) {
    fun openCreate(remixBanner: String? = null): TemplatesScreenState = copy(
        surface = TemplatesSurface.Editor,
        editorMode = TemplateEditorMode.Create,
        editingTemplateId = null,
        remixBanner = remixBanner,
        feedbackMessage = null,
    )

    fun openEdit(templateId: Long): TemplatesScreenState = copy(
        surface = TemplatesSurface.Editor,
        editorMode = TemplateEditorMode.Edit,
        editingTemplateId = templateId,
        remixBanner = null,
        feedbackMessage = null,
    )

    fun returnToList(): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        remixBanner = null,
    )

    fun onSaveSuccess(savedTemplateId: Long, message: String): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        editingTemplateId = null,
        remixBanner = null,
        highlightedTemplateId = savedTemplateId,
        feedbackMessage = message,
    )

    fun onDeleteSuccess(message: String): TemplatesScreenState = copy(
        surface = TemplatesSurface.List,
        editingTemplateId = null,
        remixBanner = null,
        highlightedTemplateId = null,
        feedbackMessage = message,
    )
}
