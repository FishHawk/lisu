package me.fishhawk.lisu.util

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import java.io.InputStream
import java.nio.file.Path

data class Image(
    val mime: ContentType?,
    val stream: InputStream
) {
    val ext = mime.let {
        if (it == null || it.withoutParameters().match(ContentType.Image.Any)) "png"
        else it.fileExtensions().first()
    }
}

fun Path.toImage(): Image? {
    return inputStream()
        .getOrNull()
        ?.let { Image(null, it) }
}

suspend fun ApplicationCall.respondImage(image: Image) {
    respondOutputStream(image.mime) { image.stream.copyTo(this) }
}
