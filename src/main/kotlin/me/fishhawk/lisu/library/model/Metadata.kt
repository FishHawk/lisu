package me.fishhawk.lisu.library.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaChapterMetadata(
    val collections: Map<String, Map<String, ChapterMetadata>>,
) {
    companion object {
        fun fromMangaDetail(detail: MangaDetail): MangaChapterMetadata {
            return MangaChapterMetadata(
                collections = detail.collections.mapValues { (_, chapters) ->
                    chapters
                        .associateBy { it.id }
                        .mapValues { (_, it) -> ChapterMetadata(it.name, it.title) }
                },
            )
        }
    }
}

@Serializable
data class MangaMetadata(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),
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
            )
        }
    }
}

@Serializable
data class ChapterMetadata(
    val name: String?,
    val title: String?,
)
