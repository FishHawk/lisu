package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaMetadataDto(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),

    val collections: Map<String, Map<String, ChapterMetadataDto>>? = null,
    val chapters: Map<String, ChapterMetadataDto>? = null,
)

@Serializable
data class ChapterMetadataDto(
    val name: String?,
    val title: String?,
)

fun MangaDetailDto.toMetadataDetail(): MangaMetadataDto {
    fun mapChapters(chapters: List<ChapterDto>): Map<String, ChapterMetadataDto> {
        return chapters
            .associateBy { it.id }
            .mapValues { (_, it) -> ChapterMetadataDto(it.name, it.title) }
    }
    return MangaMetadataDto(
        title = title,
        authors = authors,
        isFinished = isFinished,
        description = description,
        tags = tags,
        collections = collections.mapValues { mapChapters(it.value) },
        chapters = mapChapters(chapters)
    )
}
