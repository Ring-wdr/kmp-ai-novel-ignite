package io.github.ringwdr.novelignite.features.library

import io.github.ringwdr.novelignite.features.templates.AndroidTemplateMemoryStore

actual suspend fun loadLocalLibraryState(): LibraryState = LibraryState(
    templates = AndroidTemplateMemoryStore.list().map { template ->
        LibraryTemplateSummary(
            id = template.id,
            title = template.title,
            genre = template.genre,
            promptBlockCount = template.promptBlocks.size,
            updatedAtEpochMs = template.updatedAtEpochMs,
        )
    },
)
