package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion

expect fun loadLocalTemplates(): List<Template>

expect fun loadLocalTemplateVersions(templateId: Long): List<TemplateVersion>

expect suspend fun saveLocalTemplate(
    draft: TemplateDraft,
    templateId: Long? = null,
    originalTemplate: Template? = null,
    originalVersion: TemplateVersion? = null,
): Template

expect suspend fun deleteLocalTemplate(
    templateId: Long,
)

expect suspend fun enrichTemplateDraft(
    draft: TemplateDraft,
): TemplateDraft
