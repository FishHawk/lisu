package source

import kotlinx.serialization.json.*
import util.Image
import util.createDirAll
import util.outputStream
import util.andThen
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.io.path.Path

internal fun Long.asTimestamp() =
    LocalDateTime.ofInstant(
        Instant.ofEpochMilli(this),
        ZoneId.systemDefault(),
    )

internal fun String.asDateTime(pattern: String) =
    LocalDateTime
        .parse(this, DateTimeFormatter.ofPattern(pattern))

internal fun String.asDate(pattern: String) =
    LocalDate
        .parse(this, DateTimeFormatter.ofPattern(pattern))
        .atStartOfDay()

internal val JsonElement.jsonObjectOrNull: JsonObject?
    get() = this as? JsonObject

internal val JsonElement.jsonArrayOrNull: JsonArray?
    get() = this as? JsonArray

internal val JsonArray.string: List<String>
    get() = map { it.jsonPrimitive.content }

internal val JsonArray.obj: List<JsonObject>
    get() = map { it.jsonObject }

internal fun Source.saveTestImage(image: Image) {
    Path("test-image").createDirAll()
        .andThen { it.resolve("$id.png").outputStream() }
        .onSuccess { image.stream.copyTo(it) }
}
