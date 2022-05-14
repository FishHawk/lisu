package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.locations.put
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import me.fishhawk.lisu.download.Downloader
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MangaMetadataDto

@OptIn(KtorExperimentalLocationsAPI::class)
private object LibraryLocation {
    @Location("/library/search")
    data class Search(val page: Int, val keywords: String)

    @Location("/library/random-manga")
    object RandomManga

    @Location("/library/manga/{providerId}/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/library/manga/{providerId}/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String, val imageId: String)

    @Location("/library/manga/{providerId}/{mangaId}/metadata")
    data class Metadata(val providerId: String, val mangaId: String)
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.libraryRoutes(
    libraryManager: LibraryManager,
    downloader: Downloader
) {
    get<LibraryLocation.Search> { loc ->
        val mangaList = libraryManager.search(loc.page, loc.keywords)
        call.respond(mangaList)
    }

    get<LibraryLocation.RandomManga> {
        val manga = libraryManager.getRandomManga().get()
        call.respond(manga)
    }

    post<LibraryLocation.Manga> { loc ->
        val library = libraryManager.getLibrary(loc.providerId)
            ?: libraryManager.createLibrary(loc.providerId).getOrThrow()
        library.createManga(loc.mangaId)
            .onFailure {
                call.respondText(status = HttpStatusCode.Conflict, text = "conflict")
            }
            .onSuccess {
                call.respondText(status = HttpStatusCode.OK, text = "success")
                downloader.add(loc.providerId, loc.mangaId)
            }
    }

    delete<LibraryLocation.Manga> { loc ->
        val library = libraryManager.getLibrary(loc.providerId).ensure("library")
        library.deleteManga(loc.mangaId)
            .onSuccess {
                downloader.remove(loc.providerId, loc.mangaId)
                call.respondText(status = HttpStatusCode.OK, text = "success")
            }
            .onFailure {
                call.respondText(status = HttpStatusCode.NotFound, text = "manga")
            }
    }

    put<LibraryLocation.Cover> { loc ->
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && part.originalFileName == "cover") {
                libraryManager.getLibrary(loc.providerId)
                    ?.getManga(loc.mangaId).ensure("manga")
                    .setCover(Image(part.contentType, part.streamProvider()))
            }
        }
        call.response.status(HttpStatusCode.NoContent)
    }

    put<LibraryLocation.Metadata> { loc ->
        val metadata = call.receive<MangaMetadataDto>()
        val manga = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?: throw HttpException.NotFound("manga")
        manga.setMetadata(metadata)
        call.response.status(HttpStatusCode.NoContent)
    }
}
