package io.github.ringwdr.novelignite.features.board

import io.github.ringwdr.novelignite.data.remote.BoardCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

data class BoardUiState(
    val title: String = "Board",
    val subtitle: String = "Browse public templates and remixes.",
    val cards: List<BoardCard> = emptyList(),
)

class BoardViewModel {
    private val _state = MutableStateFlow(BoardUiState())
    val state: StateFlow<BoardUiState> = _state
}
