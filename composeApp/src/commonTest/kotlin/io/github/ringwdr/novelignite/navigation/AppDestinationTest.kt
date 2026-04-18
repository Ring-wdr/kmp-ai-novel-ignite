package io.github.ringwdr.novelignite.navigation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.Json

class AppDestinationTest {
    @Test
    fun workshopDestination_roundTripsThroughSerialization() {
        val json = Json
        val encoded = json.encodeToString(AppDestination.serializer(), AppDestination.Workshop)
        val decoded = json.decodeFromString(AppDestination.serializer(), encoded)

        assertEquals(AppDestination.Workshop, decoded)
    }
}
