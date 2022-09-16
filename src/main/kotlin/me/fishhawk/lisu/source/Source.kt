package me.fishhawk.lisu.source

import com.tfowl.ktor.client.features.JsoupPlugin
import io.ktor.client.*
import io.ktor.client.engine.java.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.cookies.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.library.model.MangaDetail
import me.fishhawk.lisu.source.model.*
import me.fishhawk.lisu.util.Image
import me.fishhawk.lisu.util.runCatchingException

abstract class Source {
    abstract val id: String
    abstract val lang: String

    abstract val boardModel: Map<BoardId, BoardModel>

    open val loginFeature: LoginFeature? = null

    open val commentFeature: CommentFeature? = null

    protected abstract suspend fun getBoardImpl(boardId: BoardId, page: Int, filters: Parameters): List<Manga>
    suspend fun getBoard(boardId: BoardId, page: Int, filters: Parameters) =
        runCatchingException { getBoardImpl(boardId = boardId, page = page, filters = filters) }

    protected abstract suspend fun getMangaImpl(mangaId: String): MangaDetail
    suspend fun getManga(mangaId: String) =
        runCatchingException { getMangaImpl(mangaId = mangaId) }

    protected abstract suspend fun getContentImpl(mangaId: String, chapterId: String): List<String>
    suspend fun getContent(mangaId: String, chapterId: String) =
        runCatchingException { getContentImpl(mangaId = mangaId, chapterId = chapterId) }

    protected abstract suspend fun getImageImpl(url: String): Image
    suspend fun getImage(url: String) =
        runCatchingException { getImageImpl(url = url) }

    abstract class LoginFeature {
        abstract suspend fun isLogged(): Boolean
        abstract suspend fun logout()

        open val cookiesLogin: CookiesLogin? = null
        open val passwordLogin: PasswordLogin? = null

        interface CookiesLogin {
            val loginSite: String
            val cookieNames: List<String>
            suspend fun login(cookies: Map<String, String>): Boolean
        }

        interface PasswordLogin {
            suspend fun login(username: String, password: String): Boolean
        }
    }

    abstract class CommentFeature {
        abstract suspend fun getCommentImpl(mangaId: String, page: Int): List<Comment>
        suspend fun getComment(mangaId: String, page: Int) =
            runCatchingException { getCommentImpl(mangaId = mangaId, page = page) }
    }

    protected fun Parameters.string(name: String) = get(name) ?: ""
    protected fun Parameters.int(name: String) = get(name)?.toInt() ?: 0
    protected fun Parameters.boolean(name: String, default: Boolean = false) = get(name)?.toBooleanStrict() ?: default
    protected fun Parameters.set(name: String) =
        get(name)?.split(',')?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()

    protected fun Parameters.keywords() = get("keywords") ?: ""

    companion object {
        val cookiesStorage = AcceptAllCookiesStorage()
        val client = HttpClient(Java) {
            install(HttpCookies) {
                storage = cookiesStorage
            }
            install(JsoupPlugin)
            install(ContentNegotiation) {
                json(Json {
                    prettyPrint = true
                    isLenient = true
                })
            }
            expectSuccess = true
        }
    }
}