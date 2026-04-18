package io.github.ringwdr.novelignite.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TemplatePostDto(
    val id: String,
    val title: String,
    val summary: String,
    val likes: Int,
    @SerialName("remix_count")
    val remixes: Int,
)

data class BoardCard(
    val id: String,
    val title: String,
    val summary: String,
    val likeCount: Int,
    val remixCount: Int,
)

fun TemplatePostDto.toBoardCard(): BoardCard = BoardCard(
    id = id,
    title = title,
    summary = summary,
    likeCount = likes,
    remixCount = remixes,
)
