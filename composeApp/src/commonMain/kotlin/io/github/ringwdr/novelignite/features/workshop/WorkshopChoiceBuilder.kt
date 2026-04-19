package io.github.ringwdr.novelignite.features.workshop

class WorkshopChoiceBuilder {
    fun build(markdown: String): List<WorkshopChoice> {
        val focus = extractFocus(markdown)
        return listOf(
            WorkshopChoice(
                id = "continue-${stableChoiceKey(focus)}",
                label = "Continue scene",
                prompt = "Continue the scene from $focus.",
                style = WorkshopChoiceStyle.Primary,
            ),
            WorkshopChoice(
                id = "deepen-${stableChoiceKey(focus)}",
                label = "Deepen tension",
                prompt = "Deepen the tension around $focus.",
            ),
            WorkshopChoice(
                id = "shift-${stableChoiceKey(focus)}",
                label = "Shift perspective",
                prompt = "Shift the perspective around $focus.",
            ),
        )
    }

    private fun extractFocus(markdown: String): String {
        val heading = markdown
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.startsWith("#") }
            ?.trimStart('#')
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if (heading != null) return heading

        val paragraph = markdown
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { it.isNotBlank() }
            ?.removePrefix("- ")
            ?.removePrefix("* ")
            ?.replace(Regex("\\s+"), " ")
            ?.take(80)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        return paragraph ?: "the scene"
    }

    private fun stableChoiceKey(focus: String): String =
        focus.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
            .ifBlank { "scene" }
}
