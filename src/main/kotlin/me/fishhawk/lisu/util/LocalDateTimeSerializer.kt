package me.fishhawk.lisu.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

object LocalDateTimeSerializer : KSerializer<LocalDateTime> {
    override fun serialize(encoder: Encoder, value: LocalDateTime) {
        encoder.encodeLong(value.atZone(ZoneId.systemDefault()).toEpochSecond())
    }

    override fun deserialize(decoder: Decoder): LocalDateTime {
        return Instant.ofEpochSecond(decoder.decodeLong()).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }

    override val descriptor = PrimitiveSerialDescriptor("LocalDateTime", PrimitiveKind.LONG)
}
