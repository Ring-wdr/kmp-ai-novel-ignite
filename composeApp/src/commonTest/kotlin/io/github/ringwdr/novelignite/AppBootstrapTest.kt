package io.github.ringwdr.novelignite

import io.github.ringwdr.novelignite.navigation.AppDestination
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppBootstrapTest {
    @Test
    fun appBootstrap_exposesFourTopLevelDestinations() {
        val bootstrap = createAppBootstrap()

        assertTrue(bootstrap.topLevelDestinations.map { it.route }.containsAll(
            listOf("workshop", "templates", "board", "library")
        ))
    }

    @Test
    fun appBootstrap_exposesExpectedDestinationsInOrderWithoutExtras() {
        val bootstrap = createAppBootstrap()

        assertEquals(
            listOf("workshop", "templates", "board", "library"),
            bootstrap.topLevelDestinations.map { it.route },
        )
    }

    @Test
    fun appBootstrap_derivesTopLevelDestinationsFromAppDestinationMetadata() {
        val bootstrap = createAppBootstrap()

        assertEquals(
            AppDestination.topLevelDestinations.map { TopLevelDestination(it.route, it.label) },
            bootstrap.topLevelDestinations,
        )
    }
}
