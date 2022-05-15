package me.fishhawk.lisu.source

import kotlinx.serialization.json.*
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.util.createDirAll
import me.fishhawk.lisu.util.outputStream
import me.fishhawk.lisu.util.then
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

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
    Path("test-image").createDirAll()
        .then { it.resolve("$id.png").outputStream() }
        .onSuccess { image.stream.copyTo(it) }
}
