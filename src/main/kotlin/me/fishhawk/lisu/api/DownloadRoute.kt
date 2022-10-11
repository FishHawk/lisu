package me.fishhawk.lisu.api

import io.ktor.server.application.*
import io.ktor.server.locations.*
import io.ktor.server.locations.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.download.Downloader

@OptIn(KtorExperimentalLocationsAPI::class)
private object DownloadLocation {
    @Location("/start-all")
    object StartAll

    @Location("/start-manga/{providerId}/{mangaId}")
    data class StartManga(
        val providerId: String,
        val mangaId: String,
    )

    @Location("/start-chapter/{providerId}/{mangaId}/{collectionId}/{chapterId}")
    data class StartChapter(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
    )

    @Location("/cancel-all")
    object CancelAll

    @Location("/cancel-manga/{providerId}/{mangaId}")
    data class CancelManga(
        val providerId: String,
        val mangaId: String,
    )

    @Location("/cancel-chapter/{providerId}/{mangaId}/{collectionId}/{chapterId}")
    data class CancelChapter(
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
    )
}

@OptIn(KtorExperimentalLocationsAPI::class)
fun Route.downloadRoute(
    downloader: Downloader,
) {
    route("/download") {
        webSocket("/list") {
            val job = launch {
                downloader.observeTasks().collect {
                    send(Frame.Text(Json.encodeToString(it)))
                    flush()
                }
            }
            closeReason.await()
            job.cancel()
        }

        post<DownloadLocation.StartAll> {
            downloader.startAllTasks()
            call.respond("Success")
        }

        post<DownloadLocation.StartManga> { loc ->
            downloader.startMangaTask(loc.providerId, loc.mangaId)
            call.respond("Success")
        }

        post<DownloadLocation.StartChapter> { loc ->
            downloader.startChapterTask(loc.providerId, loc.mangaId, loc.collectionId, loc.chapterId)
            call.respond("Success")
        }

        post<DownloadLocation.CancelAll> {
            downloader.cancelAllTasks()
            call.respond("Success")
        }

        post<DownloadLocation.CancelManga> { loc ->
            downloader.cancelMangaTask(loc.providerId, loc.mangaId)
            call.respond("Success")
        }

        post<DownloadLocation.CancelChapter> { loc ->
            downloader.cancelChapterTask(loc.providerId, loc.mangaId, loc.collectionId, loc.chapterId)
            call.respond("Success")
        }
    }
}