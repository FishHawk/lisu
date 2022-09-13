package me.fishhawk.lisu.source.bilibili

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import io.ktor.http.*
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.saveTestImage

class BilibiliTest : DescribeSpec({
    describe("Source test: bilibili") {
        val source = Bilibili()

        it("#getBoard Main") {
            source.getBoard(BoardId.Main, 0, Parameters.build {
                append("题材", "0")
                append("地区", "0")
                append("进度", "0")
                append("收费", "0")
                append("排序", "0")
            }).shouldBeSuccess().shouldNotBeEmpty()
        }

        it("#getBoard Rank") {
            source.getBoard(BoardId.Rank, 0, Parameters.build {
                append("榜单", "0")
            }).shouldBeSuccess().shouldNotBeEmpty()
        }

        it("#getBoard Search") {
            source.getBoard(BoardId.Search, 0, Parameters.build {
                append("keywords", "迷宫饭")
            }).shouldBeSuccess().shouldNotBeEmpty()
                .first().title.shouldBe("迷宫饭")
        }

        val mangaId = "28284"
        val chapterId = "466261"

        it("#getManga") {
            source.getManga(mangaId).shouldBeSuccess()
                .title.shouldBe("迷宫饭")
        }

        it("#getChapter") {
            source.getContent(mangaId, chapterId).shouldBeSuccess().shouldNotBeEmpty()
        }

        it("#getImage") {
            val url = source.getContent(mangaId, chapterId).shouldBeSuccess().first()
            val image = source.getImage(url).shouldBeSuccess()
            source.saveTestImage(image)
        }

        xit("#login feature") {
            val secret = ""
            source.loginFeature.isLogged().shouldBeFalse()
            source.loginFeature.login(mapOf("SESSDATA" to secret)).shouldBeTrue()
            source.loginFeature.isLogged().shouldBeTrue()
            source.loginFeature.logout()
            source.loginFeature.isLogged().shouldBeFalse()
        }

        it("#comment feature") {
            source.commentFeature.getComment(mangaId, 1).shouldBeSuccess().shouldNotBeEmpty()
        }
    }
})
