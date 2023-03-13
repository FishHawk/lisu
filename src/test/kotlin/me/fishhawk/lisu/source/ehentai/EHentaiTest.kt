package me.fishhawk.lisu.source.ehentai

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import source.BoardId
import source.ehentai.EHentai
import source.saveTestImage

class EHentaiTest : DescribeSpec({
    describe("Source test: ehentai api") {
        val source = EHentai()

        it("#getBoard Main") {
            source.getBoard(BoardId.Main, "0", Parameters.Empty)
                .shouldBeSuccess().list.shouldNotBeEmpty()
        }

        it("#getBoard Rank") {
            source.getBoard(BoardId.Rank, "0", Parameters.build {
                append("Type", "0")
            }).shouldBeSuccess().list.shouldNotBeEmpty()

            source.getBoard(BoardId.Rank, "0", Parameters.build {
                append("Type", "1")
            }).shouldBeSuccess().list.shouldNotBeEmpty()
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

        it("#comment feature") {
            source.commentFeature.getComment(mangaId, 0).shouldBeSuccess().shouldNotBeEmpty()
        }
    }
})
