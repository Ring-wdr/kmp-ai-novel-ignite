package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template

actual fun loadLocalTemplates(): List<Template> = emptyList()

actual suspend fun saveLocalTemplate(draft: TemplateDraft): Template =
    error("Local template saving is not wired on Android yet.")
