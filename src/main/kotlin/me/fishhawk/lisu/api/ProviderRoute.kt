package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.locations.*
import io.ktor.locations.put
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.model.MetadataDetailDto
import me.fishhawk.lisu.model.ProviderDto
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
fun Route.providerRoutes(library: Library, manager: ProviderManager) {
    get("/provider") {
        call.respond(manager.providers.values.map {
            ProviderDto(it.id, it.lang, it.boardModels)
        })
    }

    get<ProviderLocation.Icon> { loc ->
        val provider = manager.providers[loc.providerId].ensureExist("provider")
        val iconURL = provider.icon.ensureExist("icon")
        val iconFile = File(iconURL.file)
        if (iconFile.exists()) call.respondFile(iconFile)
        else throw NotFoundException("")
    }

    get<ProviderLocation.Search> { loc ->
        val provider = manager.providers[loc.providerId].ensureExist("provider")
        call.respond(provider.search(loc.page, loc.keywords))
    }

    get<ProviderLocation.Board> { loc ->
        val provider = manager.providers[loc.providerId].ensureExist("provider")
        val filters = call.request.queryParameters.toMap().mapValues { it.value.first().toInt() }
        val mangaList = provider.getBoard(loc.boardId, loc.page, filters)
        call.respond(mangaList)
    }

    get<ProviderLocation.Manga> { loc ->
        val manga = library.getManga(loc.providerId, loc.mangaId)
        manager.providers[loc.providerId]?.let { provider ->
            val mangaDetail = provider.getManga(loc.mangaId)
            call.respond(mangaDetail.copy(inLibrary = manga != null))

            if (manga != null) {
                mangaDetail.cover?.let {
                    val response = provider.getImage(it)
                    val cover = response.receive<ByteArray>()
                    manga.updateCover(response.contentType(), cover)
                }
                manga.updateMetadata(mangaDetail.metadataDetail)
            }
        } ?: call.respond(manga.ensureExist("manga").getDetail())
    }

    get<ProviderLocation.Cover> { loc ->
        val coverInLibrary = library.getManga(loc.providerId, loc.mangaId)?.getCover()
        if (coverInLibrary != null) {
            call.respondFile(coverInLibrary)
        } else {
            val provider = manager.providers[loc.providerId].ensureExist("provider")
            val response = provider.getImage(loc.imageId)
            call.pipeResponse(response)
        }
    }

    put<ProviderLocation.Cover> { loc ->
        val manga = library.getManga(loc.providerId, loc.mangaId).ensureExist("manga")
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && part.originalFileName == "cover") {
                val fileBytes = part.streamProvider().readBytes()
                manga.updateCover(part.contentType, fileBytes)
            }
        }
        call.response.status(HttpStatusCode.NoContent)
    }

    put<ProviderLocation.Metadata> { loc ->
        val manga = library.getManga(loc.providerId, loc.mangaId).ensureExist("manga")
        val metadata = call.receive<MetadataDetailDto>()
        manga.updateMetadata(metadata)
        call.response.status(HttpStatusCode.NoContent)
    }

    get<ProviderLocation.Content> { loc ->
        val content = library.getManga(loc.providerId, loc.mangaId)
            ?.getChapter(loc.collectionId, loc.chapterId)
            ?.getContent()
            ?: manager.providers[loc.providerId]
                ?.getContent(loc.mangaId, loc.collectionId, loc.chapterId)
            ?: throw NotFoundException()
        call.respond(content)
    }

    get<ProviderLocation.Image> { loc ->
        val imageInLibrary = library.getManga(loc.providerId, loc.mangaId)
            ?.getChapter(loc.collectionId, loc.chapterId)
            ?.getImage(loc.imageId)
        if (imageInLibrary != null) {
            call.respondFile(imageInLibrary)
        } else {
            val provider = manager.providers[loc.providerId].ensureExist("provider")
            val response = provider.getImage(loc.imageId)
            call.pipeResponse(response)
        }
    }
}

private suspend fun ApplicationCall.pipeResponse(response: HttpResponse) {
    try {
        val readChannel = response.receive<ByteReadChannel>()
        respondOutputStream(response.contentType()) {
            readChannel.toInputStream().copyTo(this)
        }
    } catch (e: Throwable) {
        println(e)
    }
}
