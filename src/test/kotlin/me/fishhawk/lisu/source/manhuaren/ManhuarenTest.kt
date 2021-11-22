package me.fishhawk.lisu.source.manhuaren

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.saveTestImage

class ManhuarenTest : DescribeSpec({
    describe("Source test: manhuaren") {
        val source = Manhuaren()

        it("#search") {
            source.search(0, "龙珠超").first().title.shouldBe("龙珠超")
        }

        it("#getBoard") {
            source.getBoard("popular", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoard("latest", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            source.getBoard("latest", 0, mapOf("type" to 1)).shouldNotBeEmpty()
            source.getBoard("category", 0, mapOf("type" to 0, "status" to 0)).shouldNotBeEmpty()
        }

        val mangaId = "18657"
        val chapterId = "1012028"

        it("#getManga") {
            source.getManga(mangaId).title.shouldBe("龙珠超")
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