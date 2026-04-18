package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import java.nio.file.Files

class ActiveWorkshopTemplateSelectionPersistenceTest {
    @Test
    fun filePersistence_roundTripsSelectionAndSupportsClearing() {
        val tempDir = Files.createTempDirectory("workshop-template-selection-test")
        val persistenceFile = tempDir.resolve("selection.properties")
        val persistence = FileWorkshopTemplateSelectionPersistence(persistenceFile)
        val selected = ActiveWorkshopTemplate(id = 42, title = "City of Glass")

        persistence.save(selected)

        assertEquals(selected, persistence.load())

        persistence.save(null)

        assertFalse(Files.exists(persistenceFile))
        assertNull(persistence.load())
    }
}
