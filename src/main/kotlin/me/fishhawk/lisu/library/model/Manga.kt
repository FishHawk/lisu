package me.fishhawk.lisu.library.model

import kotlinx.serialization.Serializable
import me.fishhawk.lisu.util.LocalDateTimeSerializer
import java.time.LocalDateTime

data class MangaPage(
    val list: List<Manga>,
    val nextKey: String? = null,
)

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

    val collections: Map<String, List<Chapter>> = emptyMap(),
    val chapterPreviews: List<String>? = null,
)

@Serializable
data class Chapter(
    val id: String,
    val name: String? = null,
    val title: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updateTime: LocalDateTime? = null,
    val isLocked: Boolean = false,
)