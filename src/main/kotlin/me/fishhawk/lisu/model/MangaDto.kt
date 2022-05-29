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
    val authors: List<String>? = null,
    val isFinished: Boolean? = null,
)

@Serializable
data class MangaDetailDto(
    val state: MangaState = MangaState.Local,

    val providerId: String,
    val id: String,

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
