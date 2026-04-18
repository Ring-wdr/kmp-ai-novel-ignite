package io.github.ringwdr.novelignite.features.library

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

class LibraryViewModel(
    private val loadState: suspend () -> LibraryState = ::loadLocalLibraryState,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default),
) {
    private val _state = MutableStateFlow(LibraryState(isLoading = true))
    val state: StateFlow<LibraryState> = _state

    init {
        refresh()
    }

    fun refresh() {
        _state.value = _state.value.copy(isLoading = true, errorMessage = null)
        scope.launch {
            _state.value = runCatching { loadState() }
                .getOrElse { throwable ->
                    LibraryState(
                        isLoading = false,
                        errorMessage = throwable.message ?: "Failed to load local library.",
                    )
                }
        }
    }

    fun clear() {
        scope.cancel()
    }
}
