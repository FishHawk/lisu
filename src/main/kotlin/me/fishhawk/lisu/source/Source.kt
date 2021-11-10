package me.fishhawk.lisu.source

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import java.net.URL

typealias BoardModel = Map<String, List<String>>

enum class Board(val id: String) {
    Popular("popular"),
    Latest("latest"),
    Category("category")
}

interface Source {
    val id: String
    val lang: String
    val icon: URL?
    val boardModels: Map<String, BoardModel>

    suspend fun search(page: Int, keywords: String): List<MangaDto>

    suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto>

    suspend fun getManga(mangaId: String): MangaDetailDto

    suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String>

    suspend fun getImage(url: String): Image
}
