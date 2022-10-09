package me.fishhawk.lisu

import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.locations.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.api.*
import me.fishhawk.lisu.download.Downloader
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.source.SourceManager
import kotlin.io.path.Path

fun main(args: Array<String>) {
    // Parse arguments.
    val parser = ArgParser("lisu")

    val libraryPath by parser.argument(
        type = ArgType.String,
        description = "Library path"
    ).optional().default("./")

    val port by parser.option(
        type = ArgType.Int,
        fullName = "port",
        shortName = "p",
        description = "Backend port"
    ).default(8080)

    parser.parse(args)

    // Create singleton.
    val libraryManager = LibraryManager(
        path = Path(libraryPath),
    )
    val sourceManager = SourceManager()
    val downloader = Downloader(
        libraryManager = libraryManager,
        sourceManager = sourceManager,
    )

    // Start server.
    embeddedServer(Netty, port) {
        install(Locations)

        install(ContentNegotiation) {
            json(Json {
                isLenient = true
            })
        }

        install(WebSockets)

        install(CallLogging) {
            format { call ->
                val status = call.response.status()
                val httpMethod = call.request.httpMethod.value
                val uri = call.request.uri
                "$httpMethod-$status $uri"
            }
        }

        routing {
            libraryRoutes(
                libraryManager = libraryManager,
                sourceManager = sourceManager,
                downloader = downloader,
            )
            providerRoutes(
                libraryManager = libraryManager,
                sourceManager = sourceManager,
            )
            downloadRoutes(downloader)
            systemRoutes()
        }
    }.start(wait = true)
}