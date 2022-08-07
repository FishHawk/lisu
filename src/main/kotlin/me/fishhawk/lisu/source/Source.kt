package me.fishhawk.lisu.source

import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.CommentDto
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.util.runCatchingException

enum class BoardId { Ranking, Latest, Category }
typealias BoardModel = Map<String, List<String>>

abstract class Source {
    abstract val id: String
    abstract val lang: String
    abstract val boardModels: Map<BoardId, BoardModel>

    open val loginFeature: LoginFeature? = null
    open val commentFeature: CommentFeature? = null

    protected abstract suspend fun searchImpl(page: Int, keywords: String): List<MangaDto>
    suspend fun search(page: Int, keywords: String) =
        runCatchingException { searchImpl(page = page, keywords = keywords) }

    protected abstract suspend fun getBoardImpl(boardId: BoardId, page: Int, filters: Map<String, Int>): List<MangaDto>
    suspend fun getBoard(boardId: BoardId, page: Int, filters: Map<String, Int>) =
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

    interface LoginFeature {
        val loginSite: String
        suspend fun isLogged(): Boolean
        suspend fun logout()
        suspend fun login(cookies: Map<String, String>): Boolean
    }

    abstract class CommentFeature {
        abstract suspend fun getCommentImpl(mangaId: String, page: Int): List<CommentDto>
        suspend fun getComment(mangaId: String, page: Int) =
            runCatchingException { getCommentImpl(mangaId = mangaId, page = page) }
    }
}