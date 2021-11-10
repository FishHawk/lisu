package me.fishhawk.lisu

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.cli.optional
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.api.libraryRoutes
import me.fishhawk.lisu.api.providerRoutes
import me.fishhawk.lisu.api.systemRoutes
import me.fishhawk.lisu.library.LibraryManager
import me.fishhawk.lisu.provider.ProviderManager
import kotlin.io.path.Path

fun main(args: Array<String>) {
    val parser = ArgParser("lisu")
    val libraryPath by parser.argument(ArgType.String, description = "Library path").optional().default("./")
    val port by parser.option(ArgType.Int, shortName = "p", description = "Backend port").default(8080)
    parser.parse(args)

    val libraryManager = LibraryManager(Path(libraryPath))
    val providerManager = ProviderManager()

    embeddedServer(Netty, port) { lisuModule(libraryManager, providerManager) }.start(wait = true)
}

private fun Application.lisuModule(
    libraryManager: LibraryManager,
    providerManager: ProviderManager
) {
    install(Locations)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
    install(StatusPages) {
        exception<NotFoundException> { cause ->
            call.respondText(cause.localizedMessage, status = HttpStatusCode.NotFound)
        }
        exception<Throwable> { cause ->
            call.respondText(cause.localizedMessage, status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        intercept(ApplicationCallPipeline.Call) {
            application.log.info("${call.request.httpMethod.value} ${call.request.uri}")
        }
        libraryRoutes(libraryManager, providerManager)
        providerRoutes(libraryManager, providerManager)
        systemRoutes()
    }
}
