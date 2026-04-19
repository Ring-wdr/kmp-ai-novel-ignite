package io.github.ringwdr.novelignite.features.templates

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val templateEnrichmentJson = Json { ignoreUnknownKeys = true }

fun buildTemplateEnrichmentPrompt(draft: TemplateDraft): String = buildString {
    appendLine("너는 소설 템플릿을 보강하는 한국어 창작 보조자다.")
    appendLine("사용자가 이미 적은 의도는 유지하고 확장한다.")
    appendLine("반드시 단일 JSON 객체만 반환한다.")
    appendLine("JSON 키는 title, genre, premise, promptBlocks 이다.")
    appendLine("promptBlocks는 장면을 계속 이어가기 쉬운 한국어 규칙 문장 배열로 작성한다.")
    appendLine("비어 있는 항목은 자연스럽게 채우고, 이미 있는 항목은 더 선명하게 다듬되 의도를 바꾸지 않는다.")
    appendLine("현재 템플릿 초안:")
    appendLine(templateEnrichmentJson.encodeToString(TemplateEnrichmentPayload.fromDraft(draft)))
}

fun parseTemplateEnrichmentResponse(
    raw: String,
    original: TemplateDraft,
): TemplateDraft {
    val payload = decodeTemplateEnrichmentPayload(raw)
    return TemplateDraft(
        title = payload.title.ifBlank { original.title },
        genre = payload.genre.ifBlank { original.genre },
        premise = payload.premise.ifBlank { original.premise },
        promptBlocks = payload.promptBlocks
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .ifEmpty { original.promptBlocks },
    )
}

internal fun buildFallbackEnrichedTemplateDraft(draft: TemplateDraft): TemplateDraft {
    val normalizedTitle = draft.title.ifBlank {
        when {
            draft.genre.isNotBlank() -> "${draft.genre} Story Template"
            else -> "Story Template"
        }
    }
    val normalizedPremise = draft.premise.ifBlank {
        "사용자와 AI가 함께 다음 장면을 밀어가는 장면 중심 이야기."
    }
    return TemplateDraft(
        title = normalizedTitle,
        genre = draft.genre.ifBlank { "Speculative Fiction" },
        premise = normalizedPremise,
        promptBlocks = draft.promptBlocks.ifEmpty {
            listOf(
                "항상 현재 장면의 긴장과 감정을 반영한다.",
                "사용자가 그대로 보낼 수 있는 다음 턴 문장 2~3개를 제안한다.",
            )
        },
    )
}

private fun decodeTemplateEnrichmentPayload(raw: String): TemplateEnrichmentPayload =
    runCatching {
        templateEnrichmentJson.decodeFromString<TemplateEnrichmentPayload>(raw.trim())
    }.recoverCatching {
        templateEnrichmentJson.decodeFromString<TemplateEnrichmentPayload>(extractJsonObject(raw))
    }.getOrThrow()

private fun extractJsonObject(raw: String): String {
    val start = raw.indexOf('{')
    val end = raw.lastIndexOf('}')
    require(start >= 0 && end > start) { "No JSON object found in enrichment response." }
    return raw.substring(start, end + 1)
}

@Serializable
private data class TemplateEnrichmentPayload(
    val title: String = "",
    val genre: String = "",
    val premise: String = "",
    val promptBlocks: List<String> = emptyList(),
) {
    companion object {
        fun fromDraft(draft: TemplateDraft): TemplateEnrichmentPayload = TemplateEnrichmentPayload(
            title = draft.title,
            genre = draft.genre,
            premise = draft.premise,
            promptBlocks = draft.promptBlocks,
        )
    }
}
