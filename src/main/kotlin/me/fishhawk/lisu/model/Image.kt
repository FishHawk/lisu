package me.fishhawk.lisu.model

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import java.io.InputStream
import java.nio.file.Path

data class Image(
    val mime: ContentType?,
    val stream: InputStream
)

fun Path.toImage() = Image(null, this.toFile().inputStream())

suspend fun ApplicationCall.respondImage(image: Image) {
    respondOutputStream(image.mime) { image.stream.copyTo(this) }
}
