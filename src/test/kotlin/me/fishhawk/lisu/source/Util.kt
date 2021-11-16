package me.fishhawk.lisu.source

import me.fishhawk.lisu.model.Image
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal fun saveTestImage(providerId: String, image: Image) {
    val testImageDir = Path("test-image").createDirectories()
    val testImageFile = testImageDir.resolve("$providerId.png").toFile()
    image.stream.copyTo(testImageFile.outputStream())
}
