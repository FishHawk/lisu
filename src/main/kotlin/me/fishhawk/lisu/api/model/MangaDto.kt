package me.fishhawk.lisu.api.model

import kotlinx.serialization.Serializable
import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.library.model.MangaContent
import me.fishhawk.lisu.library.model.MangaDetail
import me.fishhawk.lisu.util.LocalDateTimeSerializer
import java.time.LocalDateTime

@Serializable
data class MangaKeyDto(
    val providerId: String,
    val id: String,
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

    val content: MangaContent,
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
        content = manga.content,
    )
}
