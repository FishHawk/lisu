package me.fishhawk.lisu.source.manhuaren

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.ktor.http.*
import source.Source
import source.manhuaren.Api

class ApiTest : DescribeSpec({
    describe("Source test: manhuaren api") {
        val api = Api(Source.client)

        it("#getSearchManga") {
            api.getSearchManga(0, "龙珠超").shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getRank") {
            api.getRank(0, 0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getUpdate") {
            api.getUpdate(0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getRelease") {
            api.getRelease(0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getCategoryMangas") {
            api.getCategoryMangas(0, 0, 0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getDetail") {
            api.getDetail("18657").shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getRead") {
            api.getRead("1012028").shouldHaveStatus(HttpStatusCode.OK)
        }
    }
})
