package me.fishhawk.lisu.source.manhuaren

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import source.BoardId
import source.manhuaren.Manhuaren
import source.saveTestImage

class ManhuarenTest : DescribeSpec({
    describe("Source test: manhuaren") {
        val source = Manhuaren()

        it("#getBoard Main") {
            source.getBoard(BoardId.Main, "0", Parameters.build {
                append("类型", "0")
                append("状态", "0")
            }).shouldBeSuccess().list.shouldNotBeEmpty()
        }

        it("#getBoard Rank") {
            source.getBoard(BoardId.Rank, "0", Parameters.build {
                append("榜单", "2")
            }).shouldBeSuccess().list.shouldNotBeEmpty()

            source.getBoard(BoardId.Rank, "0", Parameters.build {
                append("榜单", "0")
            }).shouldBeSuccess().list.shouldNotBeEmpty()

            source.getBoard(BoardId.Rank, "0", Parameters.build {
                append("榜单", "1")
            }).shouldBeSuccess().list.shouldNotBeEmpty()
        }

        it("#getBoard Search") {
            source.getBoard(BoardId.Search, "0", Parameters.build {
                append("keywords", "龙珠超")
            }).shouldBeSuccess().list.shouldNotBeEmpty()
                .first().title.shouldBe("龙珠超")
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