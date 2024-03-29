import api.*
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import download.Downloader
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.callloging.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.request.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.json.Json
import library.LibraryManager
import source.SourceManager

private class Lisu : CliktCommand() {
    init {
        versionOption("0.0.1")
    }

    val readonly by this
        .option(
            "--readonly",
            help = "enable readonly mode",
        )
        .flag()

    val updateImmediately by this
        .option(
            "-i",
            help = "update library once immediately"
        )
        .flag()

    val libraryPath by this
        .argument(help = "library path")
        .file(
            mustExist = true,
            canBeFile = false,
            canBeDir = true,
            mustBeWritable = false,
            mustBeReadable = true,
            canBeSymlink = true,
        )

    val port: Int by this
        .option(
            "-p", "--port",
            help = "backend port",
        )
        .int()
        .default(8080)

    override fun run() {
        // Create singleton.
        val libraryManager = LibraryManager(
            path = libraryPath.toPath(),
        )
        val sourceManager = SourceManager()
        val downloader = Downloader(
            updateImmediately = updateImmediately,
            libraryManager = libraryManager,
            sourceManager = sourceManager,
        )

        // Start server.
        embeddedServer(Netty, port) {
            install(Resources)
            install(WebSockets)
            install(ContentNegotiation) {
                json(Json)
            }
            install(CallLogging) {
                format { call ->
                    val status = call.response.status()
                    val httpMethod = call.request.httpMethod.value
                    val uri = call.request.uri
                    "$httpMethod-$status $uri"
                }
            }

            routing {
                readonlyIntercept(
                    readonly = readonly,
                )
                libraryRoute(
                    libraryManager = libraryManager,
                    sourceManager = sourceManager,
                    downloader = downloader,
                )
                providerRoute(
                    libraryManager = libraryManager,
                    sourceManager = sourceManager,
                )
                downloadRoute(
                    downloader = downloader,
                )
                systemRoute()
            }
        }.start(wait = true)
    }
}

fun main(argv: Array<String>) {
    Lisu().main(argv)
}