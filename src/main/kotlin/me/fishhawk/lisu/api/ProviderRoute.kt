package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.*
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager

@OptIn(KtorExperimentalLocationsAPI::class)
private object ProviderLocation {
    @Location("/provider")
    object Provider

    @Location("/provider/{providerId}/icon")
    data class Icon(val providerId: String)

    @Location("/provider/{providerId}/login")
    data class Login(val providerId: String)

    @Location("/provider/{providerId}/logout")
    data class Logout(val providerId: String)

    @Location("/provider/{providerId}/board/{boardId}")
    data class Board(val providerId: String, val boardId: BoardId, val page: Int)

    @Location("/provider/{providerId}/manga/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/provider/{providerId}/manga/{mangaId}/comment")
    data class Comment(val providerId: String, val mangaId: String, val page: Int)

    @Location("/provider/{providerId}/manga/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String, val imageId: String)

    @Location("/provider/{providerId}/manga/{mangaId}/content/{collectionId}/{chapterId}")
    data class Content(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String
    )

    @Location("/provider/{providerId}/manga/{mangaId}/image/{collectionId}/{chapterId}/{imageId}")
    data class Image(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
        val imageId: String
    )
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.providerRoutes(providerManger: ProviderManager) {
    install(CachingHeaders) {
        options { _, outgoingContent ->
            when (outgoingContent.contentType?.withoutParameters()?.contentType) {
                ContentType.Image.Any.contentType -> {
                    CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600))
                }
                else -> null
            }
        }
    }

    get<ProviderLocation.Provider> {
        val providers = providerManger.listProvider()
        call.respond(providers)
    }

    get<ProviderLocation.Icon> { loc ->
        val provider = providerManger.getRemoteProvider(loc.providerId).ensure("provider")
        val image = provider.getIcon().ensure("icon")
        call.respondImage(image)
    }

    post<ProviderLocation.Login> { loc ->
        val cookies = call.receive<Map<String, String>>()
        val provider = providerManger.getRemoteProvider(loc.providerId).ensure("provider")
        val isSuccess = provider.login(cookies)
        if (isSuccess) call.respondText("Success")
        else call.respondText(status = HttpStatusCode.InternalServerError, text = "")
    }

    post<ProviderLocation.Logout> { loc ->
        val provider = providerManger.getRemoteProvider(loc.providerId).ensure("provider")
        provider.logout()
        call.respondText("Success")
    }

    get<ProviderLocation.Board> { loc ->
        val filters = call.request.queryParameters
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val mangas = provider.getBoard(loc.boardId, loc.page, filters).ensure("board")
        call.respond(mangas)
    }

    get<ProviderLocation.Manga> { loc ->
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val mangaDetail = provider.getManga(loc.mangaId).ensure("manga")
        call.respond(mangaDetail)
    }

    get<ProviderLocation.Comment> { loc ->
        val provider = providerManger.getRemoteProvider(loc.providerId).ensure("provider")
        val comments = provider.getComments(loc.mangaId, loc.page).ensure("comment")
        call.respond(comments)
    }

    get<ProviderLocation.Cover> { loc ->
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val cover = provider.getCover(loc.mangaId, loc.imageId).ensure("cover")
        call.respondImage(cover)
    }

    get<ProviderLocation.Content> { loc ->
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val content = provider.getContent(loc.mangaId, loc.collectionId.trim(), loc.chapterId).ensure("chapter")
        call.respond(content)
    }

    get<ProviderLocation.Image> { loc ->
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val image = provider.getImage(loc.mangaId, loc.collectionId.trim(), loc.chapterId, loc.imageId).ensure("image")
        call.respondImage(image)
    }
}

interface Provider {
    suspend fun getBoard(boardId: BoardId, page: Int, filters: Parameters): List<MangaDto>?
    suspend fun getManga(mangaId: String): MangaDetailDto?
    suspend fun getCover(mangaId: String, imageId: String): Image?
    suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String>?
    suspend fun getImage(mangaId: String, collectionId: String, chapterId: String, imageId: String): Image?
}

class LocalProvider(
    private val library: Library
) : Provider {
    private fun MangaDto.updateMangaState() =
        copy(state = MangaState.Local)

    private fun MangaDetailDto.updateMangaState() =
        copy(state = MangaState.Local)

    override suspend fun getBoard(boardId: BoardId, page: Int, filters: Parameters): List<MangaDto>? {
        return library.getBoard(boardId, page)
            ?.map { it.updateMangaState() }
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto? {
        return library.getManga(mangaId)?.getDetail()
            ?.updateMangaState()
    }

    override suspend fun getCover(mangaId: String, imageId: String): Image? {
        return library.getManga(mangaId)?.getCover()
    }

    override suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String>? {
        return library.getManga(mangaId)?.getChapter(collectionId, chapterId)?.getContent()
    }

    override suspend fun getImage(mangaId: String, collectionId: String, chapterId: String, imageId: String): Image? {
        return library.getManga(mangaId)?.getChapter(collectionId, chapterId)?.getImage(imageId)
    }
}

class RemoteProvider(
    private val source: Source,
    private val library: Library?
) : Provider {
    private fun MangaDto.updateMangaState() =
        copy(state = if (library?.getManga(id) == null) MangaState.Remote else MangaState.RemoteInLibrary)

    private fun MangaDetailDto.updateMangaState() =
        copy(state = if (library?.getManga(id) == null) MangaState.Remote else MangaState.RemoteInLibrary)

    fun getIcon(): Image? {
        return source::class.java.getResourceAsStream("icon.png")
            ?.let { Image(ContentType.Image.PNG, it) }
    }

    suspend fun login(cookies: Map<String, String>): Boolean {
        return source.loginFeature?.login(cookies) ?: false
    }

    suspend fun logout() {
        source.loginFeature?.logout()
    }

    suspend fun getComments(mangaId: String, page: Int): List<CommentDto> {
        return source.commentFeature!!.getComment(mangaId, page).getOrThrow()
    }

    override suspend fun getBoard(boardId: BoardId, page: Int, filters: Parameters): List<MangaDto> {
        return source.getBoard(boardId, page, filters).getOrThrow()
            .map { it.updateMangaState() }
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto {
        return source.getManga(mangaId).getOrThrow()
            .updateMangaState()
    }

    override suspend fun getCover(mangaId: String, imageId: String): Image? {
        // Using Cache
        return library?.getManga(mangaId)?.getCover()
            ?: source.getImage(imageId).getOrNull()
    }

    override suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String>? {
        // Using Cache
        return library?.getManga(mangaId)?.getChapter(collectionId, chapterId)?.takeIf { it.isFinished() }?.getContent()
            ?: source.getContent(mangaId, chapterId).getOrNull()
    }

    override suspend fun getImage(mangaId: String, collectionId: String, chapterId: String, imageId: String): Image? {
        // Using Cache
        return library?.getManga(mangaId)?.getChapter(collectionId, chapterId)?.getImage(imageId)
            ?: source.getImage(imageId).getOrNull()

    }
}

class ProviderManager(
    private val libraryManager: LibraryManager,
    private val sourceManager: SourceManager
) {
    suspend fun listProvider(): List<ProviderDto> {
        val sources = sourceManager.listSources()
        val libraries = libraryManager.listLibraries()

        val remoteProviders = sources.map {
            ProviderDto(
                id = it.id,
                lang = it.lang,
                boardModels = it.boardModel,
                isLogged = it.loginFeature?.isLogged(),
                loginSite = it.loginFeature?.loginSite
            )
        }
        val localProviders = libraries
            .filter { library -> sources.none { source -> source.id == library.id } }
            .map {
                ProviderDto(
                    id = it.id,
                    lang = Library.lang,
                    boardModels = Library.boardModel
                )
            }
        return remoteProviders + localProviders
    }

    fun getProvider(providerId: String): Provider? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId)
        return if (source != null) RemoteProvider(source, library)
        else if (library != null) LocalProvider(library)
        else null
    }

    fun getRemoteProvider(providerId: String): RemoteProvider? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId)
        return if (source != null) RemoteProvider(source, library) else null
    }
}
