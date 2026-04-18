package io.github.ringwdr.novelignite.domain.repository

import io.github.ringwdr.novelignite.domain.model.Project

interface ProjectRepository {
    suspend fun createProject(title: String, templateId: Long?): Project

    fun observeProjects(): List<Project>
}
