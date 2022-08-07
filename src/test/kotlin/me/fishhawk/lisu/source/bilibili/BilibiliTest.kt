package me.fishhawk.lisu.source.bilibili

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.booleans.shouldBeFalse
import io.kotest.matchers.booleans.shouldBeTrue
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.result.shouldBeSuccess
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.BoardId
import me.fishhawk.lisu.source.saveTestImage

class BilibiliTest : DescribeSpec({
    describe("Source test: bilibili") {
        val source = Bilibili()

        it("#search") {
            source.search(0, "迷宫饭").shouldBeSuccess()
                .first().title.shouldBe("迷宫饭")
        }

        it("#getBoard") {
            listOf(
                source.getBoard(BoardId.Popular.id, 0, mapOf("type" to 0)),
                source.getBoard(BoardId.Latest.id, 0, emptyMap()),
                source.getBoard(
                    BoardId.Category.id, 0,
                    mapOf("style" to 0, "area" to 0, "isFinish" to 0, "isFree" to 0, "order" to 0)
                ),
            ).forEach {
                it.shouldBeSuccess().shouldNotBeEmpty()
            }
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
