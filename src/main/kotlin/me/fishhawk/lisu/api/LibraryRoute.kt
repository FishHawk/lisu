package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.locations.post
import io.ktor.response.*
import io.ktor.routing.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.source.SourceManager

@OptIn(KtorExperimentalLocationsAPI::class)
private object LibraryLocation {
    @Location("/library/search")
    data class Search(val page: Int, val keywords: String)

    @Location("/library/random-manga")
    object RandomManga

    @Location("/library/manga/{providerId}/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.libraryRoutes(
    libraryManager: LibraryManager,
    sourceManager: SourceManager
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
        val source = sourceManager.getSource(loc.providerId).ensure("provider")
        val manga = libraryManager.createLibrary(loc.providerId)
            ?.createManga(loc.mangaId)
            ?: return@post call.respondText(status = HttpStatusCode.Conflict, text = "conflict")
        call.respondText(status = HttpStatusCode.OK, text = "success")

        val mangaDetail = source.getManga(loc.mangaId).ensure("manga")
        withContext(Dispatchers.IO) {
            manga.updateMetadata(mangaDetail.metadataDetail)
            mangaDetail.cover?.let {
                val cover = source.getImage(it)
                manga.updateCover(cover)
            }
        }
    }

    delete<LibraryLocation.Manga> { loc ->
        if (
            libraryManager.getLibrary(loc.providerId).ensure("library")
                .deleteManga(loc.mangaId)
        ) call.respondText(status = HttpStatusCode.OK, text = "success")
        else throw HttpException.NotFound("manga")
    }
}
