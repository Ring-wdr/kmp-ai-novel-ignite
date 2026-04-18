package io.github.ringwdr.novelignite.relay.routes

import io.github.ringwdr.novelignite.relay.module
import io.ktor.client.statement.bodyAsText
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

class RelayRoutesTest {
    @Test
    fun generate_returnsOkResponsePayload() = testApplication {
        application {
            module()
        }

        val response = client.post("/v1/generate") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"openrouter/auto","prompt":"Hello"}""")
        }

        assertEquals(200, response.status.value)
        assertEquals("""{"text":"Hello"}""", response.bodyAsText())
    }

    @Test
    fun generate_returnsInjectedGeneratorPayload() = testApplication {
        application {
            install(ContentNegotiation) { json() }
            installRelayRoutes { _, prompt -> "relay:$prompt" }
        }

        val response = client.post("/v1/generate") {
            contentType(ContentType.Application.Json)
            setBody("""{"model":"openrouter/auto","prompt":"Hello"}""")
        }

        assertEquals(200, response.status.value)
        assertEquals("""{"text":"relay:Hello"}""", response.bodyAsText())
    }
}
