package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val id: String,
    val name: String,
    val title: String,
    val isLocked: Boolean? = null,
    val updateTime: Long? = null
)

@Serializable
data class MetadataDetailDto(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
    val description: String? = null,
    val tags: Map<String, List<String>>? = null
)

@Serializable
data class MangaDetailDto(
    val providerId: String,
    val id: String,

    val inLibrary: Boolean = false,

    var cover: String? = null,
    val updateTime: Long? = null,

    val title: String?,
    val authors: List<String>?,
    val isFinished: Boolean?,
    val description: String?,
    val tags: Map<String, List<String>>?,

    val collections: Map<String, List<ChapterDto>>? = null,
    val chapters: List<ChapterDto>? = null,
    var preview: List<String>? = null
)
