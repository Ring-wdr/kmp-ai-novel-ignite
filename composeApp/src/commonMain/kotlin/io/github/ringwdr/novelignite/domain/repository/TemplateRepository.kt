package io.github.ringwdr.novelignite.domain.repository

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion

interface TemplateRepository {
    suspend fun saveTemplate(
        title: String,
        genre: String,
        premise: String,
        worldSetting: String,
        characterCards: String,
        relationshipNotes: String,
        toneStyle: String,
        bannedElements: String,
        plotConstraints: String,
        openingHook: String,
        promptBlocks: List<String>,
        templateId: Long? = null,
    ): Template

    fun listTemplates(): List<Template>

    fun listTemplateVersions(templateId: Long): List<TemplateVersion>

    fun listAllTemplateVersions(): List<TemplateVersion>
}
