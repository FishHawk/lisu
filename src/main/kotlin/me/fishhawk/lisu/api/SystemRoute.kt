package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*

fun Route.systemRoutes() {
    get("/") {
        call.respondText("Yes, the server is running!")
    }
}
