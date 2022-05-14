package me.fishhawk.lisu.library

import cc.ekblad.toml.encodeToString
import cc.ekblad.toml.model.TomlValue
import cc.ekblad.toml.tomlMapper
import io.kotest.core.spec.style.DescribeSpec
import me.fishhawk.lisu.model.ChapterMetadataDto
import me.fishhawk.lisu.model.MangaMetadataDto

data class Test(
    val a: Map<String, String>
)

class MangaTest : DescribeSpec({
    describe("Source test: bilibili api") {
        val mapper = tomlMapper {
            encoder<ChapterMetadataDto> { it ->
                TomlValue()
            }
        }
        it("#search") {
            val metadata = MangaMetadataDto(
                title = "title",
                authors = listOf("author1", "author2"),
                isFinished = true,
                description = "description",
                tags = mapOf(
                    "tag1" to listOf("v1", "v2"),
                    "tag2" to listOf("v1", "v2"),
                ),
                collections = mapOf(
                    "collection" to mapOf(
                        "chapter" to ChapterMetadataDto(name = "name", title = "title")
                    )
                )
            )
            println(mapper.encodeToString(metadata))
        }
    }
})
