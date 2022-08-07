package me.fishhawk.lisu.model

import kotlinx.serialization.Serializable
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.BoardModel

@Serializable
data class ProviderDto(
    val id: String,
    val lang: String,
    val boardModels: Map<BoardId, BoardModel>,
    val isLogged: Boolean? = null,
    val loginSite: String? = null,
)
