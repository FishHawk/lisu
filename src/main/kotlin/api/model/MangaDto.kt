package api.model

import kotlinx.serialization.Serializable
import library.model.Chapter
import library.model.Manga
import library.model.MangaDetail
import util.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class MangaKeyDto(
    val providerId: String,
    val id: String,
)

@Serializable
data class MangaPageDto(
    val list: List<MangaDto>,
    val nextKey: String?,
)

enum class MangaState {
    Local, Remote, RemoteInLibrary
}

@Serializable
data class MangaDto(
    val state: MangaState = MangaState.Local,
    val providerId: String,

    val id: String,

    var cover: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updateTime: LocalDateTime? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,
) {
    constructor(
        state: MangaState,
        providerId: String,
        manga: Manga,
    ) : this(
        state = state,
        providerId = providerId,
        id = manga.id,
        cover = manga.cover,
        updateTime = manga.updateTime,
        title = manga.title,
        authors = manga.authors,
        isFinished = manga.isFinished,
    )
}

@Serializable
data class MangaDetailDto(
    val state: MangaState = MangaState.Local,
    val providerId: String,

    val id: String,

    var cover: String? = null,
    @Serializable(with = LocalDateTimeSerializer::class)
    val updateTime: LocalDateTime? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,

    val description: String? = null,
    val tags: Map<String, List<String>> = emptyMap(),

    val collections: Map<String, List<Chapter>> = emptyMap(),
    val chapterPreviews: List<String>? = null,
) {
    constructor(
        state: MangaState,
        providerId: String,
        manga: MangaDetail,
    ) : this(
        state = state,
        providerId = providerId,
        id = manga.id,
        cover = manga.cover,
        updateTime = manga.updateTime,
        title = manga.title,
        authors = manga.authors,
        isFinished = manga.isFinished,
        description = manga.description,
        tags = manga.tags,
        collections = manga.collections,
        chapterPreviews = manga.chapterPreviews,
    )
}
