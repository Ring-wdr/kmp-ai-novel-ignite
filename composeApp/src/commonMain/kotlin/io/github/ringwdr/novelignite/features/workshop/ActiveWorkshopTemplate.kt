package io.github.ringwdr.novelignite.features.workshop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveWorkshopTemplate(
    val id: Long,
    val title: String,
)

interface ActiveWorkshopTemplateSelectionPersistence {
    fun load(): ActiveWorkshopTemplate?

    fun save(selection: ActiveWorkshopTemplate?)
}

object ActiveWorkshopTemplateStore {
    private val mutableSelection = MutableStateFlow<ActiveWorkshopTemplate?>(null)
    private var persistence: ActiveWorkshopTemplateSelectionPersistence =
        NoOpActiveWorkshopTemplateSelectionPersistence

    val selection: StateFlow<ActiveWorkshopTemplate?> = mutableSelection.asStateFlow()

    fun configure(persistence: ActiveWorkshopTemplateSelectionPersistence) {
        this.persistence = persistence
        mutableSelection.value = runCatching { persistence.load() }.getOrNull()
    }

    fun select(template: ActiveWorkshopTemplate) {
        mutableSelection.value = template
        runCatching { persistence.save(template) }
    }

    fun clear() {
        mutableSelection.value = null
        runCatching { persistence.save(null) }
    }

    internal fun resetForTests() {
        persistence = NoOpActiveWorkshopTemplateSelectionPersistence
        mutableSelection.value = null
    }
}

private object NoOpActiveWorkshopTemplateSelectionPersistence :
    ActiveWorkshopTemplateSelectionPersistence {
    override fun load(): ActiveWorkshopTemplate? = null

    override fun save(selection: ActiveWorkshopTemplate?) = Unit
}
