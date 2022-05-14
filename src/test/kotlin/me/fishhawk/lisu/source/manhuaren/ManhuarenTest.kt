package me.fishhawk.lisu.source.manhuaren

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.saveTestImage

class ManhuarenTest : DescribeSpec({
    describe("Source test: manhuaren") {
        val source = Manhuaren()

        it("#search") {
            source.searchImpl(0, "龙珠超").first().title.shouldBe("龙珠超")
        }

        it("#getBoard") {
            source.getBoardImpl("popular", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoardImpl("latest", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoardImpl("latest", 0, mapOf("type" to 1)).shouldNotBeEmpty()
            source.getBoardImpl("category", 0, mapOf("type" to 0, "status" to 0)).shouldNotBeEmpty()
        }

        val mangaId = "18657"
        val chapterId = "1012028"

        it("#getManga") {
            source.getMangaImpl(mangaId).title.shouldBe("龙珠超")
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