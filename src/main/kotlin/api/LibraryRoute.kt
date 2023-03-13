package api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.resources.put
import io.ktor.server.resources.delete
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable
import api.model.MangaDto
import api.model.MangaKeyDto
import api.model.MangaPageDto
import api.model.MangaState
import download.Downloader
import library.LibraryManager
import library.model.MangaMetadata
import source.SourceManager
import util.Image
import util.andThen

@Serializable
@Resource("/library")
private class Library {
    @Serializable
    @Resource("/search")
    data class Search(
        val parent: Library = Library(),
        val key: String,
        val keywords: String,
    )

    @Serializable
    @Resource("/manga-delete")
    data class MangaDelete(
        val parent: Library = Library(),
    )

    @Serializable
    @Resource("/random-manga")
    data class RandomManga(
        val parent: Library = Library(),
    )

    @Serializable
    @Resource("/manga/{providerId}/{mangaId}")
    data class Manga(
        val parent: Library = Library(),
        val providerId: String,
        val mangaId: String,
    )

    @Serializable
    @Resource("/manga/{providerId}/{mangaId}/cover")
    data class Cover(
        val parent: Library = Library(),
        val providerId: String,
        val mangaId: String,
    )

    @Serializable
    @Resource("/manga/{providerId}/{mangaId}/metadata")
    data class Metadata(
        val parent: Library = Library(),
        val providerId: String,
        val mangaId: String,
    )
}

fun Route.libraryRoute(
    libraryManager: LibraryManager,
    sourceManager: SourceManager,
    downloader: Downloader,
) {
    fun getMangaState(providerId: String) =
        if (sourceManager.hasSource(providerId)) MangaState.RemoteInLibrary
        else MangaState.Local

    get<Library.Search> { loc ->
        libraryManager
            .search(loc.key, loc.keywords)
            .map { (libraryId, manga) ->
                MangaDto(
                    state = getMangaState(libraryId),
                    providerId = libraryId,
                    manga = manga,
                )
            }
            .let {
                call.respond(
                    MangaPageDto(
                        list = it,
                        nextKey = it.lastOrNull()?.id,
                    )
                )
            }
    }

    post<Library.MangaDelete> {
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

    get<Library.RandomManga> {
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

    post<Library.Manga> { loc ->
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
            .onFailure { handleFailure(it) }
    }

    delete<Library.Manga> { loc ->
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
            .onFailure { handleFailure(it) }
    }

    put<Library.Cover> { loc ->
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
            .onFailure { handleFailure(it) }
    }

    put<Library.Metadata> { loc ->
        val metadata = call.receive<MangaMetadata>()
        libraryManager.getLibrary(loc.providerId)
            .andThen { it.getManga(loc.mangaId) }
            .andThen { mangaAccessor ->
                mangaAccessor.setMetadata(metadata)
                    .andThen { mangaAccessor.getMetadata() }
            }
            .onSuccess { call.respond(it) }
            .onFailure { handleFailure(it) }
    }
}