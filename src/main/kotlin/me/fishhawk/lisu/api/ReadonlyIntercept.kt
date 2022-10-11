package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Routing.readonlyIntercept(readonly: Boolean) {
    if (!readonly) return
    intercept(ApplicationCallPipeline.Setup) {
        if (call.request.httpMethod != HttpMethod.Get) {
            call.respondText(
                status = HttpStatusCode.InternalServerError,
                text = "This api is not supported when readonly mode enabled",
            )
            return@intercept finish()
        }
    }
}
