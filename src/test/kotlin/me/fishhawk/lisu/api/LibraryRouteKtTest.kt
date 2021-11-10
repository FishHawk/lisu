package me.fishhawk.lisu.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.server.testing.*
import me.fishhawk.lisu.library.LibraryX
import me.fishhawk.lisu.source.SourceManager
import kotlin.io.path.Path

class LibraryRouteKtTest : DescribeSpec({
    describe("Router test: provider") {
        val library = LibraryX(Path("/home/wh/Projects/temp"))
        val manager = SourceManager()

        fun Application.libraryTestModule() {
            setup()
            routing { libraryRoutes(library, manager) }
        }

        it("#search") {
            withTestApplication(Application::libraryTestModule) {
                val uri = application.locations.href(LibraryLocation.Search(keywords = "", page = 0))
                with(handleRequest(HttpMethod.Get, uri)) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }
    }
})
