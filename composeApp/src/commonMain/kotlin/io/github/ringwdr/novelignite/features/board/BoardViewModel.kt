package io.github.ringwdr.novelignite.features.board

import io.github.ringwdr.novelignite.domain.model.Template
import io.github.ringwdr.novelignite.domain.model.TemplateVersion
import io.github.ringwdr.novelignite.features.templates.loadLocalTemplateVersions
import io.github.ringwdr.novelignite.features.templates.loadLocalTemplates
import io.github.ringwdr.novelignite.features.templates.TemplateRemixSelection
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardUiState(
    val title: String = "Board",
    val subtitle: String = "Browse local template snapshots and remix them.",
    val isLoading: Boolean = true,
    val snapshots: List<BoardSnapshotCard> = emptyList(),
    val selectedSnapshotId: Long? = null,
    val activeRemix: TemplateRemixSelection? = null,
)

class BoardViewModel(
    private val loadTemplates: () -> List<Template> = ::loadLocalTemplates,
    private val loadTemplateVersions: (Long) -> List<TemplateVersion> = ::loadLocalTemplateVersions,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(BoardUiState())
    val state: StateFlow<BoardUiState> = _state

    init {
        refresh()
    }

    fun refresh() {
        scope.launch {
            val snapshots = loadTemplates()
                .flatMap { template ->
                    loadTemplateVersions(template.id).map { it.toBoardSnapshotCard() }
                }
                .sortedWith(
                    compareByDescending<BoardSnapshotCard> { it.createdAtEpochMs }
                        .thenByDescending { it.id }
                )
                .take(12)
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    snapshots = snapshots,
                    selectedSnapshotId = snapshots.firstOrNull()?.id,
                )
            }
        }
    }

    fun startRemix(snapshotId: Long? = null): TemplateRemixSelection? {
        val snapshot = state.value.snapshots.firstOrNull { it.id == snapshotId }
            ?: state.value.snapshots.firstOrNull()
            ?: return null
        val remix = TemplateRemixSelection(
            draft = snapshot.toTemplateDraft(),
            sourceLabel = "${snapshot.title} v${snapshot.versionNumber}",
            sourceVersion = snapshot.sourceVersion,
        )
        _state.update { current -> current.copy(activeRemix = remix, selectedSnapshotId = snapshot.id) }
        return remix
    }
}
