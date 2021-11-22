package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

const val DefaultTagName = ""

@Serializable
data class MetadataDetailDto(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
    val description: String? = null,
    val tags: Map<String, List<String>>? = null,

    val collections: Map<String, List<ChapterDto>>? = null,
    val chapters: List<ChapterDto>? = null,
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
) {
    val metadataDetail
        get() = MetadataDetailDto(
            title = title,
            authors = authors,
            isFinished = isFinished,
            description = description,
            tags = tags,
            collections = collections,
            chapters = chapters
        )
}
