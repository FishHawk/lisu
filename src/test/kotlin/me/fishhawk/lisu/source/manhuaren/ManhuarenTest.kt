package me.fishhawk.lisu.source.manhuaren

import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import me.fishhawk.lisu.source.saveTestImage

class ManhuarenTest : DescribeSpec({
    describe("Provider test: manhuaren") {
        val provider = Manhuaren()

        it("#search") {
            provider.search(0, "龙珠超").first().title.shouldBe("龙珠超")
        }

        it("#getBoard") {
            provider.getBoard("popular", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            provider.getBoard("latest", 0, mapOf("type" to 0)).shouldNotBeEmpty()
            provider.getBoard("category", 0, mapOf("type" to 0, "status" to 0)).shouldNotBeEmpty()
        }

        val mangaId = "18657"
        val chapterId = "1012028"

        it("#getManga") {
            provider.getManga(mangaId).title.shouldBe("龙珠超")
        }

        it("#getChapter") {
            provider.getContent(mangaId, "", chapterId).shouldNotBeEmpty()
        }

        it("#getImage") {
            val urls = provider.getContent(mangaId, "", chapterId).shouldNotBeEmpty()
            val image = provider.getImage(urls.first())
            saveTestImage("manhuaren", image)
        }
    }
})