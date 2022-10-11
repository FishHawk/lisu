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
import me.fishhawk.lisu.util.andThen

@OptIn(KtorExperimentalLocationsAPI::class)
private object LibraryLocation {
    @Location("/search")
    data class Search(val page: Int, val keywords: String)

    @Location("/random-manga")
    object RandomManga

    @Location("/manga/{providerId}/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/manga/{providerId}/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String)

    @Location("/manga/{providerId}/{mangaId}/metadata")
    data class Metadata(val providerId: String, val mangaId: String)

    @Location("/manga-delete")
    object MangaDelete
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.libraryRoutes(
    libraryManager: LibraryManager,
    sourceManager: SourceManager,
    downloader: Downloader
) {
    route("/library") {
        fun getMangaState(providerId: String) =
            if (sourceManager.hasSource(providerId)) MangaState.RemoteInLibrary
            else MangaState.Local

        get<LibraryLocation.Search> { loc ->
            libraryManager
                .search(loc.page, loc.keywords)
                .map { (libraryId, manga) ->
                    MangaDto(
                        state = getMangaState(libraryId),
                        providerId = libraryId,
                        manga = manga,
                    )
                }
                .let { call.respond(it) }
        }

        get<LibraryLocation.RandomManga> {
            libraryManager
                .getRandomManga()
                ?.let { (libraryId, manga) ->
                    MangaDto(
                        state = getMangaState(libraryId),
                        providerId = libraryId,
                        manga = manga,
                    )
                }
                ?.let { call.respond(it) }
                ?: call.respondText(text = "No manga found.", status = HttpStatusCode.NotFound)
        }

        post<LibraryLocation.Manga> { loc ->
            libraryManager
                .createLibrary(loc.providerId)
                .andThen { it.createManga(loc.mangaId) }
                .onSuccess {
                    call.respondText(
                        status = HttpStatusCode.NoContent,
                        text = "Success.",
                    )
                    downloader.addMangaTask(loc.providerId, loc.mangaId)
                }
                .onFailure { processFailure(it) }
        }

        delete<LibraryLocation.Manga> { loc ->
            libraryManager
                .getLibrary(loc.providerId)
                .andThen { it.deleteManga(loc.mangaId) }
                .onSuccess {
                    call.respondText(
                        status = HttpStatusCode.NoContent,
                        text = "Success.",
                    )
                    downloader.cancelMangaTask(loc.providerId, loc.mangaId)
                }
                .onFailure { processFailure(it) }
        }

        put<LibraryLocation.Cover> { loc ->
            val image = call
                .receiveMultipart()
                .readAllParts()
                .filterIsInstance<PartData.FileItem>()
                .firstOrNull { it.name == "cover" }
                ?.let { Image(it.contentType, it.streamProvider()) }
                ?: return@put call.respondText(
                    text = "No image file.",
                    status = HttpStatusCode.BadRequest,
                )

            libraryManager.getLibrary(loc.providerId)
                .andThen { it.getManga(loc.mangaId) }
                .andThen { it.setCover(image) }
                .onSuccess {
                    call.respondText(
                        status = HttpStatusCode.NoContent,
                        text = "Success.",
                    )
                }
                .onFailure { processFailure(it) }
        }

        put<LibraryLocation.Metadata> { loc ->
            val metadata = call.receive<MangaMetadata>()
            libraryManager.getLibrary(loc.providerId)
                .andThen { it.getManga(loc.mangaId) }
                .andThen { mangaAccessor ->
                    mangaAccessor.setMetadata(metadata)
                        .andThen { mangaAccessor.getMetadata() }
                }
                .onSuccess { call.respond(it) }
                .onFailure { processFailure(it) }
        }

        post<LibraryLocation.MangaDelete> {
            val mangaKeys = call.receive<List<MangaKeyDto>>()
            val failedKeys = mangaKeys.filter { key ->
                libraryManager
                    .getLibrary(key.providerId)
                    .andThen { it.deleteManga(key.id) }
                    .isFailure
            }
            if (failedKeys.isEmpty()) call.respondText(status = HttpStatusCode.NoContent, text = "Success.")
            else call.respondText(
                status = HttpStatusCode.InternalServerError,
                text = "${failedKeys.size} manga fail to delete.",
            )
        }
    }
}
