package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cachingheaders.*
import io.ktor.server.request.*
import io.ktor.server.resources.get
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*
import kotlinx.serialization.Serializable
import me.fishhawk.lisu.api.model.*
import me.fishhawk.lisu.library.*
import me.fishhawk.lisu.library.model.Manga
import me.fishhawk.lisu.library.model.MangaDetail
import me.fishhawk.lisu.library.model.MangaPage
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.Source
import me.fishhawk.lisu.source.SourceManager
import me.fishhawk.lisu.util.Image
import me.fishhawk.lisu.util.andThen
import me.fishhawk.lisu.util.respondImage

@Serializable
@Resource("/provider")
private class Provider {
    @Serializable
    @Resource("/{providerId}/icon")
    data class Icon(
        val parent: Provider = Provider(),
        val providerId: String,
    )

    @Serializable
    @Resource("/{providerId}/login-cookies")
    data class LoginByCookies(
        val parent: Provider = Provider(),
        val providerId: String,
    )

    @Serializable
    @Resource("/{providerId}/login-password")
    data class LoginByPassword(
        val parent: Provider = Provider(),
        val providerId: String,
        val username: String,
        val password: String,
    )

    @Serializable
    @Resource("/{providerId}/logout")
    data class Logout(
        val parent: Provider = Provider(),
        val providerId: String,
    )

    @Serializable
    @Resource("/{providerId}/board/{boardId}")
    data class Board(
        val parent: Provider = Provider(),
        val providerId: String,
        val boardId: BoardId,
        val key: String,
        val keywords: String?,
    )

    @Serializable
    @Resource("/{providerId}/manga/{mangaId}")
    data class Manga(
        val parent: Provider = Provider(),
        val providerId: String,
        val mangaId: String,
    )

    @Serializable
    @Resource("/{providerId}/manga/{mangaId}/comment")
    data class Comment(
        val parent: Provider = Provider(),
        val providerId: String,
        val mangaId: String,
        val page: Int,
    )

    @Serializable
    @Resource("/{providerId}/manga/{mangaId}/cover")
    data class Cover(
        val parent: Provider = Provider(),
        val providerId: String,
        val mangaId: String,
        val imageId: String? = null,
    )

    @Serializable
    @Resource("/{providerId}/manga/{mangaId}/content")
    data class Content(
        val parent: Provider = Provider(),
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
    )

    @Serializable
    @Resource("/{providerId}/manga/{mangaId}/image")
    data class Image(
        val parent: Provider = Provider(),
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
        val imageId: String,
    )
}

fun Route.providerRoute(
    libraryManager: LibraryManager,
    sourceManager: SourceManager,
) {
    suspend fun PipelineContext<Unit, ApplicationCall>.getProvider(providerId: String): ProviderAdapter? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId).getOrNull()
        val provider =
            if (source != null) {
                ProviderAdapter.Remote(source, library)
            } else if (library != null) {
                ProviderAdapter.Local(library)
            } else {
                call.respondText(
                    text = "Provider not found.",
                    status = HttpStatusCode.NotFound,
                )
                null
            }
        return provider
    }

    suspend fun PipelineContext<Unit, ApplicationCall>.getRemoteProvider(providerId: String): ProviderAdapter.Remote? {
        val source = sourceManager.getSource(providerId)
        val library = libraryManager.getLibrary(providerId).getOrNull()
        val provider =
            if (source != null) {
                ProviderAdapter.Remote(source, library)
            } else {
                call.respondText(
                    text = "Provider not found.",
                    status = HttpStatusCode.NotFound,
                )
                null
            }
        return provider
    }

    get<Provider> {
        val remoteProviders = sourceManager
            .listSources()
            .map { ProviderDto.fromSource(it) }
        val localProviders = libraryManager
            .listLibraries()
            .filter { library -> remoteProviders.none { it.id == library.id } }
            .map { ProviderDto.fromLibrary(it) }
        call.respond(remoteProviders + localProviders)
    }

    get<Provider.Icon> { loc ->
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

    post<Provider.LoginByCookies> { loc ->
        getRemoteProvider(loc.providerId)?.apply {
            val cookies = call.receive<Map<String, String>>()
            val isSuccess = source.loginFeature?.cookiesLogin?.login(cookies) ?: false
            if (isSuccess) call.respondText("Success")
            else call.respondText(status = HttpStatusCode.InternalServerError, text = "")
        }
    }

    post<Provider.LoginByPassword> { loc ->
        getRemoteProvider(loc.providerId)?.apply {
            val isSuccess = source.loginFeature?.passwordLogin?.login(loc.username, loc.password) ?: false
            if (isSuccess) call.respondText("Success")
            else call.respondText(status = HttpStatusCode.InternalServerError, text = "")
        }
    }

    post<Provider.Logout> { loc ->
        getRemoteProvider(loc.providerId)?.apply {
            source.loginFeature?.logout()
            call.respondText("Success")
        }
    }

    get<Provider.Comment> { loc ->
        getRemoteProvider(loc.providerId)?.apply {
            source.commentFeature?.getComment(loc.mangaId, loc.page)
                ?.onSuccess { call.respond(it) }
                ?.onFailure { handleFailure(it) }
                ?: call.respondText(
                    text = "Comment not found.",
                    status = HttpStatusCode.NotFound,
                )
        }
    }

    get<Provider.Board> { loc ->
        getProvider(loc.providerId)
            ?.onLocal {
                library
                    .search(loc.key, "")
                    .toDto()
                    .let { call.respond(it) }
            }
            ?.onRemote {
                source
                    .getBoard(loc.boardId, loc.key, call.request.queryParameters)
                    .map { it.toDto() }
                    .onSuccess { call.respond(it) }
                    .onFailure { handleFailure(it) }
            }
    }

    get<Provider.Manga> { loc ->
        getProvider(loc.providerId)?.onLocal {
            library
                .getManga(loc.mangaId)
                .map { it.getDetail().toDto() }
                .onSuccess { call.respond(it) }
                .onFailure { handleFailure(it) }
        }?.onRemote {
            source
                .getManga(loc.mangaId)
                .map { it.toDto() }
                .onSuccess { call.respond(it) }
                .onFailure { handleFailure(it) }
        }
    }

    get<Provider.Cover> { loc ->
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
                .onFailure { handleFailure(it) }
        }?.onRemote {
            // Using Cache
            library
                ?.getManga(loc.mangaId)
                ?.map { it.getCover() }
                ?.getOrNull()
                ?.let { return@onRemote call.respondImage(it) }
            if (loc.imageId == null) {
                return@onRemote call.respondText(
                    text = "Cover not found.",
                    status = HttpStatusCode.NotFound,
                )
            }
            source.getImage(loc.imageId)
                .onSuccess { call.respondImage(it) }
                .onFailure { handleFailure(it) }
        }
    }

    get<Provider.Content> { loc ->
        getProvider(loc.providerId)?.onLocal {
            library
                .getManga(loc.mangaId)
                .andThen { it.getChapter(loc.collectionId, loc.chapterId) }
                .map { it.getContent() }
                .onSuccess {
                    if (it == null) call.respondText(
                        text = "Content not found.",
                        status = HttpStatusCode.NotFound,
                    )
                    else call.respond(it)
                }
                .onFailure { handleFailure(it) }
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
                .onFailure { handleFailure(it) }
        }
    }

    get<Provider.Image> { loc ->
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
                .onFailure { handleFailure(it) }
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
                .onFailure { handleFailure(it) }
        }
    }
}

private sealed interface ProviderAdapter {
    data class Local(val library: Library) : ProviderAdapter {
        fun MangaPage.toDto() =
            MangaPageDto(
                list = list.map { it.toDto() },
                nextKey = nextKey,
            )

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

    data class Remote(val source: Source, val library: Library?) : ProviderAdapter {
        fun getMangaState(id: String) =
            if (library?.getManga(id)?.getOrNull() == null) MangaState.Remote
            else MangaState.RemoteInLibrary

        fun MangaPage.toDto() =
            MangaPageDto(
                list = list.map { it.toDto() },
                nextKey = nextKey,
            )

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

private inline fun ProviderAdapter.onLocal(action: ProviderAdapter.Local.() -> Unit): ProviderAdapter {
    if (this is ProviderAdapter.Local) action()
    return this
}

private inline fun ProviderAdapter.onRemote(action: ProviderAdapter.Remote.() -> Unit): ProviderAdapter {
    if (this is ProviderAdapter.Remote) action()
    return this
}
