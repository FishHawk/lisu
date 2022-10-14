package me.fishhawk.lisu.api

import io.ktor.resources.*
import io.ktor.server.application.*
import io.ktor.server.resources.post
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.download.Downloader

@Serializable
@Resource("/download")
private class Download {
    @Serializable
    @Resource("/start-all")
    data class StartAll(
        val parent: Download = Download(),
    )

    @Serializable
    @Resource("/start-manga/{providerId}/{mangaId}")
    data class StartManga(
        val parent: Download = Download(),
        val providerId: String,
        val mangaId: String,
    )

    @Serializable
    @Resource("/start-chapter/{providerId}/{mangaId}/{collectionId}/{chapterId}")
    data class StartChapter(
        val parent: Download = Download(),
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
    )

    @Serializable
    @Resource("/cancel-all")
    data class CancelAll(
        val parent: Download = Download(),
    )

    @Serializable
    @Resource("/cancel-manga/{providerId}/{mangaId}")
    data class CancelManga(
        val parent: Download = Download(),
        val providerId: String,
        val mangaId: String,
    )

    @Serializable
    @Resource("/cancel-chapter/{providerId}/{mangaId}/{collectionId}/{chapterId}")
    data class CancelChapter(
        val parent: Download = Download(),
        val providerId: String,
        val mangaId: String,
        val collectionId: String,
        val chapterId: String,
    )
}

fun Route.downloadRoute(
    downloader: Downloader,
) {
    webSocket("/download/list") {
        val job = launch {
            downloader.observeTasks().collect {
                send(Frame.Text(Json.encodeToString(it)))
                flush()
            }
        }
        closeReason.await()
        job.cancel()
    }

    post<Download.StartAll> {
        downloader.startAllTasks()
        call.respond("Success")
    }

    post<Download.StartManga> { loc ->
        downloader.startMangaTask(loc.providerId, loc.mangaId)
        call.respond("Success")
    }

    post<Download.StartChapter> { loc ->
        downloader.startChapterTask(loc.providerId, loc.mangaId, loc.collectionId, loc.chapterId)
        call.respond("Success")
    }

    post<Download.CancelAll> {
        downloader.cancelAllTasks()
        call.respond("Success")
    }

    post<Download.CancelManga> { loc ->
        downloader.cancelMangaTask(loc.providerId, loc.mangaId)
        call.respond("Success")
    }

    post<Download.CancelChapter> { loc ->
        downloader.cancelChapterTask(loc.providerId, loc.mangaId, loc.collectionId, loc.chapterId)
        call.respond("Success")
    }
}