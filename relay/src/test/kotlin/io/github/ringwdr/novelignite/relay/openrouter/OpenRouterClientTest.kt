package io.github.ringwdr.novelignite.relay.openrouter

import kotlin.test.Test
import kotlin.test.assertEquals

class OpenRouterClientTest {
    @Test
    fun generate_returnsPromptSynchronously() {
        val client = OpenRouterClient()

        val response = client.generate(
            model = "openrouter/auto",
            prompt = "Hello",
        )

        assertEquals("Hello", response)
    }
}
