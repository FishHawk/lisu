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
import me.fishhawk.lisu.api.model.MangaDto
import me.fishhawk.lisu.api.model.MangaKeyDto
import me.fishhawk.lisu.api.model.MangaState
import me.fishhawk.lisu.download.Downloader
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.library.model.MangaMetadata
import me.fishhawk.lisu.source.SourceManager
import me.fishhawk.lisu.util.Image

@OptIn(KtorExperimentalLocationsAPI::class)
private object LibraryLocation {
    @Location("/library/search")
    data class Search(val page: Int, val keywords: String)

    @Location("/library/random-manga")
    object RandomManga

    @Location("/library/start")
    object StartAll

    @Location("/library/pause")
    object PauseAll

    @Location("/library/manga/{providerId}/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/library/manga/{providerId}/{mangaId}/pause")
    data class StartManga(val providerId: String, val mangaId: String)

    @Location("/library/manga/{providerId}/{mangaId}/pause")
    data class PauseManga(val providerId: String, val mangaId: String)

    @Location("/library/manga/{providerId}/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String)

    @Location("/library/manga/{providerId}/{mangaId}/metadata")
    data class Metadata(val providerId: String, val mangaId: String)

    @Location("/library/manga-delete")
    object MangaDelete
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.libraryRoutes(
    libraryManager: LibraryManager,
    sourceManager: SourceManager,
    downloader: Downloader
) {
    fun MangaDto.updateMangaState() =
        copy(state = if (sourceManager.hasSource(providerId)) MangaState.RemoteInLibrary else MangaState.Local)

    fun getMangaState(providerId: String) =
        if (sourceManager.hasSource(providerId)) MangaState.RemoteInLibrary
        else MangaState.Local

    get<LibraryLocation.Search> { loc ->
        val mangaList = libraryManager.search(loc.page, loc.keywords).map { (libraryId, manga) ->
            MangaDto(
                state = getMangaState(libraryId),
                providerId = libraryId,
                manga = manga,
            )
        }
        call.respond(mangaList)
    }

    get<LibraryLocation.RandomManga> {
        val manga = libraryManager.getRandomManga().let { (libraryId, manga) ->
            MangaDto(
                state = getMangaState(libraryId),
                providerId = libraryId,
                manga = manga,
            )
        }
        call.respond(manga)
    }

    post<LibraryLocation.StartAll> {
        downloader.startAll()
        call.respond("Success")
    }

    post<LibraryLocation.PauseAll> {
        downloader.pauseAll()
        call.respond("Success")
    }

    post<LibraryLocation.Manga> { loc ->
        val library = libraryManager.getLibrary(loc.providerId)
            ?: libraryManager.createLibrary(loc.providerId).getOrThrow()
        library.createManga(loc.mangaId)
            .onSuccess {
                call.respondText("Success")
                downloader.add(loc.providerId, loc.mangaId)
            }
            .onFailure {
                call.respondText(status = HttpStatusCode.Conflict, text = "conflict")
            }
    }

    post<LibraryLocation.StartManga> { loc ->
        downloader.start(loc.providerId, loc.mangaId)
        call.respond("Success")
    }

    post<LibraryLocation.PauseManga> { loc ->
        downloader.pause(loc.providerId, loc.mangaId)
        call.respond("Success")
    }

    delete<LibraryLocation.Manga> { loc ->
        val library = libraryManager.getLibrary(loc.providerId).ensure("library")
        library.deleteManga(loc.mangaId)
            .onSuccess {
                downloader.remove(loc.providerId, loc.mangaId)
                call.respondText("Success")
            }
            .onFailure {
                call.respondText(status = HttpStatusCode.NotFound, text = "manga")
            }
    }

    put<LibraryLocation.Cover> { loc ->
        call.receiveMultipart().forEachPart { part ->
            if (part is PartData.FileItem && part.name == "cover") {
                libraryManager.getLibrary(loc.providerId)
                    ?.getManga(loc.mangaId).ensure("manga")
                    .setCover(Image(part.contentType, part.streamProvider()))
            }
        }
        call.response.status(HttpStatusCode.NoContent)
    }

    put<LibraryLocation.Metadata> { loc ->
        val metadata = call.receive<MangaMetadata>()
        val manga = libraryManager.getLibrary(loc.providerId)
            ?.getManga(loc.mangaId)
            ?: throw HttpException.NotFound("manga")
        manga.setMetadata(metadata)
        call.response.status(HttpStatusCode.NoContent)
    }

    post<LibraryLocation.MangaDelete> {
        val mangaKeys = call.receive<List<MangaKeyDto>>()
        val isFail = mangaKeys.map {
            libraryManager
                .getLibrary(it.providerId)
                ?.deleteManga(it.id)
        }.any { it == null || it.isFailure }
        if (isFail) call.response.status(HttpStatusCode.OK)
        else call.response.status(HttpStatusCode.InternalServerError)
    }
}
