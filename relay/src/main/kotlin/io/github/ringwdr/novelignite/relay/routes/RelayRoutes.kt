package io.github.ringwdr.novelignite.relay.routes

import io.github.ringwdr.novelignite.relay.model.RelayGenerateRequest
import io.github.ringwdr.novelignite.relay.model.RelayGenerateResponse
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.post
import io.ktor.server.routing.routing

fun Application.installRelayRoutes() {
    routing {
        post("/v1/generate") {
            val request = call.receive<RelayGenerateRequest>()
            call.respond(HttpStatusCode.OK, RelayGenerateResponse(text = request.prompt))
        }
    }
}
