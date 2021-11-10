package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.locations.put
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MetadataDetailDto
import me.fishhawk.lisu.model.ProviderDto
import me.fishhawk.lisu.model.respondImage
import me.fishhawk.lisu.provider.ProviderManager
import java.io.File

fun <T> T?.ensureExist(name: String) =
    this ?: throw NotFoundException("No $name found")

@OptIn(KtorExperimentalLocationsAPI::class)
private object ProviderLocation {
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
fun Route.providerRoutes(libraryManager: LibraryManager, providerManager: ProviderManager) {
    get("/provider") {
        call.respond(providerManager.providers.values.map {
            ProviderDto(it.id, it.lang, it.boardModels)
        })
    }

    get<ProviderLocation.Icon> { loc ->
        val provider = providerManager.providers[loc.providerId].ensureExist("provider")
        val iconURL = provider.icon.ensureExist("icon")
        val iconFile = File(iconURL.file)
        if (iconFile.exists()) call.respondFile(iconFile)
        else throw NotFoundException("")
    }

    get<ProviderLocation.Search> { loc ->
        val provider = providerManager.providers[loc.providerId].ensureExist("provider")
        call.respond(provider.search(loc.page, loc.keywords))
    }

    get<ProviderLocation.Board> { loc ->
        val provider = providerManager.providers[loc.providerId].ensureExist("provider")
        val filters = call.request.queryParameters.toMap().mapValues { it.value.first().toInt() }
        val mangaList = provider.getBoard(loc.boardId, loc.page, filters)
        call.respond(mangaList)
    }

    get<ProviderLocation.Manga> { loc ->
        val manga = libraryManager.getLibrary(loc.providerId)?.getManga(loc.mangaId)
        providerManager.providers[loc.providerId]?.let { provider ->
            val mangaDetail = provider.getManga(loc.mangaId)
            call.respond(mangaDetail.copy(inLibrary = manga != null))

            if (manga != null) {
                mangaDetail.cover?.let {
                    val image = provider.getImage(it)
                    manga.updateCover(image)
                }
                manga.updateMetadata(mangaDetail.metadataDetail)
            }
        } ?: call.respond(manga.ensureExist("manga").getDetail())
    }

    get<ProviderLocation.Cover> { loc ->
        call.response.headers.append(
            HttpHeaders.CacheControl,
            CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600).toString()
        )

        val image = libraryManager.getLibrary(loc.providerId)?.getManga(loc.mangaId)?.getCover()
            ?: providerManager.providers[loc.providerId].ensureExist("provider")
                .getImage(loc.imageId)
        call.respondImage(image)
    }

    put<ProviderLocation.Cover> { loc ->
        val manga = libraryManager.getLibrary(loc.providerId)?.getManga(loc.mangaId).ensureExist("manga")
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && part.originalFileName == "cover") {
                val cover = Image(part.contentType, part.streamProvider())
                manga.updateCover(cover)
            }
        }
        call.response.status(HttpStatusCode.NoContent)
    }

    put<ProviderLocation.Metadata> { loc ->
        val manga = libraryManager.getLibrary(loc.providerId)?.getManga(loc.mangaId).ensureExist("manga")
        val metadata = call.receive<MetadataDetailDto>()
        manga.updateMetadata(metadata)
        call.response.status(HttpStatusCode.NoContent)
    }

    get<ProviderLocation.Content> { loc ->
        val content = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?.getChapter(loc.collectionId, loc.chapterId)
            ?.getContent()
            ?: providerManager.providers[loc.providerId]
                ?.getContent(loc.mangaId, loc.collectionId, loc.chapterId)
            ?: throw NotFoundException()
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
            ?: providerManager.providers[loc.providerId].ensureExist("provider")
                .getImage(loc.imageId)
        call.respondImage(image)
    }
}