package me.fishhawk.lisu.source.model

import kotlinx.serialization.Serializable
import me.fishhawk.lisu.util.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class Comment(
    val username: String,
    val content: String,
    @Serializable(with = LocalDateTimeSerializer::class)
    val createTime: LocalDateTime? = null,
    val vote: Int? = null,
    val subComments: List<Comment>? = null,
)