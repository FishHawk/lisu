package me.fishhawk.lisu.source.manhuaren

import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.util.cio.*
import io.ktor.utils.io.*
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal suspend fun saveTestImage(providerId: String, response: HttpResponse) {
    val readChannel = response.receive<ByteReadChannel>()
    val testImagePath = Path("test-image").createDirectories()
    readChannel.copyTo(testImagePath.resolve("$providerId.png").toFile().writeChannel())
}
