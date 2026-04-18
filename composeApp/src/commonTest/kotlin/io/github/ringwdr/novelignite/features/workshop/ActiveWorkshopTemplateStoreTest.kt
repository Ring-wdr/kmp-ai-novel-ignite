package io.github.ringwdr.novelignite.features.workshop

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ActiveWorkshopTemplateStoreTest {
    @Test
    fun configure_restoresAndPersistsSelectionChanges() {
        val persistence = FakeSelectionPersistence(
            initialSelection = ActiveWorkshopTemplate(id = 7, title = "Noir Seoul"),
        )

        try {
            ActiveWorkshopTemplateStore.configure(persistence)

            assertEquals(
                ActiveWorkshopTemplate(id = 7, title = "Noir Seoul"),
                ActiveWorkshopTemplateStore.selection.value,
            )

            val selected = ActiveWorkshopTemplate(id = 11, title = "Moon Archive")
            ActiveWorkshopTemplateStore.select(selected)

            assertEquals(selected, ActiveWorkshopTemplateStore.selection.value)
            assertEquals(selected, persistence.savedSelections.last())

            ActiveWorkshopTemplateStore.clear()

            assertNull(ActiveWorkshopTemplateStore.selection.value)
            assertNull(persistence.savedSelections.last())
        } finally {
            ActiveWorkshopTemplateStore.resetForTests()
        }
    }

    @Test
    fun selectAndClear_ignorePersistenceFailures() {
        try {
            ActiveWorkshopTemplateStore.configure(FailingSelectionPersistence())

            val selected = ActiveWorkshopTemplate(id = 13, title = "Glass Harbor")
            ActiveWorkshopTemplateStore.select(selected)

            assertEquals(selected, ActiveWorkshopTemplateStore.selection.value)

            ActiveWorkshopTemplateStore.clear()

            assertNull(ActiveWorkshopTemplateStore.selection.value)
        } finally {
            ActiveWorkshopTemplateStore.resetForTests()
        }
    }
}

private class FakeSelectionPersistence(
    private val initialSelection: ActiveWorkshopTemplate?,
) : ActiveWorkshopTemplateSelectionPersistence {
    val savedSelections = mutableListOf<ActiveWorkshopTemplate?>()

    override fun load(): ActiveWorkshopTemplate? = initialSelection

    override fun save(selection: ActiveWorkshopTemplate?) {
        savedSelections += selection
    }
}

private class FailingSelectionPersistence : ActiveWorkshopTemplateSelectionPersistence {
    override fun load(): ActiveWorkshopTemplate? = null

    override fun save(selection: ActiveWorkshopTemplate?) {
        error("disk unavailable")
    }
}
