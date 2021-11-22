package me.fishhawk.lisu.source

import kotlinx.serialization.json.*
import me.fishhawk.lisu.model.Image
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path
import kotlin.io.path.createDirectories

internal fun JsonElement.asDateTimeToEpochSecond(pattern: String) =
    LocalDateTime
        .parse(jsonPrimitive.content, DateTimeFormatter.ofPattern(pattern))
        .atZone(ZoneId.systemDefault())
        .toEpochSecond()

internal fun JsonElement.asDateToEpochSecond(pattern: String) =
    LocalDate
        .parse(jsonPrimitive.content, DateTimeFormatter.ofPattern(pattern))
        .atStartOfDay()
        .atZone(ZoneId.systemDefault())
        .toEpochSecond()

internal val JsonArray.string: List<String> get() = map { it.jsonPrimitive.content }
internal val JsonArray.obj: List<JsonObject> get() = map { it.jsonObject }

internal fun Source.saveTestImage(image: Image) {
    val testImageDir = Path("test-image").createDirectories()
    val testImageFile = testImageDir.resolve("$id.png").toFile()
    image.stream.copyTo(testImageFile.outputStream())
}
