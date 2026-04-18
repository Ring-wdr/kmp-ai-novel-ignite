package io.github.ringwdr.novelignite.features.library

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

data class LibraryState(
    val title: String = "Library",
    val remixHint: String = "Browse saved templates and remix them locally.",
)

class LibraryViewModel {
    private val _state = MutableStateFlow(LibraryState())
    val state: StateFlow<LibraryState> = _state

    fun focusLocalRemix() {
        _state.update {
            it.copy(remixHint = "Local remix is ready for template reuse.")
        }
    }
}
