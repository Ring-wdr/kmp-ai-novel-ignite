package io.github.ringwdr.novelignite.relay.routes

import io.github.ringwdr.novelignite.relay.module
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RelayRoutesTest {
    @Test
    fun generate_returnsOkResponse() = testApplication {
        application {
            module()
        }

        val response = client.post("/v1/generate") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"openrouter/auto","prompt":"Hello"}""")
        }

        assertEquals(200, response.status.value)
    }
}
