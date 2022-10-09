package me.fishhawk.lisu.source.bilibili

import io.kotest.assertions.ktor.client.shouldHaveStatus
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.ktor.client.call.*
import io.ktor.client.statement.*
import io.ktor.http.*
import me.fishhawk.lisu.source.Source
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.time.ExperimentalTime

class ApiTest : DescribeSpec({
    describe("Source test: bilibili api") {
        val api = Api(Source.cookiesStorage, Source.client)

        xit("#login") {
            val secret = ""
            api.isLogged().shouldBeFalse()
            api.login(secret).shouldBeTrue()
            api.isLogged().shouldBeTrue()
        }

        it("#search") {
            api.search(0, "test").shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getHomeHot") {
            api.getHomeHot(0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getHomeFans") {
            api.getHomeFans(0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getHomeRecommend") {
            api.getHomeRecommend(0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getDailyPush") {
            val date = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            api.getDailyPush(0, date).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getClassPage") {
            api.getClassPage(0, 0, 0, 0, 0, 0).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getComicDetail") {
            api.getComicDetail("25522").shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getReply") {
            api.getReply("25522", 1, 2).shouldHaveStatus(HttpStatusCode.OK)
        }

        it("#getChapterIndex") {
            api.getImageIndex("259303").shouldHaveStatus(HttpStatusCode.OK)
        }
    }
})
