package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import me.fishhawk.lisu.api.model.*
import me.fishhawk.lisu.library.*
import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.library.model.MangaDetail
import me.fishhawk.lisu.source.model.BoardId
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager
import me.fishhawk.lisu.util.Image
import me.fishhawk.lisu.util.andThen
import me.fishhawk.lisu.util.respondImage

@OptIn(KtorExperimentalLocationsAPI::class)
private object ProviderLocation {
    @Location("/{providerId}/icon")
    data class Icon(val providerId: String)

    @Location("/{providerId}/login-cookies")
    data class LoginByCookies(val providerId: String)

    @Location("/{providerId}/login-password")
    data class LoginByPassword(val providerId: String, val username: String, val password: String)

    @Location("/{providerId}/logout")
    data class Logout(val providerId: String)

    @Location("/{providerId}/manga/{mangaId}/comment")
    data class Comment(val providerId: String, val mangaId: String, val page: Int)

    @Location("/{providerId}/board/{boardId}")
    data class Board(val providerId: String, val boardId: BoardId, val page: Int)

    @Location("/{providerId}/manga/{mangaId}")
    data class Manga(val providerId: String, val mangaId: String)

    @Location("/{providerId}/manga/{mangaId}/cover")
    data class Cover(val providerId: String, val mangaId: String, val imageId: String)

    @Location("/{providerId}/manga/{mangaId}/content/{collectionId}/{chapterId}")
    data class Content(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String
    )

    @Location("/{providerId}/manga/{mangaId}/image/{collectionId}/{chapterId}/{imageId}")
    data class Image(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
        val imageId: String
    )
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.providerRoutes(
    libraryManager: LibraryManager,
    sourceManager: SourceManager,
) {
    suspend fun PipelineContext<Unit, ApplicationCall>.getProvider(providerId: String): Provider? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId).getOrNull()
        val provider =
            if (source != null) {
                Provider.Remote(source, library)
            } else if (library != null) {
                Provider.Local(library)
            } else {
                call.respondText(
                    text = "Provider not found.",
                    status = HttpStatusCode.NotFound,
                )
                null
            }
        return provider
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.getRemoteProvider(providerId: String): Provider.Remote? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId).getOrNull()
        val provider =
            if (source != null) {
                Provider.Remote(source, library)
            } else {
                call.respondText(
                    text = "Provider not found.",
                    status = HttpStatusCode.NotFound,
                )
                null
            }
        return provider
    }

    route("/provider") {
        install(CachingHeaders) {
            options { _, outgoingContent ->
                when (outgoingContent.contentType?.withoutParameters()?.contentType) {
                    ContentType.Image.Any.contentType -> {
                        CachingOptions(CacheControl.MaxAge(maxAgeSeconds = 10 * 24 * 3600))
                    }
                    else -> null
                }
            }
        }

        get {
            val remoteProviders = sourceManager
                .listSources()
                .map { ProviderDto.fromSource(it) }
            val localProviders = libraryManager
                .listLibraries()
                .filter { library -> remoteProviders.none { it.id == library.id } }
                .map { ProviderDto.fromLibrary(it) }
            call.respond(remoteProviders + localProviders)
        }

        get<ProviderLocation.Icon> { loc ->
            getRemoteProvider(loc.providerId)?.apply {
                val image = source::class.java.getResourceAsStream("icon.png")
                    ?.let { Image(ContentType.Image.PNG, it) }
                if (image == null) {
                    call.respondText(
                        text = "Cover not found.",
                        status = HttpStatusCode.NotFound,
                    )
                } else {
                    call.respondImage(image)
                }
            }
        }

        post<ProviderLocation.LoginByCookies> { loc ->
            getRemoteProvider(loc.providerId)?.apply {
                val cookies = call.receive<Map<String, String>>()
                val isSuccess = source.loginFeature?.cookiesLogin?.login(cookies) ?: false
                if (isSuccess) call.respondText("Success")
                else call.respondText(status = HttpStatusCode.InternalServerError, text = "")
            }
        }

        post<ProviderLocation.LoginByPassword> { loc ->
            getRemoteProvider(loc.providerId)?.apply {
                val isSuccess = source.loginFeature?.passwordLogin?.login(loc.username, loc.password) ?: false
                if (isSuccess) call.respondText("Success")
                else call.respondText(status = HttpStatusCode.InternalServerError, text = "")
            }
        }

        post<ProviderLocation.Logout> { loc ->
            getRemoteProvider(loc.providerId)?.apply {
                source.loginFeature?.logout()
                call.respondText("Success")
            }
        }

        get<ProviderLocation.Comment> { loc ->
            getRemoteProvider(loc.providerId)?.apply {
                source.commentFeature?.getComment(loc.mangaId, loc.page)
                    ?.onSuccess { call.respond(it) }
                    ?.onFailure { }
                    ?: call.respondText(
                        text = "Cover not found.",
                        status = HttpStatusCode.NotFound,
                    )
            }
        }

        get<ProviderLocation.Board> { loc ->
            getProvider(loc.providerId)
                ?.onLocal {
                    library
                        .search(loc.page, "")
                        .map { it.toDto() }
                        .let { call.respond(it) }
                }
                ?.onRemote {
                    source
                        .getBoard(loc.boardId, loc.page, call.request.queryParameters)
                        .map { list -> list.map { it.toDto() } }
                        .onSuccess { call.respond(it) }
                        .onFailure { onRemoteProviderFailure(it) }
                }
        }

        get<ProviderLocation.Manga> { loc ->
            getProvider(loc.providerId)?.onLocal {
                library
                    .getManga(loc.mangaId)
                    .map { it.getDetail().toDto() }
                    .onSuccess { call.respond(it) }
                    .onFailure { onLocalProviderFailure(it) }
            }?.onRemote {
                source
                    .getManga(loc.mangaId)
                    .map { it.toDto() }
                    .onSuccess { call.respond(it) }
                    .onFailure { onRemoteProviderFailure(it) }
            }
        }

        get<ProviderLocation.Cover> { loc ->
            getProvider(loc.providerId)?.onLocal {
                library
                    .getManga(loc.mangaId)
                    .map { it.getCover() }
                    .onSuccess {
                        if (it == null) call.respondText(
                            text = "Cover not found.",
                            status = HttpStatusCode.NotFound,
                        )
                        else call.respondImage(it)
                    }
                    .onFailure { onLocalProviderFailure(it) }
            }?.onRemote {
                // Using Cache
                library
                    ?.getManga(loc.mangaId)
                    ?.map { it.getCover() }
                    ?.getOrNull()
                    ?.let { return@onRemote call.respondImage(it) }
                source.getImage(loc.imageId)
                    .onSuccess { call.respondImage(it) }
                    .onFailure { onRemoteProviderFailure(it) }
            }
        }

        get<ProviderLocation.Content> { loc ->
            getProvider(loc.providerId)?.onLocal {
                library
                    .getManga(loc.mangaId)
                    .andThen { it.getChapter(loc.collectionId, loc.chapterId) }
                    .map { it.getContent() }
                    .onSuccess {
                        if (it == null) call.respondText(
                            text = "Cover not found.",
                            status = HttpStatusCode.NotFound,
                        )
                        else call.respond(it)
                    }
                    .onFailure { onLocalProviderFailure(it) }
            }?.onRemote {
                // Using Cache
                library
                    ?.getManga(loc.mangaId)
                    ?.andThen { it.getChapter(loc.collectionId, loc.chapterId) }
                    ?.map { it.takeIf { it.isFinished() }?.getContent() }
                    ?.getOrNull()
                    ?.let { return@onRemote call.respond(it) }
                source.getContent(loc.mangaId, loc.chapterId)
                    .onSuccess { call.respond(it) }
                    .onFailure { onRemoteProviderFailure(it) }
            }
        }

        get<ProviderLocation.Image> { loc ->
            getProvider(loc.providerId)?.onLocal {
                library
                    .getManga(loc.mangaId)
                    .andThen { it.getChapter(loc.collectionId, loc.chapterId) }
                    .map { it.getImage(loc.imageId) }
                    .onSuccess {
                        if (it == null) call.respondText(
                            text = "Image not found.",
                            status = HttpStatusCode.NotFound,
                        )
                        else call.respondImage(it)
                    }
                    .onFailure { onLocalProviderFailure(it) }
            }?.onRemote {
                // Using Cache
                library
                    ?.getManga(loc.mangaId)
                    ?.andThen { it.getChapter(loc.collectionId, loc.chapterId) }
                    ?.map { it.getImage(loc.imageId) }
                    ?.getOrNull()
                    ?.let { return@onRemote call.respondImage(it) }
                source.getImage(loc.imageId)
                    .onSuccess { call.respondImage(it) }
                    .onFailure { onRemoteProviderFailure(it) }
            }
        }
    }
}

internal suspend fun PipelineContext<Unit, ApplicationCall>.onLocalProviderFailure(exception: Throwable) {
    if (exception !is LibraryException) {
        call.respondText(text = "Unknown error.", status = HttpStatusCode.InternalServerError)
    } else {
        val status = when (exception) {
            is LibraryException.LibraryIllegalId,
            is LibraryException.MangaIllegalId,
            is LibraryException.ChapterIllegalId ->
                HttpStatusCode.BadRequest

            is LibraryException.LibraryNotFound,
            is LibraryException.MangaNotFound,
            is LibraryException.ChapterNotFound ->
                HttpStatusCode.NotFound

            is LibraryException.LibraryCanNotCreate,
            is LibraryException.MangaCanNotCreate,
            is LibraryException.ChapterCanNotCreate ->
                HttpStatusCode.InternalServerError
        }
        call.respondText(text = exception.message, status = status)
    }
}

private suspend fun PipelineContext<Unit, ApplicationCall>.onRemoteProviderFailure(exception: Throwable) {
    call.respondText(text = "Unknown error.", status = HttpStatusCode.InternalServerError)
}

private sealed interface Provider {
    data class Local(val library: Library) : Provider {
        fun Manga.toDto() =
            MangaDto(
                state = MangaState.Local,
                providerId = library.id,
                manga = this,
            )

        fun MangaDetail.toDto() =
            MangaDetailDto(
                state = MangaState.Local,
                providerId = library.id,
                manga = this,
            )
    }

    data class Remote(val source: Source, val library: Library?) : Provider {
        fun getMangaState(id: String) =
            if (library?.getManga(id)?.getOrNull() == null) MangaState.Remote
            else MangaState.RemoteInLibrary

        fun Manga.toDto() =
            MangaDto(
                state = getMangaState(id),
                providerId = source.id,
                manga = this,
            )

        fun MangaDetail.toDto() =
            MangaDetailDto(
                state = getMangaState(id),
                providerId = source.id,
                manga = this,
            )
    }
}

private inline fun Provider.onLocal(action: Provider.Local.() -> Unit): Provider {
    if (this is Provider.Local) action()
    return this
}

private inline fun Provider.onRemote(action: Provider.Remote.() -> Unit): Provider {
    if (this is Provider.Remote) action()
    return this
}
