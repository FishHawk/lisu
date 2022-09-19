package me.fishhawk.lisu.library.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaMetadata(
    val title: String?,
    val authors: List<String>?,
    val isFinished: Boolean?,
    val description: String?,
    val tags: Map<String, List<String>>,

    val collections: Map<String, Map<String, ChapterMetadata>>?,
    val chapters: Map<String, ChapterMetadata>?,
) {
    companion object {
        fun fromMangaDetail(detail: MangaDetail): MangaMetadata {
            fun mapChapters(chapters: List<Chapter>): Map<String, ChapterMetadata> {
                return chapters
                    .associateBy { it.id }
                    .mapValues { (_, it) -> ChapterMetadata(it.name, it.title) }
            }
            return MangaMetadata(
                title = detail.title,
                authors = detail.authors,
                isFinished = detail.isFinished,
                description = detail.description,
                tags = detail.tags,
                collections = null,
                chapters = null,
//                collections = (detail.content as? MangaContent.Collections)?.let { content ->
//                    content.collections.mapValues { mapChapters(it.value) }
//                },
//                chapters = (detail.content as? MangaContent.Chapters)?.let { content ->
//                    mapChapters(content.chapters)
//                },
            )
        }
    }
}

@Serializable
data class ChapterMetadata(
    val name: String?,
    val title: String?,
)
