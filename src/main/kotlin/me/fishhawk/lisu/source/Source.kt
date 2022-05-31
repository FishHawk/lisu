package me.fishhawk.lisu.source

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.util.runCatchingException

typealias BoardModel = Map<String, List<String>>

enum class Board(val id: String) {
    Popular("popular"),
    Latest("latest"),
    Category("category")
}

abstract class Source {
    abstract val id: String
    abstract val lang: String
    abstract val boardModels: Map<String, BoardModel>

    protected abstract suspend fun searchImpl(page: Int, keywords: String): List<MangaDto>
    suspend fun search(page: Int, keywords: String) =
        runCatchingException { searchImpl(page = page, keywords = keywords) }

    protected abstract suspend fun getBoardImpl(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto>
    suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>) =
        runCatchingException { getBoardImpl(boardId = boardId, page = page, filters = filters) }

    protected abstract suspend fun getMangaImpl(mangaId: String): MangaDetailDto
    suspend fun getManga(mangaId: String) =
        runCatchingException { getMangaImpl(mangaId = mangaId) }

    protected abstract suspend fun getContentImpl(mangaId: String, chapterId: String): List<String>
    suspend fun getContent(mangaId: String, chapterId: String) =
        runCatchingException { getContentImpl(mangaId = mangaId, chapterId = chapterId) }

    protected abstract suspend fun getImageImpl(url: String): Image
    suspend fun getImage(url: String) =
        runCatchingException { getImageImpl(url = url) }
}

abstract class LoginSource : Source() {
    abstract val loginSite: String
    abstract suspend fun isLogged(): Boolean
    abstract suspend fun logout()
    abstract suspend fun login(cookies: Map<String, String>): Boolean
}
