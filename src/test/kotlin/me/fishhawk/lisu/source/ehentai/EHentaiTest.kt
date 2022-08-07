package me.fishhawk.lisu.source.ehentai

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.saveTestImage

class EHentaiTest : DescribeSpec({
    describe("Source test: ehentai api") {
        val source = EHentai()

        it("#getBoard") {
            listOf(
                source.getBoard(BoardId.Popular.id, 0, emptyMap()),
                source.getBoard(BoardId.Latest.id, 0, emptyMap()),
                source.getBoard(BoardId.Ranking.id, 0, mapOf("type" to 0)),
            ).forEach {
                it.shouldBeSuccess().shouldNotBeEmpty()
            }
        }

        val mangaId = "2237436.b6b5a0d937"
        val chapterId = " "

        it("#getManga") {
            source.getManga(mangaId).shouldBeSuccess()
                .title.shouldBe("[PIXIV] 笠木梨Ceey (27024181)")
        }

        it("#getContent") {
            source.getContent(mangaId, chapterId).shouldBeSuccess().shouldNotBeEmpty()
        }

        it("#getImage") {
            val url = source.getContent(mangaId, chapterId).shouldBeSuccess().first()
            val image = source.getImage(url).shouldBeSuccess()
            source.saveTestImage(image)
        }
    }
})
