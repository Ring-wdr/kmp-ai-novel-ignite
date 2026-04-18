package io.github.ringwdr.novelignite.features.workshop

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class ActiveWorkshopTemplate(
    val id: Long,
    val title: String,
)

object ActiveWorkshopTemplateStore {
    private val mutableSelection = MutableStateFlow<ActiveWorkshopTemplate?>(null)

    val selection: StateFlow<ActiveWorkshopTemplate?> = mutableSelection.asStateFlow()

    fun select(template: ActiveWorkshopTemplate) {
        mutableSelection.value = template
    }

    fun clear() {
        mutableSelection.value = null
    }
}
