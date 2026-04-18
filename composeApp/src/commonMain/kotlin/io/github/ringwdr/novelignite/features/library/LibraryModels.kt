package io.github.ringwdr.novelignite.features.library

import io.github.ringwdr.novelignite.domain.model.DraftSession
import io.github.ringwdr.novelignite.domain.model.Project
import io.github.ringwdr.novelignite.domain.model.Template

data class LibraryState(
    val projects: List<LibraryProjectSummary> = emptyList(),
    val templates: List<LibraryTemplateSummary> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

data class LibraryProjectSummary(
    val id: Long,
    val title: String,
    val templateTitle: String?,
    val latestDraftPreview: String?,
    val hasLatestDraftSession: Boolean,
    val updatedAtEpochMs: Long,
)

data class LibraryTemplateSummary(
    val id: Long,
    val title: String,
    val genre: String,
    val promptBlockCount: Int,
    val updatedAtEpochMs: Long,
)

fun buildLibraryState(
    projects: List<Project>,
    templates: List<Template>,
    latestDraftSession: (Long) -> DraftSession?,
): LibraryState {
    val templatesById = templates.associateBy(Template::id)

    return LibraryState(
        projects = projects.map { project ->
            val draftSession = latestDraftSession(project.id)
            LibraryProjectSummary(
                id = project.id,
                title = project.title,
                templateTitle = project.templateId?.let { templateId ->
                    templatesById[templateId]?.title
                },
                latestDraftPreview = draftSession?.content?.let(::previewDraftContent),
                hasLatestDraftSession = draftSession != null,
                updatedAtEpochMs = project.updatedAtEpochMs,
            )
        },
        templates = templates.map { template ->
            LibraryTemplateSummary(
                id = template.id,
                title = template.title,
                genre = template.genre,
                promptBlockCount = template.promptBlocks.size,
                updatedAtEpochMs = template.updatedAtEpochMs,
            )
        },
    )
}

fun previewDraftContent(content: String): String {
    val firstLine = content
        .lineSequence()
        .firstOrNull()
        .orEmpty()
        .trim()

    if (firstLine.isBlank()) {
        return "Draft content saved locally."
    }

    return if (firstLine.length <= 80) firstLine else "${firstLine.take(77)}..."
}

expect suspend fun loadLocalLibraryState(): LibraryState
