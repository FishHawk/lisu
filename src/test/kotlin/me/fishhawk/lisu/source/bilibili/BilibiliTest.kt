package me.fishhawk.lisu.source.bilibili

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.Board
import me.fishhawk.lisu.source.saveTestImage

class BilibiliTest : DescribeSpec({
    describe("Source test: bilibili") {
        val source = Bilibili()

        it("#search") {
            source.search(0, "迷宫饭").first().title.shouldBe("迷宫饭")
        }

        it("#getBoard") {
            source.getBoard(Board.Popular.id, 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoard(Board.Latest.id, 0, emptyMap()).shouldNotBeEmpty()
            source.getBoard(
                Board.Category.id, 0,
                mapOf("style" to 0, "area" to 0, "isFinish" to 0, "isFree" to 0, "order" to 0)
            ).shouldNotBeEmpty()
        }

        val mangaId = "28284"
        val chapterId = "466261"

        it("#getManga") {
            source.getManga(mangaId).title.shouldBe("迷宫饭")
        }

        it("#getChapter") {
            source.getContent(mangaId, "", chapterId).shouldNotBeEmpty()
        }

        it("#getImage") {
            val url = source.getContent(mangaId, "", chapterId).first()
            val image = source.getImage(url)
            source.saveTestImage(image)
        }
    }
})
