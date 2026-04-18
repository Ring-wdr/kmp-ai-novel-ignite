package io.github.ringwdr.novelignite.features.templates

import io.github.ringwdr.novelignite.data.local.TemplateRepositoryImpl
import io.github.ringwdr.novelignite.data.local.openDesktopDatabase
import io.github.ringwdr.novelignite.domain.model.Template

actual fun loadLocalTemplates(): List<Template> = TemplateRepositoryImpl(
    database = openDesktopDatabase(),
).listTemplates()
