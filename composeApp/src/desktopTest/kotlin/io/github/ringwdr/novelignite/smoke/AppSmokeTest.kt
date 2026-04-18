package io.github.ringwdr.novelignite.smoke

import io.github.ringwdr.novelignite.createAppBootstrap
import kotlin.test.Test
import kotlin.test.assertTrue

class AppSmokeTest {
    @Test
    fun bootstrap_containsExpectedMvpAreas() {
        val labels = createAppBootstrap().topLevelDestinations.map { it.label }
        assertTrue(labels.containsAll(listOf("Workshop", "Templates", "Board", "Library")))
    }
}
