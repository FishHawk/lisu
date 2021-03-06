package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable

@Serializable
data class ChapterDto(
    val id: String,
    val name: String?,
    val title: String?,
    val isLocked: Boolean? = null,
    val updateTime: Long? = null
)
