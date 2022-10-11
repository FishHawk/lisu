package me.fishhawk.lisu.api

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.util.pipeline.*
import me.fishhawk.lisu.library.LibraryException

internal suspend fun PipelineContext<Unit, ApplicationCall>.handleFailure(exception: Throwable) {
    val status = when (exception) {
        is LibraryException -> {
            when (exception) {
                is LibraryException.LibraryIllegalId,
                is LibraryException.MangaIllegalId,
                is LibraryException.ChapterIllegalId ->
                    HttpStatusCode.BadRequest

                is LibraryException.LibraryNotFound,
                is LibraryException.MangaNotFound,
                is LibraryException.ChapterNotFound ->
                    HttpStatusCode.NotFound
            }
        }

        else -> HttpStatusCode.InternalServerError
    }
    call.respondText(
        text = exception.message ?: "Unknown error.",
        status = status,
    )
}
