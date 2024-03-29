package source.manhuaren

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import library.model.Chapter
import library.model.Manga
import library.model.MangaDetail
import library.model.MangaPage
import util.Image
import source.*
import source.asDate
import source.asDateTime

class Manhuaren : Source() {
    override val id: String = "漫画人"
    override val lang: String = "zh"

    override val boardModel: Map<BoardId, BoardModel> = mapOf(
        BoardId.Main to BoardModel(
            base = mapOf(
                "类型" to FilterModel.Select(Api.categoryTypes.map { it.name }),
                "状态" to FilterModel.Select(Api.categoryStatuses.map { it.name }),
            )
        ),
        BoardId.Rank to BoardModel(
            base = mapOf(
                "榜单" to FilterModel.Select(listOf("最新更新", "最新上架") + Api.rankTypes.map { it.name })
            )
        ),
        BoardId.Search to BoardModel(),
    )

    private val api = Api(client = client)

    override suspend fun getBoardImpl(boardId: BoardId, key: String, filters: Parameters): MangaPage {
        val page = if (key.isEmpty()) 0 else key.toInt()

        return when (boardId) {
            BoardId.Main -> api.getCategoryMangas(
                page,
                filters.int("类型"),
                filters.int("状态"),
            )

            BoardId.Search -> api.getSearchManga(
                page,
                filters.keywords(),
            )

            BoardId.Rank -> when (val type = filters.int("榜单")) {
                0 -> api.getUpdate(page)
                1 -> api.getRelease(page)
                else -> api.getRank(page, type - 2)
            }
        }.body<JsonObject>().let {
            val obj = it["response"]!!.jsonObject
            MangaPage(
                list = parseJsonArrayToMangas((obj["result"] ?: obj["mangas"]!!).jsonArray),
                nextKey = (page + 1).toString()
            )
        }
    }

    private fun parseJsonArrayToMangas(arr: JsonArray): List<Manga> {
        return arr.map {
            val obj = it.jsonObject
            Manga(
                id = obj["mangaId"]!!.jsonPrimitive.content,
                cover = obj["mangaCoverimageUrl"]?.jsonPrimitive?.content,
                updateTime = obj["mangaNewestTime"]?.jsonPrimitive?.content?.asDateTime("yyyy-MM-dd HH:mm:ss"),
                title = obj["mangaName"]?.jsonPrimitive?.content,
                authors = obj["mangaAuthor"]?.jsonPrimitive?.content?.split(",") ?: emptyList(),
                isFinished = obj["mangaIsOver"]!!.jsonPrimitive.int == 1
            )
        }
    }

    override suspend fun getMangaImpl(mangaId: String): MangaDetail {
        return api.getDetail(mangaId).body<JsonObject>().let { data ->
            val obj = data["response"]!!.jsonObject

            MangaDetail(
                id = obj["mangaId"]!!.jsonPrimitive.content,
                cover = obj["mangaCoverimageUrl"]?.jsonPrimitive?.content.let {
                    if (it == null || it == "http://mhfm5.tel.cdndm5.com/tag/category/nopic.jpg") {
                        (obj["mangaPicimageUrl"] ?: obj["shareIcon"])?.jsonPrimitive?.content ?: ""
                    } else it
                },
                updateTime = obj["mangaNewestTime"]?.jsonPrimitive?.content?.asDateTime("yyyy-MM-dd HH:mm:ss"),

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
                        Chapter(
                            id = it.jsonObject["sectionId"]!!.jsonPrimitive.content,
                            name = it.jsonObject["sectionName"]!!.jsonPrimitive.content,
                            title = it.jsonObject["sectionTitle"]!!.jsonPrimitive.content,
                            isLocked = it.jsonObject["isMustPay"]?.jsonPrimitive?.int == 1,
                            updateTime = it.jsonObject["releaseTime"]?.jsonPrimitive?.content?.asDate("yyyy-MM-dd")
                        )
                    }.reversed()
                }.filterValues { it.isNotEmpty() }
            )
        }
    }

    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> {
        return api.getRead(chapterId).body<JsonObject>().let { data ->
            val obj = data["response"]!!.jsonObject
            val host = obj["hostList"]!!.jsonArray.first().jsonPrimitive.content
            val query = obj["query"]!!.jsonPrimitive.content
            obj["mangaSectionImages"]!!.jsonArray.map {
                "$host${it.jsonPrimitive.content}$query"
            }
        }
    }

    override suspend fun getImageImpl(url: String) = withContext(Dispatchers.IO) {
        api.getImage(url).let {
            Image(
                it.contentType(),
                it.body<ByteReadChannel>().toInputStream()
            )
        }
    }
}