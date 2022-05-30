package me.fishhawk.lisu.source.manhuaren

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.saveTestImage

class ManhuarenTest : DescribeSpec({
    describe("Source test: manhuaren") {
        val source = Manhuaren()

        it("#search") {
            source.search(0, "龙珠超").shouldBeSuccess()
                .first().title.shouldBe("龙珠超")
        }

        it("#getBoard") {
            listOf(
                source.getBoard("popular", 0, mapOf("type" to 0)),
                source.getBoard("latest", 0, mapOf("type" to 0)),
                source.getBoard("latest", 0, mapOf("type" to 1)),
                source.getBoard("category", 0, mapOf("type" to 0, "status" to 0)),
            ).forEach {
                it.shouldBeSuccess().shouldNotBeEmpty()
            }
        }

        val mangaId = "18657"
        val chapterId = "1012028"

        it("#getManga") {
            source.getManga(mangaId).shouldBeSuccess()
                .title.shouldBe("龙珠超")
        }

        it("#getChapter") {
            source.getContent(mangaId, chapterId).shouldBeSuccess()
                .shouldNotBeEmpty()
        }

        it("#getImage") {
            val url = source.getContent(mangaId, chapterId).shouldBeSuccess().first()
            val image = source.getImage(url).shouldBeSuccess()
            source.saveTestImage(image)
        }
    }
})