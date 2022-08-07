package me.fishhawk.lisu.source.bilibili

import io.ktor.client.call.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import me.fishhawk.lisu.model.*
import me.fishhawk.lisu.source.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class Bilibili : Source() {
    override val id: String = "哔哩哔哩漫画"
    override val lang: String = "zh"

    override val boardModels: Map<BoardId, BoardModel> = mapOf(
        BoardId.Ranking to mapOf("type" to Api.homeHotType.map { it.first }),
        BoardId.Latest to emptyMap(),
        BoardId.Category to mapOf(
            "style" to Api.classStyle.map { it.first },
            "area" to Api.classArea.map { it.first },
            "isFinish" to Api.classIsFinish.map { it.first },
            "isFree" to Api.classIsFree.map { it.first },
            "order" to Api.classOrder.map { it.first }
        )
    )

    private val api = Api()

    override val loginFeature = object : LoginFeature {
        override val loginSite = Api.baseUrl
        override suspend fun isLogged() = api.isLogged()
        override suspend fun logout() = api.logout()
        override suspend fun login(cookies: Map<String, String>): Boolean {
            return cookies[Api.SESSDATA]?.let { api.login(it) } ?: false
        }
    }

    override val commentFeature = object : CommentFeature() {
        override suspend fun getCommentImpl(mangaId: String, page: Int): List<CommentDto> =
            api.getReply(mangaId, page = page + 1, sort = 2)
                .body<JsonObject>()
                .let { it["data"]!!.jsonObject }
                .let { obj ->
                    fun parseComment(reply: JsonObject): CommentDto {
                        return CommentDto(
                            username = reply["member"]!!.jsonObject["uname"]!!.jsonPrimitive.content,
                            content = reply["content"]!!.jsonObject["message"]!!.jsonPrimitive.content,
                            createTime = reply["ctime"]!!.jsonPrimitive.long,
                            vote = reply["like"]!!.jsonPrimitive.int,
                            subComments = reply["replies"]!!.jsonArrayOrNull?.obj?.map { parseComment(it) },
                        )
                    }

                    val upper = obj["upper"]!!.jsonObject["top"]!!.jsonObjectOrNull
                        ?.let { listOf(parseComment(it)) } ?: emptyList()
                    val replies = obj["replies"]!!.jsonArrayOrNull?.obj
                        ?.map { parseComment(it) } ?: emptyList()
                    upper + replies
                }

    }

    override suspend fun searchImpl(page: Int, keywords: String): List<MangaDto> =
        api.search(page, keywords).body<JsonObject>().let { json ->
            json["data"]!!.jsonObject["list"]!!.jsonArray.map { it.jsonObject }.map { obj ->
                MangaDto(
                    providerId = id,
                    id = obj["id"]!!.jsonPrimitive.content,
                    cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                    updateTime = null,
                    title = obj["org_title"]?.jsonPrimitive?.content,
                    authors = obj["author_name"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList(),
                    isFinished = obj["is_finish"]?.asMangaIsFinish()
                )
            }
        }

    override suspend fun getBoardImpl(boardId: BoardId, page: Int, filters: Map<String, Int>): List<MangaDto> =
        when (boardId) {
            BoardId.Ranking -> if (page > 0) emptyList() else api.getHomeHot(filters["type"]!!)
                .body<JsonObject>()
                .let { it["data"]!!.jsonArray.obj }
                .map { obj ->
                    MangaDto(
                        providerId = id,
                        id = obj["comic_id"]!!.jsonPrimitive.content,
                        cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                        title = obj["title"]?.jsonPrimitive?.content,
                        authors = obj["author"]?.jsonArray?.string ?: emptyList(),
                        isFinished = obj["is_finish"]?.asMangaIsFinish()
                    )
                }
            BoardId.Latest -> api.getDailyPush(
                page,
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            ).body<JsonObject>()
                .let { it["data"]!!.jsonObject["list"]!!.jsonArray.obj }
                .map { obj ->
                    MangaDto(
                        providerId = id,
                        id = obj["comic_id"]!!.jsonPrimitive.content,
                        cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                        title = obj["title"]?.jsonPrimitive?.content,
                    )
                }
            BoardId.Category -> api.getClassPage(
                page,
                filters["style"]!!,
                filters["area"]!!,
                filters["isFinish"]!!,
                filters["isFree"]!!,
                filters["order"]!!,
            ).body<JsonObject>()
                .let { it["data"]!!.jsonArray.obj }
                .map { obj ->
                    MangaDto(
                        providerId = id,
                        id = obj["season_id"]!!.jsonPrimitive.content,
                        cover = obj["vertical_cover"]?.jsonPrimitive?.content,
                        title = obj["title"]?.jsonPrimitive?.content,
                        isFinished = obj["is_finish"]?.asMangaIsFinish()
                    )
                }
            else -> throw Error("board not found")
        }

    override suspend fun getMangaImpl(mangaId: String): MangaDetailDto =
        api.getComicDetail(mangaId)
            .body<JsonObject>()
            .let { it["data"]!!.jsonObject }
            .let { obj ->
                MangaDetailDto(
                    providerId = id,
                    id = obj["id"]!!.jsonPrimitive.content,
                    cover = obj["vertical_cover"]!!.jsonPrimitive.content,

                    title = obj["title"]!!.jsonPrimitive.content,
                    authors = obj["author_name"]!!.jsonArray.string,
                    isFinished = obj["is_finish"]!!.asMangaIsFinish(),
                    description = obj["evaluate"]!!.jsonPrimitive.content,
                    tags = mapOf(DefaultTagName to obj["styles"]!!.jsonArray.string),

                    chapters = obj["ep_list"]!!.jsonArray.obj.map {
                        ChapterDto(
                            id = it["id"]!!.jsonPrimitive.content,
                            name = it["short_title"]!!.jsonPrimitive.content,
                            title = it["title"]!!.jsonPrimitive.content,
                            isLocked = it["is_locked"]?.jsonPrimitive?.boolean,
                            updateTime = it["pub_time"]?.jsonPrimitive?.content?.asDateTimeToEpochSecond("yyyy-MM-dd HH:mm:ss")
                        )
                    }.reversed()
                )
            }

    override suspend fun getContentImpl(mangaId: String, chapterId: String): List<String> =
        api.getImageIndex(chapterId)
            .body<JsonObject>()
            .let { it["data"]!!.jsonObject }
            .let { obj -> obj["images"]!!.jsonArray.obj.map { it["path"]!!.jsonPrimitive.content } }
            .let { images -> api.getImageToken(images) }
            .body<JsonObject>()
            .let { it["data"]!!.jsonArray.obj }
            .map {
                val url = it["url"]!!.jsonPrimitive.content
                val token = it["token"]!!.jsonPrimitive.content
                "$url?token=$token"
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

private fun JsonElement.asMangaIsFinish() =
    when (jsonPrimitive.int) {
        0 -> false
        1 -> true
        else -> null
    }