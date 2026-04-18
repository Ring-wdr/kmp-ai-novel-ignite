package io.github.ringwdr.novelignite.domain.inference

sealed interface GenerationEvent {
    data class Token(val text: String) : GenerationEvent
    data class Final(val text: String) : GenerationEvent
    data class Error(val message: String) : GenerationEvent
}
