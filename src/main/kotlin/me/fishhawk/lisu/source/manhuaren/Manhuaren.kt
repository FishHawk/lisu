package me.fishhawk.lisu.source.manhuaren

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import me.fishhawk.lisu.model.ChapterDto
import me.fishhawk.lisu.model.Image
import me.fishhawk.lisu.model.MangaDetailDto
import me.fishhawk.lisu.model.MangaDto
import me.fishhawk.lisu.source.*

class Manhuaren : Source {
    override val id: String = "漫画人"
    override val lang: String = "zh"

    override val boardModels: Map<String, BoardModel> = mapOf(
        Board.Popular.id to mapOf("type" to Api.rankTypes.map { it.name }),
        Board.Latest.id to mapOf("type" to listOf("最新更新", "最新上架")),
        Board.Category.id to mapOf(
            "type" to Api.categoryTypes.map { it.name },
            "status" to Api.categoryStatuses.map { it.name }
        )
    )

    private val api = Api()

    override suspend fun search(page: Int, keywords: String): List<MangaDto> =
        api.getSearchManga(page, keywords).body<JsonObject>().let {
            val obj = it["response"]!!.jsonObject
            parseJsonArrayToMangas((obj["result"] ?: obj["mangas"]!!).jsonArray)
        }

    override suspend fun getBoard(boardId: String, page: Int, filters: Map<String, Int>): List<MangaDto> =
        when (boardId) {
            Board.Popular.id -> api.getRank(page, filters["type"]!!)
            Board.Latest.id -> when (filters["type"]!!) {
                0 -> api.getUpdate(page)
                1 -> api.getRelease(page)
                else -> throw Error("board not found")
            }
            Board.Category.id -> api.getCategoryMangas(page, filters["type"]!!, filters["status"]!!)
            else -> throw Error("board not found")
        }.body<JsonObject>().let {
            parseJsonArrayToMangas(it["response"]!!.jsonObject["mangas"]!!.jsonArray)
        }

    private fun parseJsonArrayToMangas(arr: JsonArray) = arr.map {
        val obj = it.jsonObject
        MangaDto(
            providerId = id,
            id = obj["mangaId"]!!.jsonPrimitive.content,
            cover = obj["mangaCoverimageUrl"]?.jsonPrimitive?.content,
            updateTime = obj["mangaNewestTime"]?.asDateTimeToEpochSecond("yyyy-MM-dd HH:mm:ss"),
            title = obj["mangaName"]?.jsonPrimitive?.content,
            authors = obj["mangaAuthor"]?.jsonPrimitive?.content?.split(","),
            isFinished = obj["mangaIsOver"]!!.jsonPrimitive.int == 1
        )
    }

    override suspend fun getManga(mangaId: String): MangaDetailDto =
        api.getDetail(mangaId).body<JsonObject>().let { data ->
            val obj = data["response"]!!.jsonObject

            MangaDetailDto(
                providerId = id,
                id = obj["mangaId"]!!.jsonPrimitive.content,
                cover = obj["mangaCoverimageUrl"]?.jsonPrimitive?.content.let {
                    if (it == null || it == "http://mhfm5.tel.cdndm5.com/tag/category/nopic.jpg") {
                        (obj["mangaPicimageUrl"] ?: obj["shareIcon"])?.jsonPrimitive?.content ?: ""
                    } else it
                },
                updateTime = obj["mangaNewestTime"]?.asDateTimeToEpochSecond("yyyy-MM-dd HH:mm:ss"),

                title = obj["mangaName"]!!.jsonPrimitive.content,
                authors = obj["mangaAuthors"]!!.jsonArray.map { it.jsonPrimitive.content },
                isFinished = obj["mangaIsOver"]!!.jsonPrimitive.int == 1,
                description = obj["mangaIntro"]!!.jsonPrimitive.content,
                tags = obj["mangaTheme"]!!.jsonPrimitive.content.let {
                    if (it.isBlank()) emptyMap()
                    else mapOf("" to it.split(' '))
                },

                collections = mapOf(
                    "连载" to obj["mangaWords"],
                    "单行本" to obj["mangaRolls"],
                    "番外" to obj["mangaEpisode"],
                ).mapValues { arr ->
                    arr.value!!.jsonArray.map {
                        ChapterDto(
                            id = it.jsonObject["sectionId"]!!.jsonPrimitive.content,
                            name = it.jsonObject["sectionName"]!!.jsonPrimitive.content,
                            title = it.jsonObject["sectionTitle"]!!.jsonPrimitive.content,
                            isLocked = it.jsonObject["isMustPay"]?.jsonPrimitive?.int == 1,
                            updateTime = it.jsonObject["releaseTime"]?.asDateToEpochSecond("yyyy-MM-dd")
                        )
                    }.reversed()
                }.filterValues { it.isNotEmpty() }
            )
        }

    override suspend fun getContent(mangaId: String, collectionId: String, chapterId: String): List<String> =
        api.getRead(chapterId).body<JsonObject>().let { data ->
            val obj = data["response"]!!.jsonObject
            val host = obj["hostList"]!!.jsonArray.first().jsonPrimitive.content
            val query = obj["query"]!!.jsonPrimitive.content
            obj["mangaSectionImages"]!!.jsonArray.map {
                "$host${it.jsonPrimitive.content}$query"
            }
        }

    override suspend fun getImage(url: String) = withContext(Dispatchers.IO) {
        api.getImage(url).let {
            Image(
                it.contentType(),
                it.body<ByteReadChannel>().toInputStream()
            )
        }
    }
}