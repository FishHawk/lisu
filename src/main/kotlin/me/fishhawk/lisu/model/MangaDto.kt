package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

const val DefaultTagName = ""

@Serializable
data class MangaKeyDto(
    val providerId: String,
    val id: String,
)

@Serializable
enum class MangaState {
    Local, Remote, RemoteInLibrary
}

@Serializable
data class MangaDto(
    val state: MangaState = MangaState.Local,

    val providerId: String,
    val id: String,

    var cover: String? = null,
    val updateTime: Long? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,
)

@Serializable
data class MangaDetailDto(
    val state: MangaState = MangaState.Local,

    val providerId: String,
    val id: String,

    var cover: String? = null,
    val updateTime: Long? = null,
    val title: String? = null,
    val authors: List<String> = emptyList(),
    val isFinished: Boolean? = null,

    val description: String?,
    val tags: Map<String, List<String>> = emptyMap(),

    val collections: Map<String, List<ChapterDto>> = emptyMap(),
    val chapters: List<ChapterDto> = emptyList(),
    var preview: List<String> = emptyList()
)
