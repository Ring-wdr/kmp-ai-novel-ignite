package io.github.ringwdr.novelignite

import kotlin.test.Test
import kotlin.test.assertTrue

class AppBootstrapTest {
    @Test
    fun appBootstrap_exposesFourTopLevelDestinations() {
        val bootstrap = createAppBootstrap()

        assertTrue(bootstrap.topLevelDestinations.map { it.route }.containsAll(
            listOf("workshop", "templates", "board", "library")
        ))
    }
}
