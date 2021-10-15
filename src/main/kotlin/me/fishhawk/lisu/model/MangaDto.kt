package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

@Serializable
data class MetadataDto(
    val title: String? = null,
    val authors: List<String>? = null,
    val isFinished: Boolean? = null
)

@Serializable
data class MangaDto(
    val providerId: String,
    val id: String,

    var cover: String?,
    val updateTime: Long?,

    val title: String?,
    val authors: List<String>?,
    val isFinished: Boolean?,
)