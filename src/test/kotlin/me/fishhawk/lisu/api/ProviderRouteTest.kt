package me.fishhawk.lisu.api

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.locations.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.testing.*
import kotlinx.serialization.json.Json
import me.fishhawk.lisu.library.Library
import me.fishhawk.lisu.provider.ProviderManager
import kotlin.io.path.Path

fun Application.setup() {
    install(Locations)
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
        })
    }
}

class ProviderRouteTest : DescribeSpec({
    describe("Router test: provider") {
        val library = Library(Path("/home/wh/Projects/temp"))
        val manager = ProviderManager()

        fun Application.providerTestModule() {
            setup()
            routing { providerRoutes(library, manager) }
        }

        it("#listProvider") {
            withTestApplication(Application::providerTestModule) {
                with(handleRequest(HttpMethod.Get, "/provider")) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }

        it("#search") {
            withTestApplication(Application::providerTestModule) {
                with(handleRequest(HttpMethod.Get, "/provider/漫画人/search?page=1&keywords=hello")) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }

        it("getBoard") {
            withTestApplication(Application::providerTestModule) {
                with(handleRequest(HttpMethod.Get, "/provider/漫画人/board/popular?page=1&type=0")) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }

        it("#getManga") {
            withTestApplication(Application::providerTestModule) {
                with(handleRequest(HttpMethod.Get, "/provider/漫画人/manga/18657")) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }

        it("#getContent") {
            withTestApplication(Application::providerTestModule) {
                with(handleRequest(HttpMethod.Get, "/provider/漫画人/chapter/18657/ /1012028")) {
                    response.status().shouldBe(HttpStatusCode.OK)
                }
            }
        }
    }
})
