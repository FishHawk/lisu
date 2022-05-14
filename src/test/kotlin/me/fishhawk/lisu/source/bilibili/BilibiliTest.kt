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
            source.searchImpl(0, "迷宫饭").first().title.shouldBe("迷宫饭")
        }

        it("#getBoard") {
            source.getBoardImpl(Board.Popular.id, 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoardImpl(Board.Latest.id, 0, emptyMap()).shouldNotBeEmpty()
            source.getBoardImpl(
                Board.Category.id, 0,
                mapOf("style" to 0, "area" to 0, "isFinish" to 0, "isFree" to 0, "order" to 0)
            ).shouldNotBeEmpty()
        }

        val mangaId = "28284"
        val chapterId = "466261"

        it("#getManga") {
            source.getMangaImpl(mangaId).title.shouldBe("迷宫饭")
        }

        it("#getChapter") {
            source.getContentImpl(mangaId, chapterId).shouldNotBeEmpty()
        }

        it("#getImage") {
            val url = source.getContentImpl(mangaId, chapterId).first()
            val image = source.getImageImpl(url)
            source.saveTestImage(image)
        }
    }
})
