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
import io.ktor.util.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.*
import me.fishhawk.lisu.source.LoginSource
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

    @Location("/provider/{providerId}/search")
    data class Search(val providerId: String, val page: Int, val keywords: String)

    @Location("/provider/{providerId}/board/{boardId}")
    data class Board(val providerId: String, val boardId: String, val page: Int)

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
            when (outgoingContent.contentType?.withoutParameters()) {
                ContentType.Image.Any -> CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600))
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

    get<ProviderLocation.Search> { loc ->
        val provider = providerManger.getProvider(loc.providerId).ensure("provider")
        val mangas = provider.search(loc.page, loc.keywords)
        call.respond(mangas)
    }

    get<ProviderLocation.Board> { loc ->
        val filters = call.request.queryParameters.toMap().mapValues { it.value.first().toInt() }
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
    suspend fun search(page: Int, keywords: String): List<MangaDto>
    suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto>?
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

    override suspend fun search(page: Int, keywords: String): List<MangaDto> {
        return library.search(page, keywords)
            .map { it.updateMangaState() }
    }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto>? {
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
        return if (source is LoginSource) source.login(cookies)
        else false
    }

    suspend fun logout() {
        if (source is LoginSource) source.logout()
    }

    suspend fun getComments(mangaId: String, page: Int) =
        source.getComment(mangaId, page).getOrThrow()

    override suspend fun search(page: Int, keywords: String): List<MangaDto> {
        return source.search(page, keywords).getOrThrow()
            .map { it.updateMangaState() }
    }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> {
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
            if (it is LoginSource) {
                ProviderDto(it.id, it.lang, it.boardModels, it.isLogged(), it.loginSite)
            } else {
                ProviderDto(it.id, it.lang, it.boardModels)
            }
        }
        val localProviders = libraries
            .filter { library -> sources.none { source -> source.id == library.id } }
            .map { ProviderDto(it.id, Library.lang, Library.boardModels) }
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
