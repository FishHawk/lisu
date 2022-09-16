package me.fishhawk.lisu.library.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import me.fishhawk.lisu.util.LocalDateTimeSerializer
import java.time.LocalDateTime

data class Manga(
    val id: String,

    var cover: String? = null,
    val updateTime: LocalDateTime? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,
)

data class MangaDetail(
    val id: String,

    var cover: String? = null,
    val updateTime: LocalDateTime? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,

    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),

    val content: MangaContent,
)

@Serializable
sealed interface MangaContent {
    @Serializable
    @SerialName("Collections")
    data class Collections(val collections: Map<String, List<Chapter>> = emptyMap()) : MangaContent

    @Serializable
    @SerialName("Chapters")
    data class Chapters(val chapters: List<Chapter> = emptyList()) : MangaContent

    @Serializable
    @SerialName("SingleChapter")
    data class SingleChapter(var preview: List<String> = emptyList()) : MangaContent
}

@Serializable
data class Chapter(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updateTime: LocalDateTime? = null,
    val isLocked: Boolean = false,
)