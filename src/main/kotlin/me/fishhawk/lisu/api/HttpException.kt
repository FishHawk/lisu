package me.fishhawk.lisu.api

import io.ktor.http.*

sealed class HttpException(
    val status: HttpStatusCode,
    text: String
) : RuntimeException() {
    override val message = "$status: $text"

    class NotFound(name: String) : HttpException(HttpStatusCode.NotFound, "$name not found.")
    class Conflict(name: String) : HttpException(HttpStatusCode.Conflict, "$name conflict.")
}

fun <T> T?.ensure(name: String) = this ?: throw HttpException.NotFound(name)

