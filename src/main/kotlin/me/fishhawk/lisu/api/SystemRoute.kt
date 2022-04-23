package me.fishhawk.lisu.api

import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Route.systemRoutes() {
    get("/") {
        call.respondText("Yes, the server is running!")
    }
}
