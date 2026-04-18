package io.github.ringwdr.novelignite.relay.routes

import io.github.ringwdr.novelignite.relay.model.RelayGenerateRequest
import io.github.ringwdr.novelignite.relay.model.RelayGenerateResponse
import io.github.ringwdr.novelignite.relay.openrouter.OpenRouterClient
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.installRelayRoutes(openRouterClient: OpenRouterClient = OpenRouterClient()) {
    installRelayRoutes(generate = openRouterClient::generate)
}

internal fun Application.installRelayRoutes(generate: (String, String) -> String) {
    routing {
        post("/v1/generate") {
            val request = call.receive<RelayGenerateRequest>()
            val text = generate(request.model, request.prompt)
            call.respond(HttpStatusCode.OK, RelayGenerateResponse(text = text))
        }
    }
}
