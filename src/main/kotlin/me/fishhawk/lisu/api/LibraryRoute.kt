package me.fishhawk.lisu.api

import io.ktor.application.*
import io.ktor.client.call.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.locations.post
import io.ktor.response.*
import io.ktor.routing.*
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.provider.ProviderManager

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
    library: Library,
    manager: ProviderManager
) {
    get<LibraryLocation.Search> { loc ->
        val mangaList = library.search(loc.page, loc.keywords)
        call.respond(mangaList)
    }

    get<LibraryLocation.RandomManga> {
        val manga = library.getRandomManga().get()
        call.respond(manga)
    }

    post<LibraryLocation.Manga> { loc ->
        val provider = manager.providers[loc.providerId].ensureExist("provider")
        val manga = library.createManga(loc.providerId, loc.mangaId)
            ?: return@post call.respondText(status = HttpStatusCode.Conflict, text = "conflict")
        call.respondText(status = HttpStatusCode.OK, text = "success")

        val mangaDetail = provider.getManga(loc.mangaId).ensureExist("manga")
        mangaDetail.cover?.let {
            val response = provider.getImage(it)
            val cover = response.receive<ByteArray>()
            manga.updateCover(response.contentType(), cover)
        }
        manga.updateMetadata(mangaDetail.metadataDetail)
    }

    delete<LibraryLocation.Manga> { loc ->
        if (library.deleteManga(loc.providerId, loc.mangaId))
            call.respondText(status = HttpStatusCode.OK, text = "success")
        else throw NotFoundException("")
    }
}
