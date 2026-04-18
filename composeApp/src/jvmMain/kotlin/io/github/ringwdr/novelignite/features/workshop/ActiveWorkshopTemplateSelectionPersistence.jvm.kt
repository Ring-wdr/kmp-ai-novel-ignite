package io.github.ringwdr.novelignite.features.workshop

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.Base64

class FileWorkshopTemplateSelectionPersistence(
    private val file: Path,
) : ActiveWorkshopTemplateSelectionPersistence {
    override fun load(): ActiveWorkshopTemplate? {
        if (!Files.exists(file)) return null

        return runCatching {
            val lines = Files.readAllLines(file, StandardCharsets.UTF_8)
            if (lines.size < 2) return null

            val id = lines[0].trim().toLongOrNull() ?: return null
            val title = String(Base64.getUrlDecoder().decode(lines[1]), StandardCharsets.UTF_8)
            ActiveWorkshopTemplate(id = id, title = title)
        }.getOrNull()
    }

    override fun save(selection: ActiveWorkshopTemplate?) {
        if (selection == null) {
            Files.deleteIfExists(file)
            return
        }

        val parent = file.parent
        if (parent != null) {
            Files.createDirectories(parent)
        }

        Files.write(
            file,
            listOf(
                selection.id.toString(),
                Base64.getUrlEncoder().encodeToString(selection.title.toByteArray(StandardCharsets.UTF_8)),
            ),
            StandardCharsets.UTF_8,
        )
    }
}
