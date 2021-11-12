package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.locations.put
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MetadataDetailDto
import me.fishhawk.lisu.model.ProviderDto
import me.fishhawk.lisu.model.respondImage
import me.fishhawk.lisu.source.SourceManager
import java.io.File

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

    @Location("/provider/{providerId}/manga/{mangaId}/metadata")
    data class Metadata(val providerId: String, val mangaId: String)

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
fun Route.providerRoutes(libraryManager: LibraryManager, sourceManager: SourceManager) {
    get<ProviderLocation.Provider> {
        val sources = sourceManager.listSources()
        val libraries = libraryManager.listLibraries()

        val remoteProviders = sources.map { ProviderDto(it.id, it.lang, it.boardModels) }
        val localProviders = libraries
            .filter { library -> sources.none { source -> source.id == library.id } }
            .map { ProviderDto(it.id, Library.lang, Library.boardModels) }
        call.respond(remoteProviders + localProviders)
    }

    get<ProviderLocation.Icon> { loc ->
        val icon = sourceManager.getSource(loc.providerId).ensure("provider")
            .icon?.let { File(it.file) }.ensure("icon")
        if (icon.exists()) call.respondFile(icon)
        else throw HttpException.NotFound("icon")
    }

    get<ProviderLocation.Search> { loc ->
        val mangas = sourceManager.getSource(loc.providerId)
            ?.search(loc.page, loc.keywords)
            ?: libraryManager.getLibrary(loc.providerId)
                ?.search(loc.page, loc.keywords)
            ?: throw HttpException.NotFound("provider")
        call.respond(mangas)
    }

    get<ProviderLocation.Board> { loc ->
        val filters = call.request.queryParameters.toMap().mapValues { it.value.first().toInt() }
        val mangas = sourceManager.getSource(loc.providerId)
            ?.getBoard(loc.boardId, loc.page, filters)
            ?: libraryManager.getLibrary(loc.providerId)
                ?.getBoard(loc.boardId, loc.page)
            ?: throw HttpException.NotFound("provider")
        call.respond(mangas)
    }

    get<ProviderLocation.Manga> { loc ->
        val manga = libraryManager.getLibrary(loc.providerId)?.getManga(loc.mangaId)
        sourceManager.getSource(loc.providerId)?.let { source ->
            val mangaDetail = source.getManga(loc.mangaId)
            call.respond(mangaDetail.copy(inLibrary = manga != null))

            withContext(Dispatchers.IO) {
                manga?.takeIf { !it.hasMetadata() }?.updateMetadata(mangaDetail.metadataDetail)
                manga?.takeIf { !it.hasCover() }?.let {
                    mangaDetail.cover?.let { cover ->
                        it.updateCover(source.getImage(cover))
                    }
                }
            }
        } ?: call.respond(manga.ensure("manga").getDetail())
    }

    get<ProviderLocation.Cover> { loc ->
        call.response.headers.append(
            HttpHeaders.CacheControl,
            CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600).toString()
        )
        val image = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?.getCover()
            ?: sourceManager.getSource(loc.providerId)
                ?.getImage(loc.imageId)
            ?: throw HttpException.NotFound("provider")
        call.respondImage(image)
    }

    put<ProviderLocation.Cover> { loc ->
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && part.originalFileName == "cover") {
                libraryManager.getLibrary(loc.providerId)
                    ?.getManga(loc.mangaId).ensure("manga")
                    .updateCover(Image(part.contentType, part.streamProvider()))
            }
        }
        call.response.status(HttpStatusCode.NoContent)
    }

    put<ProviderLocation.Metadata> { loc ->
        val metadata = call.receive<MetadataDetailDto>()
        val manga = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?: throw HttpException.NotFound("manga")
        manga.updateMetadata(metadata)
        call.response.status(HttpStatusCode.NoContent)
    }

    get<ProviderLocation.Content> { loc ->
        val content = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?.getChapter(loc.collectionId, loc.chapterId)
            ?.getContent()
            ?: sourceManager.getSource(loc.providerId)
                ?.getContent(loc.mangaId, loc.collectionId, loc.chapterId)
            ?: throw HttpException.NotFound("provider")
        call.respond(content)
    }

    get<ProviderLocation.Image> { loc ->
        call.response.headers.append(
            HttpHeaders.CacheControl,
            CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600).toString()
        )
        val image = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?.getChapter(loc.collectionId, loc.chapterId)
            ?.getImage(loc.imageId)
            ?: sourceManager.getSource(loc.providerId)
                ?.getImage(loc.imageId)
            ?: throw HttpException.NotFound("provider")
        call.respondImage(image)
    }
}