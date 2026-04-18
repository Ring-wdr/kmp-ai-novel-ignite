package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template

expect fun loadLocalTemplates(): List<Template>

expect suspend fun saveLocalTemplate(draft: TemplateDraft): Template
