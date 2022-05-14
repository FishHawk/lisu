package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.*
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager

@OptIn(KtorExperimentalLocationsAPI::class)
private object ProviderLocation {
    @Location("/provider")
    object Provider

    @Location("/provider/{providerId}/icon")
    data class Icon(val providerId: String)

    @Location("/provider/{providerId}/search")
    data class Search(val providerId: String, val page: Int, val keywords: String)

    @Location("/provider/{providerId}/board/{boardId}")
    data class Board(val providerId: String, val boardId: String, val page: Int)

    @Location("/provider/{providerId}/manga/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/provider/{providerId}/manga/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String, val imageId: String)

    @Location("/provider/{providerId}/manga/{mangaId}/chapter/{collectionId}/{chapterId}")
    data class Content(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String
    )

    @Location("/provider/{providerId}/manga/{mangaId}/chapter/{collectionId}/{chapterId}/image/{imageId}")
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
    override suspend fun search(page: Int, keywords: String): List<MangaDto> {
        return library.search(page, keywords)
    }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto>? {
        return library.getBoard(boardId, page)
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto? {
        return library.getManga(mangaId)?.getDetail()
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
    fun getIcon(): Image? {
        return source::class.java.getResourceAsStream("icon.png")
            ?.let { Image(ContentType.Image.PNG, it) }
    }

    override suspend fun search(page: Int, keywords: String): List<MangaDto> {
        return source.search(page, keywords).getOrThrow()
            .map { it.copy(inLibrary = library?.getManga(it.id) != null) }
    }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> {
        return source.getBoard(boardId, page, filters).getOrThrow()
            .map { it.copy(inLibrary = library?.getManga(it.id) != null) }
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto {
        return source.getManga(mangaId).getOrThrow()
            .copy(inLibrary = library?.getManga(mangaId) != null)
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
    fun listProvider(): List<ProviderDto> {
        val sources = sourceManager.listSources()
        val libraries = libraryManager.listLibraries()

        val remoteProviders = sources.map { ProviderDto(it.id, it.lang, it.boardModels) }
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
