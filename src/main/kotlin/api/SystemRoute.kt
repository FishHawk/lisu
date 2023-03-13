package api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.systemRoute() {
    get("/") {
        call.respondText("Yes, the server is running!")
    }
}
