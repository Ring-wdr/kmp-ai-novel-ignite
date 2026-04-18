package io.github.ringwdr.novelignite.relay.model

import kotlinx.serialization.Serializable

@Serializable
data class RelayGenerateRequest(
    val model: String,
    val prompt: String,
)

@Serializable
data class RelayGenerateResponse(
    val text: String,
)
