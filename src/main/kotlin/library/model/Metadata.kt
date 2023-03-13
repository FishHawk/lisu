package library.model

import kotlinx.serialization.Serializable

@Serializable
data class MangaMetadata(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),
)

typealias MangaChapterMetadata = Map<String, Map<String, ChapterMetadata>>

@Serializable
data class ChapterMetadata(
    val name: String? = null,
    val title: String? = null,
)
